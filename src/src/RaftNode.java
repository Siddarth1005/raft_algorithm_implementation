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
    Set<VoteResponse> votesReceived;
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
        votesReceived.add(new VoteResponse(true, this.nodeID, this.currentTerm));
        VoteRequest request = new VoteRequest(logs.size(), currentTerm, this.nodeID, loglastTerm);
        for(RaftNode node : nodes ) if(node.nodeID != this.nodeID) votesReceived.add(node.recieveVoteRequest(request));
        boolean result = checkIfLeader(votesReceived);
        if(result){
            this.currentRole = Role.LEADER;
//            publish the data to all the nodes
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

}
