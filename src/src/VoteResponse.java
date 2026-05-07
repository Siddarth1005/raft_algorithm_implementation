public class VoteResponse {

    Boolean voted;
    Integer nodeID;
    Integer currentTerm;

    public VoteResponse(Boolean voted, Integer nodeID, Integer currentTerm) {
        this.voted = voted;
        this.nodeID = nodeID;
        this.currentTerm = currentTerm;
    }

    public Boolean getVoted() {
        return voted;
    }

    public Integer getNodeID() {
        return nodeID;
    }

    public Integer getCurrentTerm() {
        return currentTerm;
    }

    public void setVoted(Boolean voted) {
        this.voted = voted;
    }

    public void setNodeID(Integer nodeID) {
        this.nodeID = nodeID;
    }

    public void setCurrentTerm(Integer currentTerm) {
        this.currentTerm = currentTerm;
    }
}
