import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        try {
            String grammarPath = args.length > 0 ? args[0] : "input/grammar2.txt";
            List<String> inputs = resolveInputs(args);

            // Task 1.1: Load the Grammar
            Grammar myGrammar = new Grammar();
            myGrammar.loadFromFile(grammarPath);
            System.out.println("--- Task 1.1: Grammar Loaded Successfully ---");
            System.out.println("Grammar file: " + grammarPath);

            // Task 1.2: Grammar Augmentation [cite: 44, 47]
            myGrammar.augment();
            System.out.println("\n--- Task 1.2: Augmented Grammar ---");
            myGrammar.displayGrammar();

            // Task 1.3: Canonical Collection of LR(0) Items
            List<Set<Items>> states = buildLR0States(myGrammar);

            // Task 1.3 Output: Display all item sets numbered starting at 10
            System.out.println("\n--- Task 1.3: LR(0) Item Sets (States) ---");
            for (int s = 0; s < states.size(); s++) {
                System.out.println("State " + (s) + ":");
                for (Items item : states.get(s)) {
                    System.out.println("  " + item);
                }
                System.out.println("---------------------------");
            }


            // --- Task 1.4: SLR(1) Table ---
            ParsingTable slrTable = new ParsingTable();
            slrTable.buildSLRTable(states, myGrammar); 
            System.out.println("\n--- Task 1.4: SLR(1) Parsing Table ---");
            slrTable.displayTable(myGrammar);

            SLRParser slrEngine = new SLRParser();

            // --- Part 2: LR(1) Parser Implementation ---

            LR1Parser lr1Parser = new LR1Parser();
            List<Set<LR1Items>> lr1States = buildLR1States(myGrammar, lr1Parser);

            // Task 2.1 Output: Display all LR(1) item sets [cite: 158]
            System.out.println("\n--- Task 2.1: LR(1) Item Sets (States) ---");
            for (int s = 0; s < lr1States.size(); s++) {
                System.out.println("State " + s + ":");
                for (LR1Items item : lr1States.get(s)) {
                    System.out.println("  " + item);
                }
                System.out.println("---------------------------");
            }



            // --- Task 2.2: LR(1) Table ---
            ParsingTable lr1Table = new ParsingTable();
            lr1Table.buildLR1Table(lr1States, myGrammar, lr1Parser);
            System.out.println("\n--- Task 2.2: LR(1) Parsing Table ---");
            lr1Table.displayTable(myGrammar);

            // --- Task 3.1: LALR(1) Parser Implementation ---
            LALRParser lalrParser = new LALRParser();
            List<Set<LR1Items>> lalrStates = lalrParser.buildLALRStates(myGrammar, lr1Parser);

            lalrParser.printLALRSummary(lr1States.size(), lalrStates.size());

            // Task 3.1 Output: Display all LALR(1) item sets
            System.out.println("\n--- Task 3.1: LALR(1) Item Sets (States) ---");
            for (int s = 0; s < lalrStates.size(); s++) {
                System.out.println("State " + s + ":");
                for (LR1Items item : lalrStates.get(s)) {
                    System.out.println("  " + item);
                }
                System.out.println("---------------------------");
            }

            // --- Task 3.2: LALR(1) Table ---
            ParsingTable lalrTable = new ParsingTable();
            lalrTable.buildLALRTable(lalrStates, myGrammar, lalrParser, lr1Parser);
            System.out.println("\n--- Task 3.2: LALR(1) Parsing Table ---");
            lalrTable.displayTable(myGrammar);

            // Timing SLR(1)
            long startSLR = System.nanoTime();
            slrTable.buildSLRTable(states, myGrammar);
            long endSLR = System.nanoTime();

            // Timing LR(1)
            long startLR = System.nanoTime();
            lr1Table.buildLR1Table(lr1States, myGrammar, lr1Parser);
            long endLR = System.nanoTime();

            // Timing LALR(1)
            long startLALR = System.nanoTime();
            lalrTable.buildLALRTable(lalrStates, myGrammar, lalrParser, lr1Parser);
            long endLALR = System.nanoTime();

            System.out.println("\n--- Task 3.3: Performance Results ---");
            System.out.println("SLR(1)  States: " + states.size() + " | Time: " + (endSLR - startSLR)/1000000.0 + " ms");
            System.out.println("LR(1)   States: " + lr1States.size() + " | Time: " + (endLR - startLR)/1000000.0 + " ms");
            System.out.println("LALR(1) States: " + lalrStates.size() + " | Time: " + (endLALR - startLALR)/1000000.0 + " ms");
            System.out.println("SLR(1)  Conflicts Found: " + (slrTable.hasConflict ? "Yes" : "No"));
            System.out.println("LR(1)   Conflicts Found: " + (lr1Table.hasConflict ? "Yes" : "No"));
            System.out.println("LALR(1) Conflicts Found: " + (lalrTable.hasConflict ? "Yes" : "No"));

            // Task 1.5 and 2.3: Run traces for each input
            for (String input : inputs) {
                System.out.println("\n====================================================");
                System.out.println("Input: " + input);

                System.out.println("\n--- Task 1.5: SLR(1) Parsing Trace ---");
                System.out.printf("%-5s %-40s %-25s %-15s\n", "Step", "Stack", "Input", "Action");
                slrEngine.parse(input, slrTable, myGrammar);

                System.out.println("\n--- Task 2.3: LR(1) Parsing Trace ---");
                System.out.printf("%-5s %-40s %-25s %-15s\n", "Step", "Stack", "Input", "Action");
                slrEngine.parse(input, lr1Table, myGrammar);

                System.out.println("\n--- Task 3.4: LALR(1) Parsing Trace ---");
                System.out.printf("%-5s %-40s %-25s %-15s\n", "Step", "Stack", "Input", "Action");
                slrEngine.parse(input, lalrTable, myGrammar);
            }



//------------------------------------------------------------------------------------------------------
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.err.println("Grammar Error: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("An unexpected error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static List<Set<Items>> buildLR0States(Grammar grammar) {
        List<Set<Items>> states = new ArrayList<>();

        Set<Items> startItems = new HashSet<>();
        String startSymbol = grammar.getStartSymbol();
        List<List<String>> startProductions = grammar.getProductions(startSymbol);
        startItems.add(new Items(startSymbol, startProductions.get(0), 0));

        Set<Items> i0 = grammar.closure(startItems);
        states.add(i0);

        int statePointer = 0;
        while (statePointer < states.size()) {
            Set<Items> currentState = states.get(statePointer);

            Set<String> allSymbols = new HashSet<>();
            allSymbols.addAll(grammar.getNonTerminals());
            allSymbols.addAll(grammar.getTerminals());

            for (String symbol : allSymbols) {
                Set<Items> nextState = grammar.goTo(currentState, symbol);
                if (!nextState.isEmpty() && !states.contains(nextState)) {
                    states.add(nextState);
                }
            }
            statePointer++;
        }

        return states;
    }

    private static List<Set<LR1Items>> buildLR1States(Grammar grammar, LR1Parser lr1Parser) {
        List<Set<LR1Items>> lr1States = new ArrayList<>();

        Set<LR1Items> lr1StartItems = new HashSet<>();
        String augmentedStart = grammar.getStartSymbol();
        List<String> startProd = grammar.getProductions(augmentedStart).get(0);
        lr1StartItems.add(new LR1Items(augmentedStart, startProd, 0, "$"));

        Set<LR1Items> lr1I0 = lr1Parser.lr1Closure(lr1StartItems, grammar);
        lr1States.add(lr1I0);

        int lr1Pointer = 0;
        while (lr1Pointer < lr1States.size()) {
            Set<LR1Items> currentState = lr1States.get(lr1Pointer);

            Set<String> allSymbols = new HashSet<>();
            allSymbols.addAll(grammar.getNonTerminals());
            allSymbols.addAll(grammar.getTerminals());

            for (String symbol : allSymbols) {
                Set<LR1Items> nextState = lr1Parser.lr1Goto(currentState, symbol, grammar);
                if (!nextState.isEmpty() && !lr1States.contains(nextState)) {
                    lr1States.add(nextState);
                }
            }
            lr1Pointer++;
        }

        return lr1States;
    }

    private static List<String> resolveInputs(String[] args) throws IOException {
        if (args.length <= 1) {
            return Collections.singletonList("id + id * id $");
        }

        Path inputPath = Path.of(args[1]);
        if (Files.exists(inputPath) && Files.isRegularFile(inputPath)) {
            List<String> lines = Files.readAllLines(inputPath);
            List<String> inputs = new ArrayList<>();
            for (String line : lines) {
                String trimmed = line.trim();
                if (!trimmed.isEmpty()) {
                    inputs.add(trimmed);
                }
            }
            if (!inputs.isEmpty()) {
                return inputs;
            }
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            if (i > 1) sb.append(' ');
            sb.append(args[i]);
        }
        return Collections.singletonList(sb.toString());
    }
}