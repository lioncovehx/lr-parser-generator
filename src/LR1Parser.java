import java.util.*;

public class LR1Parser {

    public Set<LR1Items> lr1Closure(Set<LR1Items> items, Grammar grammar) {
        Set<LR1Items> closureSet = new LinkedHashSet<>(items); // cite: 136
        boolean added = true;

        while (added) {
            added = false;
            Set<LR1Items> tempSet = new LinkedHashSet<>(closureSet);
            for (LR1Items item : tempSet) {
                if (item.dotPosition < item.rhs.size()) {
                    String symbolAtDot = item.rhs.get(item.dotPosition);
                    
                    if (grammar.getNonTerminals().contains(symbolAtDot)) {
                        List<String> beta = new ArrayList<>(item.rhs.subList(item.dotPosition + 1, item.rhs.size()));
                        beta.add(item.lookahead); // This is beta + a
                        
                        Set<String> lookaheads = grammar.getFirstOfSequence(beta); // cite: 141
                        
                        for (List<String> prod : grammar.getProductions(symbolAtDot)) {
                            for (String b : lookaheads) {
                                LR1Items newItem = new LR1Items(symbolAtDot, prod, 0, b); // cite: 143
                                if (!closureSet.contains(newItem)) {
                                    closureSet.add(newItem);
                                    added = true;
                                }
                            }
                        }
                    }
                }
            }
        }
        return closureSet;
    }

    public Set<LR1Items> lr1Goto(Set<LR1Items> items, String X, Grammar grammar) {
        Set<LR1Items> movedItems = new LinkedHashSet<>();
        for (LR1Items item : items) {
            if (item.dotPosition < item.rhs.size() && item.rhs.get(item.dotPosition).equals(X)) {
                // Move dot past X, keep lookahead 'a' [cite: 148]
                movedItems.add(new LR1Items(item.lhs, item.rhs, item.dotPosition + 1, item.lookahead));
            }
        }
        return lr1Closure(movedItems, grammar);
    }
}