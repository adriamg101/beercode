package gramatica;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FirstAlgorithm {
    //Conjunt de Firsts per cada no terminal de la gramatica
    public String AXIOMA;
    public final HashMap<String, Set<String>> firsts = new HashMap<>();
    public final HashMap<String, Set<String>> follows = new HashMap<>();

    /**
     * Generates the firsts set from the grammar
     */
    public void generateFirstSet() {
        // Calcular Firsts per tots els no terminals
        for (String nonTerminal : Grammar.getGrammar().keySet()) {
            //System.out.println(nonTerminal);
            calculateFirst(nonTerminal);
        }
    }
    /**
     *
     *
     * @param nonTerminal No terminal per al qual es vol calcular el First
     */
    private void calculateFirst(String nonTerminal) {
        // Si ja s'ha calculat el first, retornar
        if (firsts.containsKey(nonTerminal)) return;

        // Crear un conjunt per al no terminal
        Set<String> firstSet = new HashSet<>();
        firsts.put(nonTerminal, firstSet);

        // Processar totes les produccions del no terminal
        List<List<String>> productions = Grammar.getGrammar().get(nonTerminal);
        if (productions == null) return;

        for (List<String> production : productions) {
            processProduction(production, firstSet);
        }
    }

    private void processProduction(List<String> production, Set<String> currentFirst) {
        boolean canDeriveEpsilon = true;

        // Recorrer tots els símbols de la producció
        for (String symbol : production) {
            // si la producció conté ε, afegir ε als firsts
            if (symbol.equals("ε")) {
                currentFirst.add("ε");
                break;
            }

            // Si és terminal, afegir-lo als firsts
            if (isTerminal(symbol)) {
                currentFirst.add(symbol);
                canDeriveEpsilon = false;
                break;
            }

            // Es no terminal i no s'ha calculat el first, calcular-lo
            calculateFirst(symbol);
            Set<String> symbolFirst = firsts.get(symbol);

            // Afegir els firsts del no terminal al currentFirst
            addNonEpsilon(symbolFirst, currentFirst);

            // verificar si el no terminal pot derivar epsilon
            if (!symbolFirst.contains("ε")) {
                canDeriveEpsilon = false;
                break;
            }
        }

        // si tots els símbols poden derivar epsilon, afegir ε al currentFirst
        if (canDeriveEpsilon) {
            currentFirst.add("ε");
        }
    }


    private void addNonEpsilon(Set<String> source, Set<String> target) {
        for (String s : source) {
            if (!s.equals("ε")) {
                target.add(s);
            }
        }
    }

    private boolean isTerminal(String symbol) {
        // Els terminals utilitzen <...>
        return !(symbol.startsWith("<") && symbol.endsWith(">"));
    }

    public void displayFirstSets() {
        System.out.println("\nConjunt de First:");
        firsts.forEach((nt, set) ->
                System.out.println("First(" + nt + ") = " + set)
        );
    }

    public void generateFollowSet() {
        // Afegim els no terminals a la taula de follows
        for (String nonTerminal : Grammar.getGrammar().keySet()) {
            follows.put(nonTerminal, new HashSet<>());
        }

        //Afegim el simbol de final de fitxer al conjunt de Follow del simbol inicial
        String startSymbol = Grammar.getStartSymbol();
        this.AXIOMA = startSymbol;
        follows.get(startSymbol).add("$");


        boolean changed;    // ens indica si hi han més canvis a fer
        do {
            changed = false;
            for (String nonTerminal : Grammar.getGrammar().keySet()) {
                List<List<String>> productions = Grammar.getGrammar().get(nonTerminal);
                for (List<String> production : productions) {
                    for (int i = 0; i < production.size(); i++) {
                        String symbol = production.get(i);
                        // Si el símbol és un no terminal, afegir els firsts del següent símbol
                        if (!isTerminal(symbol)) {
                            Set<String> followSet = follows.get(symbol);
                            int initialSize = followSet.size();
                            // si no és l'últim símbol de la producció
                            if (i < production.size() - 1) {
                                String nextSymbol = production.get(i + 1);
                                // si el següent símbol és terminal, afegir-lo al followSet
                                if (isTerminal(nextSymbol)) {
                                    followSet.add(nextSymbol);
                                } else {
                                    // si el següent símbol és no terminal, afegir els firsts del següent símbol
                                    Set<String> nextFirst = firsts.get(nextSymbol);
                                    addNonEpsilon(nextFirst, followSet);
                                    // si el següent símbol pot derivar epsilon, afegir els follows del no terminal
                                    if (nextFirst.contains("ε")) {
                                        followSet.addAll(follows.get(nonTerminal));
                                    }
                                }
                            } else {
                                // Si és l'últim símbol de la producció, afegir els follows del no terminal
                                followSet.addAll(follows.get(nonTerminal));
                            }

                            // si s'ha afegit algun element al followSet, canviar la variable changed
                            if (followSet.size() > initialSize) {
                                changed = true;
                            }
                        }
                    }
                }
            }
            // si no s'ha fet cap canvi, sortir del bucle
        } while (changed);
    }

    /**
     * Function that returns the firsts' of the given keys.
     * @return Set of the firsts of the given keys.
     */
    public Set<String> getConstantsFirst(String... keys) {
        Set<String> constants = new HashSet<>();

        for (String key : keys) {
            Set<String> values = firsts.get(key);
            if (values != null) {
                constants.addAll(values);
            }
        }

        return constants;
    }

    public String getAXIOMA() {
        return this.AXIOMA;
    }

    public void displayFollowSets() {
        System.out.println("\nConjunt de Follow:");
        follows.forEach((nt, set) ->
                System.out.println("Follow(" + nt + ") = " + set)
        );
    }

    /**
     * Function that returns the follows' HashMap.
     * @return HashMap with the follows.
     */
    public HashMap<String, Set<String>> getFollows() {
        return follows;
    }
}