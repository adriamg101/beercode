# BeerCode 🍺

## Getting Started 🍻

Tired of seeing your students yawn at old, boring, plain C? Teach them **BeerCode**! 🍺

**BeerCode** is a fun and engaging coding language designed to capture the attention of university students with the one thing they enjoy the most: **beer**! 🍻

---

## About the project

BeerCode is a small programming language and the compiler our group wrote for it in Java. The compiler reads a `.beer` file and produces MIPS assembly, which you can run in the MARS simulator that ships in the repo (`Mips/Mars4_5.jar`). The language is a toy, but everything between the source file and the assembly is built by hand: the lexer, the grammar and its parsing table, the semantic checks, the intermediate code, the optimizer and the MIPS backend.

It depends on `org.json`, included in `lib/`, and on Gson, which is not in the repo. Download `gson-2.8.5.jar` (any recent version should work) and put it in `lib/`.

---

## How it works

The compiler runs in a few stages, all driven from `Main.java`:

1. **Tables**: the token dictionary (`dictionary/dictionary.json`) and the grammar (`gramatica/gramatica.json`) are loaded, FIRST and FOLLOW sets are computed, and the parsing table is generated. It is cached in `ParserTable/parserTable.json` and rebuilt with `-re-pt`.
2. **Preprocessing**: comments and empty lines are stripped from the source.
3. **Scanner, parser and semantic analysis**: they run together. The table-driven parser asks the scanner for tokens, builds the parsing tree and fills the symbol table while checking types, arguments and undefined symbols. Errors go through a shared handler, and compilation stops if there are any.
4. **Three Address Code**: the parsing tree is translated into TAC.
5. **Optimization** (optional, with `-o`): dead code elimination and removal of pointless arithmetic, using a control flow graph.
6. **MIPS generation**: a register manager (`Mips/MMU.java`) maps TAC to MIPS and writes `output/output.asm`.

Folder names follow the stages: `Lexer`, `Parser`, `ParserTable`, `ParsingTree`, `Semmantic`, `ThreeAddressCode`, `Optimitzador` and `Mips`. Custom exceptions live in `Exceptions`.

---

## The language at a glance

Programs read like an order at a bar. Blocks open with 🤢 and close with 🤮, comments start with 🍺, and statements go to the `Camarero` (waiter) and end with `gracias`.

| BeerCode | Meaning |
|---|---|
| `canya`, `litrona`, `clara`, `mediana`, `quinto`, `chupito` | int, long, float, double, char, bool |
| `cubo`, `combo` | array, struct |
| `Camarero una canya de Turia gracias` | declare an int called `Turia` |
| `refill` | assign a value |
| `mezclame`, `rebajame`, `barrejame`, `separame` | add, subtract, multiply, divide |
| `si` / `sinó` | if / else |
| `apunta mentre` | while |
| `pedido` | function definition |
| `sirveme` | function call |
| `ya sabes` | return |
| `entra bar` | main |

Variable names have to be beer brands (`EstrellaDamm`, `Turia`, `Moritz`...) and function names have to be bar names (`OvellaNegra`, `BarPaco`...). The allowed names are in `Semmantic/MarcasBirras.json` and `Semmantic/NombreBares.json`.

Here is a recursive Fibonacci, from `test/working/code/TESTER_FIBONACCI_RECURSIU.beer`:

```
pedido OvellaNegra de canya (canya Amstel) 🤢
    si Amstel es menor 1 🤢
        ya sabes 0 gracias
    🤮

    si Amstel es igual 1 🤢
        ya sabes 1 gracias
    🤮

    Camarero una canya de Turia gracias
    Camarero una canya de EstrellaDamm gracias

    Camarero refill de EstrellaDamm Amstel gracias

    Camarero rebajame esto EstrellaDamm, 1 en Turia que?
    Camarero sirveme OvellaNegra en Turia con Turia gracias

    Camarero una canya de Moritz gracias

    Camarero rebajame esto EstrellaDamm, 2 en Moritz que?
    Camarero sirveme OvellaNegra en Moritz con Moritz gracias

    Camarero una canya de Ambar gracias

    Camarero mezclame esto Turia, Moritz en Ambar lo siento

    ya sabes Ambar gracias
🤮

entra bar aigua () 🤢
    Camarero una canya de EstrellaDamm gracias
    Camarero una canya de Amstel gracias

    Camarero refill de EstrellaDamm 0 gracias
    Camarero refill de Amstel 7 gracias

    Camarero sirveme OvellaNegra en EstrellaDamm con Amstel gracias
🤮
```

The full token list is in `dictionary/dictionary.json`. Sample programs are in `test/working` (arithmetic, arrays, conditionals, functions, factorial and Fibonacci). `test/NotWorking` holds tests for features that are not finished.

---

## How to Run BeerCode 🎉

To get started with BeerCode, simply follow these steps:

1. **Prepare the Environment**:
   - Make sure the files `MarcasBirras.json` and `NombreBares.json` are present in the `Semmantic` folder.

2. **Use Provided Test Files**:
   - Use any of the given test files (e.g., `TESTER_X`) or create your own.
   - The program requires a file to be passed as the first command-line argument in order to execute. The path from project source to file needs to be provided (for example: `test/working/code/TESTER_FIBONACCI_RECURSIU.beer`)

   ⚠️ **Important**: Remember to follow the language rules from the report if you're creating your own test files!

3. **Compile and Run**:
   - Build from the project root with a JDK: `javac -cp lib/org.json-20161124.jar:lib/gson-2.8.5.jar -d out $(find . -name '*.java')`
   - Run the compiler from the project root: `java -cp out:lib/org.json-20161124.jar:lib/gson-2.8.5.jar Main test/working/code/TESTER_FIBONACCI_RECURSIU.beer -o`
   - The compiler writes MIPS assembly to `output/output.asm`. Open it in MARS with `java -jar Mips/Mars4_5.jar output/output.asm`.

---

## Flags & Options 🏁

To easily toggle options like visualizations, BeerCode comes with a set of flags you can include in the "running configurations" (or similar). These flags control different features and outputs:

### Visualization Flags:
- `--verbalize` | `-v` (both accepted): Verbalize output in detail, in general, of the program.
- `-v-ps`: Verbalize parsing-scanner process, including the retrieval of the next token in the stream and prediction of future tokens.
- `-v-st`: Verbalize symbol-table structure, highlighting the different scopes and identifiers.
- `-v-t`: Verbalize parsing-tree structure, highlighting the productions and different types of tokens.
- `-v-pt`: Verbalize parsing-table process of construction.
- `-v-tac`: Verbalize Three Address Code (TAC) generation.
- `-v-o`: Verbalize Three Address Code (TAC) before and after optimizations.

### Update Flags:
- `-re-pt`: Re-run the creation of the parsing table in case the grammar has changed, for example.
- `--redo-parse-table`: Same as `-re-pt`.

### Optimization Flags:
- `-o`: Apply optimizations (dead code eliminations and pointless arithmetic eliminated).
- `-o-t`: Optimize parsing tree.
