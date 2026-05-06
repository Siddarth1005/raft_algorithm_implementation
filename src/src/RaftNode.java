import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RaftNode {
    Integer nodeID;
    Role currentRole;
    List<Log> logs;
    List<RaftNode> nodes = new ArrayList<>();
    RaftNode votedFor = null;
    Set<Integer> votesReceived;
    int currentTerm;
    RaftNode currentLeader;

    public RaftNode(int nodeID,List<RaftNode> nodes) {
        this.nodeID = nodeID;
        this.currentRole = Role.FOLLOWER;
        this.logs = new ArrayList<>();;
        this.nodes = nodes;
        this.votedFor = null;
        this.votesReceived = new HashSet<>();
        this.currentTerm = 0;
        this.currentLeader = null;
    }

    public void crashRecovery(int currentTerm, RaftNode votedFor, List<Log> logs) {
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
        this.votedFor = this;
        currentTerm = this.currentTerm + 1;
        int lastTerm = logs.isEmpty() ? 0 : logs.getLast().term;
        votesReceived.add(this.nodeID);
        VoteRequest request = new VoteRequest(logs.size(), currentTerm, this.nodeID, lastTerm);
        for(RaftNode node : nodes ) node.recieveVoteRequest(request);
    }

//    stage 2
    public void  recieveVoteRequest(VoteRequest voteRequest){
        return;
    }

}
