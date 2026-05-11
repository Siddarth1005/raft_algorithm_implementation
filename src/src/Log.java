public class Log {
    Message message;
    int term;

    public Log(Message message, Integer term) {
        this.message = message;
        this.term = term;
    }

    public int getMessageID() {
        return this.message.messageID;
    }

    public String getMessage() {
        return this.message.message;
    }

    public int getTerm() {
        return term;
    }

    public void setTerm(int term) {
        this.term = term;
    }

    public void setMessage(String message) {
        this.message.message = message;
    }

    public void setMessageID(int messageID) {
        this.message.messageID = messageID;
    }
}