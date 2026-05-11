public class AckResponse {
    boolean response;
    int logLength;
    int term;

    public AckResponse(boolean response, int logLength, int term) {
        this.response = response;
        this.term = term;
        this.logLength = logLength;
    }
}
