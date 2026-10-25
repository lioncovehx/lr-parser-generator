import java.util.ArrayList;
import java.util.List;

public class Tree {
    public static class Node {
        String label;
        List<Node> children = new ArrayList<>();

        Node(String label) {
            this.label = label;
        }

        public void print(String prefix, boolean isTail) {
            System.out.println(prefix + (isTail ? "+-- " : "|-- ") + label);
            for (int i = 0; i < children.size() - 1; i++) {
                children.get(i).print(prefix + (isTail ? "    " : "|   "), false);
            }
            if (!children.isEmpty()) {
                children.get(children.size() - 1).print(prefix + (isTail ? "    " : "|   "), true);
            }
        }
    }
}

