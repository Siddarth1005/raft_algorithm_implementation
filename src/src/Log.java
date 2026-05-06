public class Log {
    int messageID;
    String message;
    int term;

    public Log(int messageID, String message, int term) {
        this.messageID = messageID;
        this.message = message;
        this.term = term;
    }

    public int getMessageID() {''
        return messageID;
    }

    public String getMessage() {
        return message;
    }

    public int getTerm() {
        return term;
    }

    public void setTerm(int term) {
        this.term = term;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setMessageID(int messageID) {
        this.messageID = messageID;
    }
}