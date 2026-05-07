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
    Integer votedFor = null;
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
        votesReceived.add(this.nodeID);
        VoteRequest request = new VoteRequest(logs.size(), currentTerm, this.nodeID, loglastTerm);
        for(RaftNode node : nodes ) node.recieveVoteRequest(request);
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

}
