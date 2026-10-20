package Optimitzador.CFG;

import Optimitzador.CFG.CFGNodes.FlowGraphNode;
import Optimitzador.CFG.CFGNodes.FlowGraphNodeFunc;
import Optimitzador.OptimizerTriplet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FlowGraph {
    private HashMap<String, FlowGraphNodeFunc> funcNodes;

    private static final String REFERENCE_LINE_REGEX = "\\((\\d+)\\)";
    private static final String LABEL_LINE_REGEX = "L\\d+";

    // Operadors del TAC
    private static final String START_FUNCTION = "start";
    private static final String END_FUNCTION = "END FUNC";
    private static final String EQUAL = "=";
    private static final String GOTO = "goto";
    private static final String RETURN = "return";
    private static final String RETURNED = "returned";
    private static final String MAIN = "main";

    private static final int CALL_LINE = 0;
    private static final int OFFSET = 1;

    public FlowGraph() {
        funcNodes = new HashMap<>();
    }

    public void newNode(String name, FlowGraphNodeFunc node) {
        funcNodes.put(name, node);
    }

    public void printGraph() {
        System.out.println("\nNodes created: ");
        for (String names : funcNodes.keySet()) {
            System.out.println("--------------------------");
            funcNodes.get(names).printInstructions();
        }
        System.out.println("--------------------------");
    }

    public FlowGraphNodeFunc getMain() {
        return funcNodes.get(MAIN);
    }

    public FlowGraphNodeFunc getNode(String name) {
        return funcNodes.get(name);
    }

    public void eliminateUnusedFuncs(HashSet<String> usedFuncs) {
        Iterator<String> it = funcNodes.keySet().iterator();
        while (it.hasNext()) {
            String name = it.next();
            if (!usedFuncs.contains(name)) {
                it.remove();
            }
        }
    }

    public ArrayList<OptimizerTriplet> inliningLines() {
        // Primer
        ArrayList<OptimizerTriplet> optCode = inliningLines(0, funcNodes.get(MAIN).getInstructions());

        // Afegim limits de main
        //optCode.addFirst(new OptimizerTriplet(null, MAIN, START_FUNCTION, 0));
        //optCode.addLast(new OptimizerTriplet(null, null, END_FUNCTION, optCode.size()));

        // Afegim codi de funcions recursives
        for (String name : funcNodes.keySet()) {
            if (funcNodes.get(name).isRecursive()) {
                optCode.addAll(funcNodes.get(name).getInstructions());
            }
        }

        return optCode;
    }

    private ArrayList<OptimizerTriplet> inliningLines(int offset_line, ArrayList<OptimizerTriplet> instructions) {
        ArrayList<OptimizerTriplet> res = new ArrayList<>();
        /*ArrayList<ArrayList<Integer>> offsets_funcs = new ArrayList<>();
        Pattern pattern = Pattern.compile(REFERENCE_LINE_REGEX);
        Pattern pattern2 = Pattern.compile(LABEL_LINE_REGEX);
        int returnsNeeded = 0;

        // Actualitzem les lines de la funcio
        for (int i = 1; i < instructions.size() - 1; i++) {
            res.add((instructions.get(i).clone()));

            if (res.getLast().getInstruction().getOperator().equals(GOTO)) {
                // Mirem si la funcio es recursiva
                if (!funcNodes.get(res.getLast().getInstruction().getOperand2()).isRecursive()) {
                    // Generem el codi de la funcio
                    ArrayList<OptimizerTriplet> inlining = inliningLines(offset_line + res.size(), funcNodes.get(res.getLast().getInstruction().getOperand2()).getInstructions());
                    res.removeLast();   // Treiem instruccio de goto
                    offsets_funcs.add(new ArrayList<>());

                    // Setejem la marca del offset
                    offsets_funcs.getLast().add(res.size()); // CALL_LINE
                    offsets_funcs.getLast().add(inlining.size());  // OFFSET

                    // Afegim el codi de la nova funcio
                    res.addAll(inlining);
                }
                returnsNeeded++;
            } else if (res.getLast().getInstruction().getOperator().equals(RETURN)) {    // Si es una linia de return
                // La idea es guardar el contingut de la variable a retornar a un registre temporal de return.
                // Es a dir, canviar a una operacio de assignació i afegir un goto al final.

                // Afegim la assignació
                res.getLast().getInstruction().setOperator(EQUAL);
                res.getLast().getInstruction().setOperand1(res.getLast().getInstruction().getOperand2());
                res.getLast().getInstruction().setOperand2("return_temp");

                // Afegim el goto al final de la funció
                res.add(new OptimizerTriplet(null, ("L" + (instructions.size() - 1 + offset_line)), GOTO, res.size()));
            } else if (res.getLast().getInstruction().getOperator().equals(RETURNED) && returnsNeeded > 0) {    // Si es una linia de returned d'una funcio no recursiva
                // La idea es guardar el contingut del registre temporal de return a la variable assignada.
                // Es a dir, canviar a una operacio de assignació. Revisant que no sigues el return d'una funcio recursiva.
                res.getLast().getInstruction().setOperator(EQUAL);
                res.getLast().getInstruction().setOperand1("return_temp");
                // Operand2 el deixem com esta
                returnsNeeded--;
            } else {
                // Qualsevol linia
                res.getLast().repositionLine(offset_line);

                // Si en la funcio hi ha una linia que fa referencia a un calcul previ, actualitzem la referencia de la
                // linia
                String op1 = res.getLast().getInstruction().getOperand1();
                if (op1 != null && (pattern.matcher(op1).matches() || pattern2.matcher(op1).matches())) {
                    Matcher matcher = pattern.matcher(op1);
                    if (matcher.find()) {
                        int newLine = Integer.parseInt(matcher.group(1)) + offset_line;
                        // Hem de mirar el offset per les funcions
                        int j = 0;
                        while (j < offsets_funcs.size() && newLine < offsets_funcs.get(j).get(CALL_LINE)) {
                            newLine += offsets_funcs.get(j).get(OFFSET);
                            j++;
                        }
                        res.getLast().getInstruction().setOperand1("(" + newLine + ")");
                    } else {
                        matcher = pattern2.matcher(op1);
                        if (matcher.find()) {
                            int newLine = Integer.parseInt(op1.substring(1)) + offset_line;
                            // Hem de mirar el offset per les funcions
                            int j = 0;
                            while (j < offsets_funcs.size() && newLine < offsets_funcs.get(j).get(CALL_LINE)) {
                                newLine += offsets_funcs.get(j).get(OFFSET);
                                j++;
                            }
                            res.getLast().getInstruction().setOperand1("L" + newLine);
                        }
                    }
                }

                // Fem el mateix amb l'operand 2
                String op2 = res.getLast().getInstruction().getOperand2();
                if (op2 != null && (pattern.matcher(op2).matches() || pattern2.matcher(op2).matches())) {
                    Matcher matcher = pattern.matcher(op2);
                    if (matcher.find()) {
                        int newLine = Integer.parseInt(matcher.group(1)) + offset_line;
                        // Hem de mirar el offset per les funcions
                        int j = 0;
                        while (j < offsets_funcs.size() && newLine < offsets_funcs.get(j).get(CALL_LINE)) {
                            newLine += offsets_funcs.get(j).get(OFFSET);
                            j++;
                        }
                        res.getLast().getInstruction().setOperand2("(" + newLine + ")");
                    } else {
                        matcher = pattern2.matcher(op2);
                        if (matcher.find()) {
                            int newLine = Integer.parseInt(op2.substring(1)) + offset_line;
                            // Hem de mirar el offset per les funcions
                            int j = 0;
                            while (j < offsets_funcs.size() && newLine < offsets_funcs.get(j).get(CALL_LINE)) {
                                newLine += offsets_funcs.get(j).get(OFFSET);
                                j++;
                            }
                            res.getLast().getInstruction().setOperand2("L" + newLine);
                        }
                    }
                }
            }
        }*/

        return res;
    }
}
