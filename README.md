# BeerCode 🍺

## Getting Started 🍻

Tired of seeing your students yawn at old, boring, plain C? Teach them **BeerCode**! 🍺

**BeerCode** is a fun and engaging coding language designed to capture the attention of university students with the one thing they enjoy the most: **beer**! 🍻

---

## How to Run BeerCode 🎉

To get started with BeerCode, simply follow these steps:

1. **Prepare the Environment**:
   - Make sure the files `MarcasBirras.json` and `NomesBares.json` are present in the `Semmantic` folder.

2. **Use Provided Test Files**:
   - Use any of the given test files (e.g., `TESTER_X`) or create your own.
   - The program requires a file to be passed as the first command-line argument in order to execute. The path from project source to file needs to be provided (for example: `test/working/code/TESTER_FIBONACCI_RECURSIU.beer`)

   ⚠️ **Important**: Remember to follow the language rules from the report if you're creating your own test files!

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
