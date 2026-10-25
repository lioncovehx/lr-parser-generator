# SLR(1), LR(1) and LALR(1) Parsers

The third assignment for Compiler Construction (CS4031, FAST-NUCES), by Saad Lone and Mobeen Hassan.

This is the bottom-up follow-up to our [LL(1) parser](https://github.com/lioncovehx/ll1-parser-generator). From a grammar file it builds three kinds of shift-reduce parsers, SLR(1), canonical LR(1) and LALR(1), and parses strings with each one. Then it compares them on the same grammars. Everything is written from scratch in Java.

## What it does

- **Augments the grammar** with a new start rule `S' -> S`.
- **SLR(1):** builds the LR(0) item sets, then the ACTION/GOTO table, using FOLLOW sets to decide where to reduce.
- **LR(1):** builds the item sets with a lookahead token on each item, then its table.
- **LALR(1):** merges LR(1) states that have the same core. This gives a table about the size of SLR's, but with LR(1)'s more precise lookaheads.
- **Conflict reporting:** it prints every shift/reduce and reduce/reduce conflict, with the state and symbol where it happens.
- **Parsing:** for each input string it prints a step-by-step trace (stack, input, action) and draws a parse tree when the string is accepted.
- **Timing:** it measures how long each table takes to build.

## Results

We ran each test grammar through all three parsers:

| Grammar | SLR(1) states | LR(1) states | LALR(1) states | Conflicts (SLR / LR / LALR) |
|---|---|---|---|---|
| `grammar1.txt`, simple expression | 7 | 7 | 7 | none |
| `grammar2.txt`, `+ * ( )` expressions | 12 | 22 | 12 | none |
| `grammar3.txt`, `L = R` assignment grammar | 10 | 14 | 10 | **yes** / no / no |
| `grammar_lalr_demo.txt` | 11 | 11 | 11 | **yes** / no / no |
| `grammar_with_conflict.txt`, dangling else | 10 | 17 | 10 | yes / yes / yes |

What the table shows:

- **On the expression grammar, LR(1) needs almost twice as many states as SLR (22 vs 12).** LALR merges them back down to 12 without adding any conflicts. On every grammar here, LALR ends up with the same number of states as SLR but keeps LR(1)'s lookaheads. That's why real parser generators like yacc and bison use LALR.
- **The `L = R` grammar is the textbook case SLR can't handle.** `=` is in FOLLOW(R), so SLR tries to reduce in the wrong place and gets a shift/reduce conflict. LR(1) and LALR use the actual lookahead and have no conflict. So `id = id` and `* id = id` get rejected by SLR but accepted by LR(1) and LALR.
- **The dangling-else grammar is ambiguous**, so all three report a conflict. That's the right answer, because no LR parser can resolve it without an extra rule.
- On the expression grammar, all three parsers accept every expression string in `input/input_valid.txt` and reject every string in `input/input_invalid.txt` (`id +`, `( id + )`, `id * * id`, …).
- SLR builds fastest (under 1 ms on these grammars). LR(1) and LALR take a few ms, because they compute lookaheads and LALR also merges states.

Saved outputs from our runs are in `output/`: item sets, parsing tables, traces, parse trees and the comparison.

## Run it

You need JDK 11 or newer.

```bash
./build.sh                       # or: javac src/*.java

java -cp src Main                                          # default grammar and inputs
java -cp src Main input/grammar2.txt "id + id * id $"      # one string
java -cp src Main input/grammar2.txt input/input_valid.txt # one string per line
```

The output is long. To see just one parser's trace, you can cut it with `awk`:

```bash
java -cp src Main input/grammar2.txt input/input_valid.txt \
  | awk '/SLR\(1\) Parsing Trace/{f=1} /LR\(1\) Parsing Trace/&&!/SLR/{f=0} f'
```

## Grammar file format

```
Expr -> Expr + Term | Term
Term -> Term * Factor | Factor
Factor -> ( Expr ) | id
```

- One non-terminal per line, `->` and `|` as usual, symbols separated by spaces.
- Non-terminals start with an uppercase letter and are longer than one character.
- Write the empty string as `epsilon` or `@`.
- Input strings are space-separated tokens.

## Code

| File | What it does |
|---|---|
| `Grammar.java` | Reads and augments the grammar, computes FIRST and FOLLOW sets |
| `Items.java`, `LR1Items.java` | LR(0) and LR(1) items |
| `SLRParser.java` | LR(0) item sets and closure/goto |
| `LR1Parser.java` | LR(1) item sets |
| `LALRParser.java` | Merges LR(1) states with the same core |
| `ParsingTable.java` | ACTION/GOTO tables and conflict detection |
| `Stack.java`, `Tree.java` | Parse stack and parse tree |
| `Main.java` | Runs everything and prints the comparison |
