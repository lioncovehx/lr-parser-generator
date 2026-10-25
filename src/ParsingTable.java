import java.util.*;

public class ParsingTable {
    // Structure: Map<StateIndex, Map<Symbol, ActionString>>
    public Map<Integer, Map<String, String>> actionTable = new TreeMap<>();
    public Map<Integer, Map<String, Integer>> gotoTable = new TreeMap<>();
    public boolean hasConflict = false;

    public void buildSLRTable(List<Set<Items>> states, Grammar grammar) {
        actionTable.clear();
        gotoTable.clear();
        hasConflict = false;

        Map<String, Set<String>> followSets = grammar.computeFollowSets();

        for (int i = 0; i < states.size(); i++) {
            int stateNum = i; // Numbering starts at 10
            Set<Items> currentState = states.get(i);

            for (Items item : currentState) {
                // 1. Accept Action [cite: 86-89]
                if (item.lhs.equals(grammar.getStartSymbol()) && item.dotPosition == item.rhs.size()) {
                    addAction(stateNum, "$", "acc");
                } 
                // 2. Reduce Action [cite: 81-85]
                else if (item.dotPosition == item.rhs.size()) {
                    Set<String> follows = followSets.get(item.lhs);
                    for (String terminal : follows) {
                        addAction(stateNum, terminal, "r(" + item.lhs + "->" + String.join(" ", item.rhs) + ")");
                    }
                } 
                // 3. Shift & GOTO Actions [cite: 77-80, 90-93]
                else {
                    String symbol = item.rhs.get(item.dotPosition);
                    Set<Items> nextStateSet = grammar.goTo(currentState, symbol);
                    int nextStateIdx = findStateIndex(states, nextStateSet);

                    if (grammar.getTerminals().contains(symbol)) {
                        addAction(stateNum, symbol, "s" + nextStateIdx);
                    } else {
                        gotoTable.computeIfAbsent(stateNum, k -> new HashMap<>()).put(symbol, nextStateIdx);
                    }
                }
            }
        }
    }

    private void addAction(int state, String symbol, String action) {
        actionTable.computeIfAbsent(state, k -> new HashMap<>());
        if (actionTable.get(state).containsKey(symbol)) {
            String existing = actionTable.get(state).get(symbol);
            if (!existing.equals(action)) {
                System.out.println("CONFLICT at State " + state + " on " + symbol + ": " + existing + " vs " + action);
                hasConflict = true;
            }
        }
        actionTable.get(state).put(symbol, action);
    }

    private int findStateIndex(List<Set<Items>> states, Set<Items> target) {
        for (int i = 0; i < states.size(); i++) {
            if (states.get(i).equals(target)) return i;
        }
        return -1;
    }

    public void displayTable(Grammar grammar) {
        List<String> terminals = new ArrayList<>(grammar.getTerminals());
        List<String> nonTerminals = new ArrayList<>(grammar.getNonTerminals());
        nonTerminals.remove(grammar.getStartSymbol());

        // Header
        System.out.printf("%-8s", "State");
        for (String t : terminals) System.out.printf("%-12s", t);
        for (String nt : nonTerminals) System.out.printf("%-10s", nt);
        System.out.println("\n" + "=".repeat(100));

        // Rows (sorted)
        Set<Integer> allStates = new TreeSet<>();
        allStates.addAll(actionTable.keySet());
        allStates.addAll(gotoTable.keySet());
        for (int state : allStates) {
            System.out.printf("%-8d", state);
            for (String t : terminals) {
                System.out.printf("%-12s", actionTable.getOrDefault(state, new HashMap<>()).getOrDefault(t, "-"));
            }
            for (String nt : nonTerminals) {
                Integer g = gotoTable.getOrDefault(state, new HashMap<>()).get(nt);
                System.out.printf("%-10s", (g == null ? "-" : g.toString()));
            }
            System.out.println();
        }
    }


    public void buildLR1Table(List<Set<LR1Items>> states, Grammar grammar, LR1Parser parser) {
        actionTable.clear();
        gotoTable.clear();
        hasConflict = false;

        for (int i = 0; i < states.size(); i++) {
            int stateNum = i; // ADD THIS OFFSET HERE
            Set<LR1Items> currentState = states.get(i);
            for (LR1Items item : currentState) {
                if (item.dotPosition < item.rhs.size()) {
                    String symbol = item.rhs.get(item.dotPosition);
                    Set<LR1Items> nextStateSet = parser.lr1Goto(currentState, symbol, grammar);
                    int nextIdx = findLR1StateIndex(states, nextStateSet);
                    
                    if (grammar.getTerminals().contains(symbol)) {
                        addAction(stateNum, symbol, "s" + nextIdx); // USE stateNum
                    } else {
                        setGoto(stateNum, symbol, nextIdx); // USE stateNum
                    }
                } else {
                    if (item.lhs.equals(grammar.getStartSymbol()) && item.lookahead.equals("$")) {
                        addAction(stateNum, "$", "acc"); // USE stateNum
                    } else {
                        String reduction = "r(" + item.lhs + "->" + String.join(" ", item.rhs) + ")";
                        addAction(stateNum, item.lookahead, reduction); // USE stateNum
                    }
                }
            }
        }
    }

    private int findLR1StateIndex(List<Set<LR1Items>> states, Set<LR1Items> target) {
        for (int i = 0; i < states.size(); i++) {
            if (states.get(i).equals(target)) return i;
        }
        return -1;
    }


    public void setGoto(int state, String nonTerminal, int nextState) {
        // We use computeIfAbsent to ensure the inner map exists before we put the value
        gotoTable.computeIfAbsent(state, k -> new HashMap<>()).put(nonTerminal, nextState);
    }


    public void buildLALRTable(List<Set<LR1Items>> states, Grammar grammar, LALRParser lalrParser, LR1Parser lr1Parser) {
        actionTable.clear();
        gotoTable.clear();
        hasConflict = false;

        for (int i = 0; i < states.size(); i++) {
            int stateNum = i;
            Set<LR1Items> currentState = states.get(i);
            for (LR1Items item : currentState) {
                if (item.dotPosition < item.rhs.size()) {
                    String symbol = item.rhs.get(item.dotPosition);
                    Set<LR1Items> nextStateSet = lalrParser.lalrGoto(currentState, symbol, grammar, lr1Parser);
                    int nextIdx = lalrParser.findLALRStateIndex(states, nextStateSet);
                    
                    if (grammar.getTerminals().contains(symbol)) {
                        addAction(stateNum, symbol, "s" + nextIdx);
                    } else {
                        setGoto(stateNum, symbol, nextIdx);
                    }
                } else {
                    if (item.lhs.equals(grammar.getStartSymbol()) && item.lookahead.equals("$")) {
                        addAction(stateNum, "$", "acc");
                    } else {
                        String reduction = "r(" + item.lhs + "->" + String.join(" ", item.rhs) + ")";
                        addAction(stateNum, item.lookahead, reduction);
                    }
                }
            }
        }
    }


}