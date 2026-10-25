import java.util.*;

public class SLRParser {

    public void parse(String input, ParsingTable table, Grammar grammar) {
        Stack<Object> stack = new Stack<>();
        Stack<Tree.Node> nodeStack = new Stack<>(); // Task 3.3: Stack to build the tree

        stack.push(0); // Start state
        
        // Split input into tokens (e.g., "id", "+", "id")
        List<String> tokens = new ArrayList<>(Arrays.asList(input.trim().split("\\s+")));
        if (!tokens.get(tokens.size() - 1).equals("$")) {
            tokens.add("$"); // Append $
        }
        
        int step = 1;
        int index = 0;

        while (true) {
            int currentState = (int) stack.peek(); //
            String currentSymbol = tokens.get(index);
            
            String action = table.actionTable.getOrDefault(currentState, new HashMap<>()).getOrDefault(currentSymbol, "");

            // Display current status
            String inputLeft = String.join(" ", tokens.subList(index, tokens.size()));
            System.out.printf("%-5d %-40s %-25s %-15s\n", step++, stack.toString(), inputLeft, action);

            if (action.startsWith("s")) { // SHIFT
                int nextState = Integer.parseInt(action.substring(1));
                
                // Task 3.3: Push leaf node for the terminal
                nodeStack.push(new Tree.Node(currentSymbol));

                stack.push(currentSymbol); // Push symbol
                stack.push(nextState);   // Push state
                index++; // Advance input
            } 
            else if (action.startsWith("r")) { // REDUCE
                String content = action.substring(2, action.length() - 1);
                String[] parts = content.split("->", 2);
                String lhs = parts[0];
                String rhsStr = (parts.length > 1) ? parts[1] : "";

                // Get actual symbol count from grammar
                int rhsSize = grammar.getRHSCount(lhs, rhsStr);
                
                // Task 3.3: Create parent node and attach children
                Tree.Node parentNode = new Tree.Node(lhs);
                List<Tree.Node> children = new ArrayList<>();
                for (int i = 0; i < rhsSize; i++) {
                    // Pop children in reverse order
                    children.add(0, nodeStack.pop());
                }
                parentNode.children.addAll(children);
                nodeStack.push(parentNode);
                
                // Pop 2 * |beta| from the state stack
                for (int i = 0; i < 2 * rhsSize; i++) {
                    stack.pop();
                }
                
                // GOTO logic
                int topState = (int) stack.peek();
                Integer nextState = table.gotoTable.getOrDefault(topState, new HashMap<>()).get(lhs);

                if (nextState == null) {
                    System.out.println("\nResult: Error - No GOTO for " + lhs + " from state " + topState);
                    break;
                }
                
                stack.push(lhs);
                stack.push(nextState);
            } 
            else if (action.equals("acc")) { // ACCEPT
                System.out.println("\nResult: String accepted successfully!");
                
                // Task 3.3: Display the final tree
                System.out.println("\n--- Task 3.3: Generated Parse Tree ---");
                if (!nodeStack.isEmpty()) {
                    nodeStack.peek().print("", true);
                }
                break;
            } 
            else { // ERROR
                System.out.println("\nResult: Error - String rejected.");
                break;
            }
        }
    }

}