import java.util.*;
import java.io.*;

public class Grammar {
    private Map<String, List<List<String>>> productions = new LinkedHashMap<>();
    private Set<String> terminals = new HashSet<>();
    private Set<String> nonTerminals = new HashSet<>();
    private String startSymbol = null;

    public void loadFromFile(String filename) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(filename));
        String line;

        while ((line = reader.readLine()) != null) {
            if (line.trim().isEmpty()) continue;

            String[] parts = line.split("->");
            if (parts.length != 2) {
                throw new IllegalArgumentException("Invalid production format: " + line);
            }
            String lhs = parts[0].trim();
            
            // Validate LHS (Must be multi-character, starting with Uppercase)
            if (lhs.length() < 2 || !Character.isUpperCase(lhs.charAt(0))) {
                throw new IllegalArgumentException("Invalid Non-Terminal: " + lhs);
            }

            if (startSymbol == null) startSymbol = lhs;
            nonTerminals.add(lhs);

            String[] rhsAlternatives = parts[1].split("\\|");
            for (String alt : rhsAlternatives) {
                List<String> symbols = new ArrayList<>();
                for (String symbol : alt.trim().split("\\s+")) {
                    if (symbol.equals("epsilon") || symbol.equals("@")) {
                        symbols.add("@"); 
                    } else {
                        symbols.add(symbol);
                        identifySymbol(symbol);
                    }
                }
                productions.computeIfAbsent(lhs, k -> new ArrayList<>()).add(symbols);
            }
        }
        reader.close();
        // Add $ to terminals as it's the end-of-input marker [cite: 89]
        terminals.add("$");
    }

    private void identifySymbol(String s) {
        if (s.length() >= 1 && Character.isUpperCase(s.charAt(0))) {
            nonTerminals.add(s);
        } else {
            terminals.add(s); 
        }
    }

    public void augment() {
        String oldStart = this.startSymbol;
        String augmentedStart = oldStart + "Prime"; //

        List<String> rhs = new ArrayList<>();
        rhs.add(oldStart);
        
        List<List<String>> newProductions = new ArrayList<>();
        newProductions.add(rhs);
        
        Map<String, List<List<String>>> updatedMap = new LinkedHashMap<>();
        updatedMap.put(augmentedStart, newProductions);
        updatedMap.putAll(this.productions);
        
        this.productions = updatedMap;
        this.nonTerminals.add(augmentedStart);
        this.startSymbol = augmentedStart;
    }

    public void displayGrammar() {
        for (Map.Entry<String, List<List<String>>> entry : productions.entrySet()) {
            System.out.print(entry.getKey() + " -> ");
            for (int i = 0; i < entry.getValue().size(); i++) {
                System.out.print(String.join(" ", entry.getValue().get(i)));
                if (i < entry.getValue().size() - 1) System.out.print(" | ");
            }
            System.out.println();
        }
    }

    // Task 1.3: CLOSURE Operation [cite: 54-59]
    public Set<Items> closure(Set<Items> items) {
        Set<Items> closureSet = new LinkedHashSet<>(items);
        boolean added = true;

        while (added) {
            added = false;
            Set<Items> tempSet = new LinkedHashSet<>(closureSet);
            for (Items item : tempSet) {
                if (item.dotPosition < item.rhs.size()) {
                    String symbolAtDot = item.rhs.get(item.dotPosition);
                    
                    if (nonTerminals.contains(symbolAtDot)) {
                        for (List<String> prod : productions.get(symbolAtDot)) {
                            Items newItem = new Items(symbolAtDot, prod, 0);
                            if (!closureSet.contains(newItem)) {
                                closureSet.add(newItem);
                                added = true;
                            }
                        }
                    }
                }
            }
        }
        return closureSet;
    }

    // Task 1.3: GOTO Operation [cite: 60-64]
    public Set<Items> goTo(Set<Items> items, String X) {
        Set<Items> movedItems = new LinkedHashSet<>();
        for (Items item : items) {
            if (item.dotPosition < item.rhs.size() && item.rhs.get(item.dotPosition).equals(X)) {
                movedItems.add(new Items(item.lhs, item.rhs, item.dotPosition + 1));
            }
        }
        return closure(movedItems);
    }

    // --- GETTERS (CRITICAL for Main.java) ---
    
    public String getStartSymbol() {
        return startSymbol;
    }

    public List<List<String>> getProductions(String lhs) {
        return productions.get(lhs);
    }

    public Set<String> getNonTerminals() {
        return nonTerminals;
    }

    public Set<String> getTerminals() {
        return terminals;
    }


public Map<String, Set<String>> computeFirstSets() {
        Map<String, Set<String>> firstSets = new HashMap<>();
        for (String nt : nonTerminals) {
            firstSets.put(nt, new HashSet<>());
        }

        boolean changed = true;
        while (changed) {
            changed = false;
            for (String lhs : productions.keySet()) {
                for (List<String> prod : productions.get(lhs)) {
                    int before = firstSets.get(lhs).size();

                    boolean allNullable = true;
                    for (String symbol : prod) {
                        if (symbol.equals("@")) {
                            firstSets.get(lhs).add("@");
                            allNullable = false;
                            break;
                        }

                        if (terminals.contains(symbol) || !nonTerminals.contains(symbol)) {
                            firstSets.get(lhs).add(symbol);
                            allNullable = false;
                            break;
                        }

                        Set<String> firstOfSymbol = new HashSet<>(firstSets.get(symbol));
                        boolean hasEpsilon = firstOfSymbol.remove("@");
                        firstSets.get(lhs).addAll(firstOfSymbol);

                        if (!hasEpsilon) {
                            allNullable = false;
                            break;
                        }
                    }

                    if (allNullable) {
                        firstSets.get(lhs).add("@");
                    }

                    if (firstSets.get(lhs).size() > before) {
                        changed = true;
                    }
                }
            }
        }
        return firstSets;
    }

    public Map<String, Set<String>> computeFollowSets() {
        Map<String, Set<String>> firstSets = computeFirstSets();
        Map<String, Set<String>> followSets = new HashMap<>();
        for (String nt : nonTerminals) followSets.put(nt, new HashSet<>());

        // Rule: S' (Augmented Start) gets $ [cite: 89]
        followSets.get(getStartSymbol()).add("$");

        boolean changed = true;
        while (changed) {
            changed = false;
            for (String lhs : productions.keySet()) {
                for (List<String> prod : productions.get(lhs)) {
                    for (int i = 0; i < prod.size(); i++) {
                        String symbol = prod.get(i);
                        if (nonTerminals.contains(symbol)) {
                            int before = followSets.get(symbol).size();

                            List<String> beta = new ArrayList<>();
                            if (i + 1 < prod.size()) {
                                beta.addAll(prod.subList(i + 1, prod.size()));
                            }

                            if (!beta.isEmpty()) {
                                Set<String> firstBeta = getFirstOfSequence(beta);
                                Set<String> firstBetaWithoutEpsilon = new HashSet<>(firstBeta);
                                firstBetaWithoutEpsilon.remove("@");
                                followSets.get(symbol).addAll(firstBetaWithoutEpsilon);

                                if (firstBeta.contains("@")) {
                                    followSets.get(symbol).addAll(followSets.get(lhs));
                                }
                            } else {
                                followSets.get(symbol).addAll(followSets.get(lhs));
                            }

                            if (followSets.get(symbol).size() > before) changed = true;
                        }
                    }
                }
            }
        }
        return followSets;
    }


    public int getRHSCount(String lhs, String rhsString) {
        List<List<String>> prods = productions.get(lhs);
        if (prods == null) return 0;

        for (List<String> p : prods) {
            // Handle epsilon
            if (p.get(0).equals("@") && (rhsString.isEmpty() || rhsString.equals("@"))) {
                return 0;
            }
            // Check if the joined production matches the string from the table
            if (String.join(" ", p).equals(rhsString.trim())) {
                return p.size();
            }
        }
        return 0;
    }

    public Set<String> getFirstOfSequence(List<String> sequence) {
        Set<String> result = new HashSet<>();
        if (sequence.isEmpty()) {
            result.add("@");
            return result;
        }

        Map<String, Set<String>> allFirstSets = computeFirstSets();

        for (String symbol : sequence) {
            if (symbol.equals("@")) {
                result.add("@");
                return result;
            }

            if (terminals.contains(symbol) || !nonTerminals.contains(symbol)) {
                result.add(symbol);
                return result;
            }
            
            Set<String> firstOfSymbol = allFirstSets.get(symbol);
            if (firstOfSymbol == null) { // Literal terminal check
                result.add(symbol);
                return result;
            }
            
            Set<String> copy = new HashSet<>(firstOfSymbol);
            boolean hasEpsilon = copy.remove("@");
            result.addAll(copy);
            
            if (!hasEpsilon) return result;
        }
        
        result.add("@");
        return result;
    }


}