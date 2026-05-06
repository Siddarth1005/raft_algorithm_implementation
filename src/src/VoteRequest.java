public class VoteRequest {
    int currentLogLength;
    int currentTerm;
    int candidateID;
    int lastTerm;

    public VoteRequest(int currentLogLength, int currentTerm, int candidateID, int lastTerm) {
        this.currentLogLength = currentLogLength;
        this.currentTerm = currentTerm;
        this.candidateID = candidateID;
        this.lastTerm = lastTerm;
    }

    public int getCurrentLogLength() {
        return currentLogLength;
    }

    public int getCurrentTerm() {
        return currentTerm;
    }

    public int getCandidateID() {
        return candidateID;
    }

    public int getLastTerm() {
        return lastTerm;
    }

    public void setCurrentLogLength(int currentLogLength) {
        this.currentLogLength = currentLogLength;
    }

    public void setCurrentTerm(int currentTerm) {
        this.currentTerm = currentTerm;
    }

    public void setCandidateID(int candidateID) {
        this.candidateID = candidateID;
    }

    public void setLastTerm(int lastTerm) {
        this.lastTerm = lastTerm;
    }
}
