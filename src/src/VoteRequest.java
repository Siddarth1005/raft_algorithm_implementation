public class VoteRequest {
    int currentLogLength;
    int currentTerm;
    int candidateID;
    int logLastTerm;

    public VoteRequest(int currentLogLength, int currentTerm, int candidateID, int logLastTerm) {
        this.currentLogLength = currentLogLength;
        this.currentTerm = currentTerm;
        this.candidateID = candidateID;
        this.logLastTerm = logLastTerm;
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
        return logLastTerm;
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

    public void setLastTerm(int logLastTerm) {
        this.logLastTerm = logLastTerm;
    }
}
