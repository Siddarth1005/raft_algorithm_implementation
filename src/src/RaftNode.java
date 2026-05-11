import java.util.*;

public class RaftNode {
    Integer nodeID;
    Role currentRole;
    List<Log> logs;
    List<RaftNode> nodes = new ArrayList<>();
    Integer votedFor = null;
    Set<VoteResponse> votesReceived;
    int currentTerm;
    RaftNode currentLeader;
    HashMap<Integer, Integer> sentLength = new HashMap<>();
    HashMap<Integer, Integer> ackLength = new HashMap<>();
    HashSet<Integer> messageIDSet = new HashSet<>();
    int commitLength;

    public RaftNode(int nodeID, List<RaftNode> nodes) {
        this.nodeID = nodeID;
        this.currentRole = Role.FOLLOWER;
        this.logs = new ArrayList<>();
        this.nodes = nodes;
        this.votedFor = null;
        this.votesReceived = new HashSet<>();
        this.currentTerm = 0;
        this.currentLeader = null;
        this.commitLength = 0;
    }

    // In real Raft, currentTerm, votedFor, logs are persisted to disk before recovering
    public void crashRecovery(int currentTerm, Integer votedFor, List<Log> logs) {
        this.currentTerm = currentTerm;
        this.votedFor = votedFor;
        this.logs = logs;
        this.currentRole = Role.FOLLOWER;
        this.currentLeader = null;
        this.votesReceived = new HashSet<>();
    }

    // Stage 1: triggered when node suspects leader failure or election timeout
    // In real Raft, election timeout is randomized (150-300ms) to prevent split votes
    public void becomeCandidate() {
        currentRole = Role.CANDIDATE;
        this.votedFor = this.nodeID;
        currentTerm = this.currentTerm + 1;
        int logLastTerm = logs.isEmpty() ? 0 : logs.getLast().term;

        // vote for self
        votesReceived.add(new VoteResponse(true, this.nodeID, this.currentTerm));
        VoteRequest request = new VoteRequest(logs.size(), currentTerm, this.nodeID, logLastTerm);

        // send VoteRequest to all other nodes
        for (RaftNode node : nodes) {
            if (!Objects.equals(node.nodeID, this.nodeID)) {
                votesReceived.add(node.receiveVoteRequest(request));
            }
        }

        boolean result = checkIfLeader(votesReceived);
        if (result) {
            this.currentRole = Role.LEADER;
            for (RaftNode node : nodes) {
                // sentLength initialized to current log size — assume follower is up to date
                sentLength.put(node.nodeID, this.logs.size());
                // ackLength initialized to 0 — nothing confirmed yet
                ackLength.put(node.nodeID, 0);
                node.currentLeader = this;
            }
            // In real Raft, leader immediately sends heartbeat to all followers
            // to assert leadership and prevent new elections
            // periodically called even with no new messages
        }
    }

    // Stage 3: check if candidate has received enough votes to become leader
    private boolean checkIfLeader(Set<VoteResponse> votesReceived) {
        int quorum = nodes.size() / 2 + 1;
        int votesCount = 0;
        for (VoteResponse votes : votesReceived) {
            // if any response has higher term, step down immediately
            // this means a new election has started that we don't know about
            if (votes.currentTerm > this.currentTerm) {
                this.currentTerm = votes.currentTerm;
                this.currentRole = Role.FOLLOWER;
                this.votedFor = null;
                return false;
            } else if (votes.voted) votesCount += 1;
        }
        return votesCount >= quorum;
    }

    // Stage 2: receive and process a vote request from a candidate
    public VoteResponse receiveVoteRequest(VoteRequest voteRequest) {
        // if candidate has higher term, update our term and step down
        // this keeps all nodes in sync with the latest term
        if (voteRequest.currentTerm > this.currentTerm) {
            this.votedFor = null;
            this.currentRole = Role.FOLLOWER;
            this.currentTerm = voteRequest.currentTerm;
        }

        int logLength = this.logs.isEmpty() ? 0 : this.logs.size();
        int logLastTerm = this.logs.isEmpty() ? 0 : this.logs.getLast().term;

        // logOk ensures we only vote for candidates whose log is at least as up to date as ours
        // prevents data loss if a candidate with stale log becomes leader
        boolean logOK = voteRequest.logLastTerm > logLastTerm ||
                (voteRequest.logLastTerm == logLastTerm && voteRequest.currentLogLength >= logLength);

        // grant vote only if:
        // 1. candidate term matches our current term
        // 2. candidate log is up to date
        // 3. we haven't voted for someone else this term (idempotency for retries)
        if (logOK && voteRequest.currentTerm == this.currentTerm &&
                (this.votedFor == null || this.votedFor.equals(voteRequest.candidateID))) {
            this.votedFor = voteRequest.candidateID;
            return new VoteResponse(true, this.nodeID, this.currentTerm);
        }
        return new VoteResponse(false, this.nodeID, this.currentTerm);
    }

    // Stage 4: broadcast a message — leader appends to log and replicates
    // follower forwards to leader
    public void broadcastMessage(Message message) {
        // deduplication check — idempotent operation
        if (!messageIDSet.contains(message.messageID)) {
            if (this.currentLeader == this) {
                this.messageIDSet.add(message.messageID);

                // leader appends to its own log with current term
                this.logs.add(new Log(new Message(message.message, message.messageID), this.currentTerm));

                // leader acknowledges its own entry
                ackLength.put(this.nodeID, logs.size());

                // start ackCounter at 1 — leader counts itself
                int ackCounter = 1;

                for (RaftNode node : this.nodes) {
                    if (node.nodeID == this.nodeID) continue;

                    // prefixLength = how many entries we think follower already has
                    int prefixLength = this.sentLength.get(node.nodeID);
                    int prefixTerm = 0;

                    // prefixTerm = term of last entry in the prefix
                    // follower uses this to verify log consistency
                    if (prefixLength > 0) prefixTerm = this.logs.get(prefixLength - 1).getTerm();

                    // suffix = new entries follower doesn't have yet
                    List<Log> suffixMessage = new ArrayList<>();
                    for (int i = prefixLength; i < logs.size(); i++) suffixMessage.add(logs.get(i));

                    // update sentLength — we've now sent up to this point
                    node.sentLength.put(node.nodeID, prefixLength + suffixMessage.size());

                    // Stage 5/6/7: replicate log entries to follower
                    AckResponse ack = node.replicateLogs(prefixLength, prefixTerm, suffixMessage, this.commitLength);

                    // Stage 8: process ack from follower
                    if (ack.response) {
                        // follower accepted — update ackLength
                        node.ackLength.put(node.nodeID, ack.logLength);
                        if (ack.logLength >= this.logs.size()) ackCounter++;
                    } else {
                        // follower rejected — step back and retry with earlier entries
                        // this happens when follower is missing earlier entries
                        // In real Raft this is async — leader retries on next heartbeat
                        int newPrefixLength = this.sentLength.get(node.nodeID);
                        if (newPrefixLength > 0) {
                            node.sentLength.put(node.nodeID, newPrefixLength - 1);
                            // retry replication with earlier prefix
                            // in real Raft this would be called on next heartbeat
                        }
                    }

                    // check if follower has higher term — step down if so
                    // this means a new leader has been elected that we don't know about
                    if (ack.term > this.currentTerm) {
                        this.currentTerm = ack.term;
                        this.currentRole = Role.FOLLOWER;
                        this.votedFor = null;
                        return;
                    }
                }

                // Stage 9: commitLogEntries
                // only commit if quorum acknowledged AND entry is from current term
                // the currentTerm check prevents committing entries from previous terms directly
                // (Raft safety property — leader can only commit entries from its own term)
                if (ackCounter >= (nodes.size() / 2 + 1) &&
                        !logs.isEmpty() &&
                        logs.getLast().term == this.currentTerm) {
                    // find max log length acknowledged by quorum
                    int maxReady = 0;
                    for (int len = 1; len <= logs.size(); len++) {
                        int acks = 0;
                        for (RaftNode node : nodes) {
                            if (ackLength.getOrDefault(node.nodeID, 0) >= len) acks++;
                        }
                        if (acks >= (nodes.size() / 2 + 1)) maxReady = len;
                    }

                    if (maxReady > commitLength) {
                        // deliver committed entries to application
                        for (int i = commitLength; i < maxReady; i++) {
                            System.out.println("Delivering to application: " +
                                    logs.get(i).getMessage() + " from term " + logs.get(i).getTerm());
                        }
                        this.commitLength = maxReady;
                    }
                }

            } else {
                // follower forwards to leader via FIFO link
                // in real Raft this goes through network, here direct call
                this.currentLeader.broadcastMessage(message);
            }
        } else {
            System.out.println("Duplicate message ignored: " + message.messageID);
        }
    }

    // Stage 5/6/7: follower receives log entries from leader
    // checks consistency, truncates stale entries, appends new ones, delivers committed entries
    public AckResponse replicateLogs(int prefixLength, int prefixTerm,
                                     List<Log> suffixLogs, Integer leaderCommitLength) {

        // Stage 6: logOk check — verify follower's log is consistent with leader's prefix
        // prefixLength == 0 means leader expects empty prefix, always ok
        // otherwise check that follower has enough entries AND term matches at prefix boundary
        boolean logOk = (prefixLength == 0) ||
                (this.logs.size() >= prefixLength &&
                        this.logs.get(prefixLength - 1).getTerm() == prefixTerm);

        if (!logOk) {
            // follower log is inconsistent — reject and tell leader to send earlier entries
            // leader will decrement sentLength and retry
            return new AckResponse(false, 0, this.currentTerm);
        }

        // Stage 7: AppendEntries
        // truncate stale entries from old leaders if needed
        if (this.logs.size() > prefixLength && !suffixLogs.isEmpty()) {
            // check if existing entries conflict with suffix
            int index = Math.min(this.logs.size(), prefixLength + suffixLogs.size()) - 1;
            if (this.logs.get(index).getTerm() != suffixLogs.get(index - prefixLength).getTerm()) {
                // conflict found — truncate back to prefix
                // entries beyond prefixLength are from old leader and must be discarded
                logs = new ArrayList<>(logs.subList(0, prefixLength));
            }
        }

        // append only entries not already in log
        if (prefixLength + suffixLogs.size() > this.logs.size()) {
            int from = this.logs.size() - prefixLength;
            for (int i = from; i < suffixLogs.size(); i++) this.logs.add(suffixLogs.get(i));
        }

        // deliver committed entries to application
        if (this.commitLength < leaderCommitLength) {
            for (int i = commitLength; i < leaderCommitLength; i++) {
                System.out.println("Node " + nodeID + " delivering to application: " +
                        logs.get(i).getMessage());
            }
            this.commitLength = leaderCommitLength;
        }

        // return success with current log size and term
        return new AckResponse(true, this.logs.size(), this.currentTerm);
    }
}