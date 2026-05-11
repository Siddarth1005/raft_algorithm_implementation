import org.w3c.dom.Node;

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


    public RaftNode(int nodeID,List<RaftNode> nodes) {
        this.nodeID = nodeID;
        this.currentRole = Role.FOLLOWER;
        this.logs = new ArrayList<>();;
        this.nodes = nodes;
        this.votedFor = null;
        this.votesReceived = new HashSet<>();
        this.currentTerm = 0;
        this.currentLeader = null;
        this.commitLength = 0;
    }

    public void crashRecovery(int currentTerm, Integer votedFor, List<Log> logs) {
        // restore persistent state
        this.currentTerm = currentTerm;
        this.votedFor = votedFor;
        this.logs = logs;
        // reset volatile state
        this.currentRole = Role.FOLLOWER;
        this.currentLeader = null;
        this.votesReceived = new HashSet<>();
    }
//    triggered when suspects leader failure or election timeout
//    stage 1
    public void becomeCandidate(){
        currentRole = Role.CANDIDATE;
        this.votedFor = this.nodeID;
        currentTerm = this.currentTerm + 1;
        int loglastTerm = logs.isEmpty() ? 0 : logs.getLast().term;
        votesReceived.add(new VoteResponse(true, this.nodeID, this.currentTerm));
        VoteRequest request = new VoteRequest(logs.size(), currentTerm, this.nodeID, loglastTerm);
        for(RaftNode node : nodes ) if(!Objects.equals(node.nodeID, this.nodeID)) votesReceived.add(node.recieveVoteRequest(request));
        boolean result = checkIfLeader(votesReceived);
        if(result){
            for(RaftNode node : nodes){
                sentLength.put(node.nodeID, this.logs.size());
                ackLength.put(node.nodeID, 0);
//                publish the data to all the nodes
                node.currentLeader = this;

            }
        }

    }

    private boolean checkIfLeader( Set<VoteResponse> votesReceived){
        int quorum = nodes.size() / 2 + 1;
        int votesCount = 0;
        for(VoteResponse votes : votesReceived){
            if(votes.currentTerm > this.currentTerm){
                this.currentTerm = votes.currentTerm;
                this.currentRole = Role.FOLLOWER;
                return false;
            }else if(votes.voted) votesCount += 1;
        }
        return votesCount >= quorum;
    }

//    stage 2
    public VoteResponse recieveVoteRequest(VoteRequest voteRequest){
       if(voteRequest.currentTerm > this.currentTerm){
           this.votedFor = null;
           this.currentRole = Role.FOLLOWER;
           this.currentTerm = voteRequest.currentTerm;
       }
       int logLength = 0;
       if(!this.logs.isEmpty()) logLength =  this.logs.size();
       int logLastTerm = this.logs.isEmpty() ? 0 : this.logs.getLast().term;

       boolean logOK = voteRequest.logLastTerm > logLastTerm ||
               (voteRequest.logLastTerm == logLastTerm && voteRequest.currentLogLength >= logLength);

        if( logOK && voteRequest.currentTerm == this.currentTerm && (this.votedFor == null || this.votedFor == voteRequest.candidateID)){
           this.votedFor = voteRequest.candidateID;
           return new VoteResponse(true, this.nodeID, this.currentTerm);
       }
       return new VoteResponse(false, this.nodeID, this.currentTerm);
    }


    public void broadcastMessage(Message message){
        if(!messageIDSet.contains(message.messageID)){
//            Two scenarios
//                1. The leader recieves the message and forwards it to the followers
//                2. The follower recieves the message and forwards it to the leader

            if(this.currentLeader == this){
                 this.messageIDSet.add(message.messageID);
//                Recieve the message, update the logs,send the message to it's followers.
                this.logs.add(new Log(new Message(message.message, message.messageID), this.currentTerm));
                ackLength.put(this.nodeID, logs.size());
                int ackCounter = 1;
                for(RaftNode node : this.nodes){
                    if(node.nodeID == this.nodeID) continue;
                    int prefixLength = this.sentLength.get(node.nodeID);
                    int prexfixTerm = 0;
                    List<Log> suffixMessage = new ArrayList<>();
                    if(prefixLength > 0) prexfixTerm =  this.logs.get(prefixLength - 1).getTerm();

                    for(int i = prefixLength; i < logs.size(); i++) suffixMessage.add(logs.get(i));
                    node.sentLength.put(node.nodeID, prefixLength + suffixMessage.size());
                    AckResponse ack = node.replicateLogs(prefixLength, prexfixTerm, suffixMessage, this.commitLength);
                    node.ackLength.put(node.nodeID, ack.logLength);
                    if(ack.logLength >= this.logs.size()) ackCounter += 1;
                }


                if(ackCounter >= (nodes.size()/2 + 1)){
                    System.out.println("Sending  the data to application");
                    this.commitLength = this.logs.size();
                }
            }else{
                this.currentLeader.broadcastMessage(message);
            }
        }else{
            System.out.println("This message is a duplicate");
        }
    }


    public AckResponse replicateLogs(int prefixLength, int prefixTerm, List<Log> suffixLogs, Integer leaderCommitLength){
//     checking if the follower has any stale data
        if(this.logs.size() > prefixLength && !suffixLogs.isEmpty()) logs = new ArrayList<>(logs.subList(0, prefixLength));
        this.logs.addAll(suffixLogs);

        if(this.commitLength < leaderCommitLength){
            for(int i = commitLength; i < leaderCommitLength; i++){
//                deliver to application
                System.out.println("Delivering the data to the application");
            }
            this.commitLength = leaderCommitLength;
        }

        return new AckResponse(this.logs.size());
    }
}
