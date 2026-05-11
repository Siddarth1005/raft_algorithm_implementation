import java.util.ArrayList;
import java.util.List;

public class Main {

    public static void main(String[] args) {
        System.out.println("=== Setting up 5 node Raft cluster ===");
        List<RaftNode> nodes = new ArrayList<>();
        for (int i = 0; i < 5; i++) nodes.add(new RaftNode(i, nodes));

        System.out.println("=== Test 1: Leader Election ===");
        test1(nodes);

        System.out.println("\n=== Test 2: Basic Message Broadcast ===");
        test2(nodes);

        System.out.println("\n=== Test 3: Follower forwards to leader ===");
        test3(nodes);

        System.out.println("\n=== Test 4: Duplicate message ignored ===");
        test4(nodes);

        System.out.println("\n=== Test 5: Multiple messages in order ===");
        test5(nodes);
    }

    // Test 1: Node 0 becomes candidate and wins election
    static void test1(List<RaftNode> nodes) {
        RaftNode node0 = nodes.get(0);
        System.out.println("Node 0 starting election...");
        node0.becomeCandidate();
        System.out.println("Node 0 role: " + node0.currentRole + " (expected: LEADER)");
        System.out.println("Node 0 term: " + node0.currentTerm + " (expected: 1)");
        for (RaftNode node : nodes) {
            System.out.println("Node " + node.nodeID + " currentLeader: " +
                    (node.currentLeader != null ? node.currentLeader.nodeID : "null") +
                    " (expected: 0)");
        }
    }

    // Test 2: Leader broadcasts a message, all nodes deliver it
    static void test2(List<RaftNode> nodes) {
        RaftNode leader = nodes.get(0);
        System.out.println("Leader broadcasting message 1...");
        leader.broadcastMessage(new Message("Hello Raft!", 1));
        System.out.println("Leader log size: " + leader.logs.size() + " (expected: 1)");
        System.out.println("Leader commitLength: " + leader.commitLength + " (expected: 1)");
        for (RaftNode node : nodes) {
            System.out.println("Node " + node.nodeID +
                    " log size: " + node.logs.size() +
                    " commitLength: " + node.commitLength);
        }
    }

    // Test 3: Follower receives message and forwards to leader
    static void test3(List<RaftNode> nodes) {
        RaftNode follower = nodes.get(1);
        System.out.println("Follower 1 broadcasting message 2...");
        follower.broadcastMessage(new Message("From follower", 2));
        System.out.println("Leader log size: " + nodes.get(0).logs.size() + " (expected: 2)");
        for (RaftNode node : nodes) {
            System.out.println("Node " + node.nodeID +
                    " log size: " + node.logs.size() +
                    " commitLength: " + node.commitLength);
        }
    }

    // Test 4: Duplicate message is ignored
    static void test4(List<RaftNode> nodes) {
        RaftNode leader = nodes.get(0);
        int logSizeBefore = leader.logs.size();
        System.out.println("Sending duplicate message (messageID=1)...");
        leader.broadcastMessage(new Message("Hello Raft!", 1));
        System.out.println("Log size before: " + logSizeBefore +
                " after: " + leader.logs.size() +
                " (expected: same, duplicate ignored)");
    }

    // Test 5: Multiple messages broadcast in order
    static void test5(List<RaftNode> nodes) {
        RaftNode leader = nodes.get(0);
        System.out.println("Broadcasting 3 messages in order...");
        leader.broadcastMessage(new Message("Message A", 3));
        leader.broadcastMessage(new Message("Message B", 4));
        leader.broadcastMessage(new Message("Message C", 5));
        System.out.println("Leader log size: " + leader.logs.size() + " (expected: 5)");
        System.out.println("Leader commitLength: " + leader.commitLength + " (expected: 5)");
        System.out.println("Verifying all nodes have same log...");
        for (RaftNode node : nodes) {
            System.out.println("Node " + node.nodeID +
                    " log size: " + node.logs.size() +
                    " commitLength: " + node.commitLength);
        }
    }
}