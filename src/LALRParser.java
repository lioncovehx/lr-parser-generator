import java.util.*;

public class LALRParser {

    private List<Set<LR1Items>> canonicalLR1States;
    private Map<String, Integer> coreToLALRStateIndex;

    /**
     * Builds LALR(1) states by:
     * 1. Building canonical LR(1) states
     * 2. Grouping states by core (LR(0) items without lookaheads)
     * 3. Merging lookaheads for states with the same core
     */
    public List<Set<LR1Items>> buildLALRStates(Grammar grammar, LR1Parser lr1Parser) {
        // Step 1: Build canonical LR(1) states
        canonicalLR1States = buildCanonicalLR1States(grammar, lr1Parser);

        // Step 2: Group states by core and build LALR(1) states
        Map<String, List<Set<LR1Items>>> coreGroups = new LinkedHashMap<>();
        for (Set<LR1Items> state : canonicalLR1States) {
            String core = getCore(state);
            coreGroups.computeIfAbsent(core, k -> new ArrayList<>()).add(state);
        }

        // Step 3: Merge each group and build index map
        List<Set<LR1Items>> lalrStates = new ArrayList<>();
        coreToLALRStateIndex = new HashMap<>();
        int lalrIndex = 0;
        for (List<Set<LR1Items>> group : coreGroups.values()) {
            Set<LR1Items> merged = mergeStates(group);
            lalrStates.add(merged);
            coreToLALRStateIndex.put(getCore(merged), lalrIndex);
            lalrIndex++;
        }

        return lalrStates;
    }

    /**
     * Compute GOTO for LALR(1) states.
     * Uses canonical LR(1) GOTO on all states in the core group,
     * then merges the results.
     */
    public Set<LR1Items> lalrGoto(Set<LR1Items> lalrState, String X, Grammar grammar, LR1Parser lr1Parser) {
        // Find all canonical states whose core matches this LALR state
        String core = getCore(lalrState);
        List<Set<LR1Items>> canonicalStatesInGroup = new ArrayList<>();
        for (Set<LR1Items> canonState : canonicalLR1States) {
            if (getCore(canonState).equals(core)) {
                canonicalStatesInGroup.add(canonState);
            }
        }

        // Compute GOTO on each canonical state
        Set<Set<LR1Items>> resultingCanonicalStates = new HashSet<>();
        for (Set<LR1Items> canonState : canonicalStatesInGroup) {
            Set<LR1Items> gotoResult = lr1Parser.lr1Goto(canonState, X, grammar);
            if (!gotoResult.isEmpty()) {
                resultingCanonicalStates.add(gotoResult);
            }
        }

        // Merge all resulting canonical states by core
        List<Set<LR1Items>> resultGroups = new ArrayList<>();
        for (Set<LR1Items> resultCanon : resultingCanonicalStates) {
            resultGroups.add(resultCanon);
        }

        return mergeStates(resultGroups);
    }

    /**
     * Find the LALR(1) state index for a given set of LR(1) items.
     * Matches by core.
     */
    public int findLALRStateIndex(List<Set<LR1Items>> lalrStates, Set<LR1Items> target) {
        String targetCore = getCore(target);
        for (int i = 0; i < lalrStates.size(); i++) {
            if (getCore(lalrStates.get(i)).equals(targetCore)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Build canonical LR(1) states using the same algorithm as LR1Parser.
     */
    private List<Set<LR1Items>> buildCanonicalLR1States(Grammar grammar, LR1Parser lr1Parser) {
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

    /**
     * Get the "core" of an LR(1) state — the LR(0) items without lookaheads.
     * Used to group states that should be merged.
     */
    private String getCore(Set<LR1Items> state) {
        List<String> coreItems = new ArrayList<>();
        for (LR1Items item : state) {
            coreItems.add(item.lhs + " -> " + String.join(" ", item.rhs) + " | " + item.dotPosition);
        }
        Collections.sort(coreItems);
        return String.join(";", coreItems);
    }

    /**
     * Merge a list of LR(1) states that share the same core.
     * For each core item, union all lookaheads from the constituent states.
     */
    private Set<LR1Items> mergeStates(List<Set<LR1Items>> states) {
        Set<LR1Items> merged = new LinkedHashSet<>();

        // Get all unique core items across all states
        Set<String> coreItemKeys = new LinkedHashSet<>();
        for (Set<LR1Items> state : states) {
            for (LR1Items item : state) {
                coreItemKeys.add(getCoreItemKey(item));
            }
        }

        // For each unique core item, collect all lookaheads
        for (String coreKey : coreItemKeys) {
            Set<String> lookaheads = new HashSet<>();
            LR1Items templateItem = null;

            for (Set<LR1Items> state : states) {
                for (LR1Items item : state) {
                    if (getCoreItemKey(item).equals(coreKey)) {
                        lookaheads.add(item.lookahead);
                        templateItem = item;
                    }
                }
            }

            // Create one LR(1) item per lookahead
            for (String la : lookaheads) {
                merged.add(new LR1Items(
                    templateItem.lhs,
                    templateItem.rhs,
                    templateItem.dotPosition,
                    la
                ));
            }
        }

        return merged;
    }

    /**
     * Get a key representing the core (LR(0) part) of an LR(1) item.
     */
    private String getCoreItemKey(LR1Items item) {
        return item.lhs + " -> " + String.join(" ", item.rhs) + " | " + item.dotPosition;
    }

    /**
     * Print summary of LALR(1) state construction.
     */
    public void printLALRSummary(int canonicalStateCount, int lalrStateCount) {
        System.out.println("\n--- LALR(1) State Construction Summary ---");
        System.out.println("Canonical LR(1) states: " + canonicalStateCount);
        System.out.println("LALR(1) states (after merging): " + lalrStateCount);
        System.out.println("States merged away: " + (canonicalStateCount - lalrStateCount));
    }
}
