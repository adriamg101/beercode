package Optimitzador;

import Optimitzador.CFG.ControlFlowGraph;
import ThreeAddressCode.TAC;
import ThreeAddressCode.Triplet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Optimitzador {
    private ControlFlowGraph controlFlowGraph;

    /*
    // Operadors del TAC
    private static final String START_FUNCTION = "start";
    private static final String END_FUNCTION = "END FUNC";
    private static final String END_IF = "END IF";
    private static final String END_ELSE = "END ELSE";
    private static final String ELSE_GOTO = "else-goto";
    private static final String IF_GOTO = "if-goto";
    private static final String LOOP_GOTO = "goto-loop";
    private static final String ASSIGNATION = "=";
    private static final String GOTO = "goto";
    private static final String RETURN = "return";
    private static final String RETURNED = "returned";
    private static final String ADD = "ADD";
    private static final String SUBTRACT = "SUBTRACT";
    private static final String MULTIPLY = "MULTIPLY";
    private static final String DIVIDE = "DIVIDE";
    private static final String DECLARE = "declare";
    private static final String OR = "OR";
    private static final String AND = "AND";
    private static final String NOT_EQUAL = "!=";
    private static final String EQUAL = "==";

    // Operador nomes utilitzat per l'optimitzador
    private static final String RESULT = "RESULT";
    private static final String TRUE = "TRUE";
    private static final String FALSE = "FALSE";

    private static final String CTE_ENTER_REGEX = "-?[0-9]+";
    private static final String CTE_DECIMAL_REGEX = "-?[0-9]+\\.[0-9]+";
    private static final String REFERENCE_LINE_REGEX = "\\((\\d+)\\)";
    private static final String ID_REGEX = "[a-zA-Z]+";
     */

    public Optimitzador() {
        controlFlowGraph = null;
    }

    public TAC optimizeTAC(TAC tac) {
        optimizeTACLow(tac);
        return tac;
    }

    // DEP T0=4 :(
    private TAC optimizeTACDeep(TAC tac) {
        // Utilitzem l'optimitzador petit primer per facilitar la feina al optimitzador potent
        optimizeTACLow(tac);

        // Eliminem codi mort i fem inlining de funcions amb el control flow graph
        new ControlFlowGraph(tac);

        // Per finalitzar calculem constants (o no)
        // calculateConstantVariables(tac);

        return tac;
    }

    private TAC optimizeTACLow(TAC tac) {
        List<Triplet> original = tac.getInstructions();
        List<Triplet> optimized = new ArrayList<>();

        HashMap<String, deadCodeFunction> functions = new HashMap<>();

        int currLine = -1;
        boolean skip = false;
        String lastFunction = "";
        boolean isInFunction = false;

        for (int i = 0; i < original.size(); i++) {
            if (skip) {
                skip = false;
                continue;
            }
            Triplet instr = original.get(i);

            // Only optimize binary ops
            if (!instr.isFull()) {
                String op1 = instr.getOperator();
                String op2 = instr.getOperand2();

                //DEADCODE (functions)
                if (op1 != null && op2 != null) {
                    if (op1.equals("goto")) {   //Function detection --> goto
                        if (!functions.containsKey(op2)) {
                            functions.put(op2, new deadCodeFunction(op2, true, new ArrayList<>()));
                        } else {
                            functions.get(op2).found = true;
                        }
                    }
                    if (op2.equals("start") && !op1.equals("main")) {   //Function detection --> start (No main)
                        isInFunction = true;
                        lastFunction = op1;
                        if (!functions.containsKey(op1)) {
                            functions.put(op1, new deadCodeFunction(op1, false, new ArrayList<>()));
                        }
                    }
                }
                if (isInFunction) {
                    functions.get(lastFunction).lines.add(currLine);
                }
                if (op1 != null && op1.equals("END FUNC")) isInFunction = false;

                optimized.add(instr.clone());
                currLine++;
                continue;
            }
            if (isInFunction) {
                functions.get(lastFunction).lines.add(currLine);
            }

            String op1 = instr.getOperand1();
            String op2 = instr.getOperand2();
            String op = instr.getOperator();

            //INSIGNIFICANT OPERATIONS (+0, *1...)
            if (op.equals("ADD") || op.equals("SUBTRACT")) {
                Triplet nextOp = (i + 1 < original.size()) ? original.get(i + 1) : null;
                if (op2.equals("0") && nextOp != null && nextOp.getOperand1().startsWith("(") && nextOp.getOperand2().equals(op1)) {
                    skip = true;
                    continue;
                } else if (op.equals("ADD") && op1.equals("0") && nextOp != null && nextOp.getOperand1().startsWith("(") && nextOp.getOperand2().equals(op2)) {
                    skip = true;
                    continue;
                }
            } else if (op.equals("MULTIPLY")) {
                Triplet nextOp = (i + 1 < original.size()) ? original.get(i + 1) : null;
                if (op1.equals("1") && nextOp != null && nextOp.getOperand1().startsWith("(") && nextOp.getOperand2().equals(op2)) {
                    skip = true;
                    continue;
                } else if (op2.equals("1") && nextOp != null && nextOp.getOperand1().startsWith("(") && nextOp.getOperand2().equals(op1)) {
                    skip = true;
                    continue;
                }
            } else if (op.equals("DIVIDE")) {
                Triplet nextOp = (i + 1 < original.size()) ? original.get(i + 1) : null;
                if (op2.equals("1") && nextOp != null && nextOp.getOperand1().startsWith("(") && nextOp.getOperand2().equals(op1)) {
                    skip = true;
                    continue;
                }
            }

            currLine++;
            if (instr.getOperand1().startsWith("(")) {
                instr.setOperand1("("+(currLine-1)+")");
            }
            optimized.add(instr.clone());
        }

        eliminateDeadFunctions(optimized, functions);

        tac.setInstructions(optimized);
        return tac;
    }

    // Descansa en paz optimizador, te recordaremos como un heroe, inspirando codigos de t1=4. DEP. :(
    /*
    private TAC calculateConstantVariables(TAC tac) {
        ArrayList<Triplet> optTac = new ArrayList<>();

        // Mirem en les funcions que hi hagi en el tac
        HashMap<String, Number> varValue = new HashMap<>();
        HashMap<Integer, Number> resLines = new HashMap<>();
        HashMap<Integer, Boolean> resBoolLines = new HashMap<>();
        HashMap<String, Integer> varLastAssign = new HashMap<>();
        HashMap<String, Boolean> varIsConstant = new HashMap<>();

        // Carreguem regex per facilitarnos la feina
        Pattern pattern_int = Pattern.compile(CTE_ENTER_REGEX);
        Pattern pattern_float = Pattern.compile(CTE_DECIMAL_REGEX);
        Pattern pattern_ref = Pattern.compile(REFERENCE_LINE_REGEX);
        Pattern pattern_id = Pattern.compile(ID_REGEX);

        boolean addTriplet;
        // Calculem tot
        for (int i = 0; i < tac.getInstructionsSize(); i++) {
            addTriplet = true;
            switch (tac.getInstructions(i).getOperator()) {
                case ASSIGNATION:
                    // Fem la assignació
                    if (pattern_int.matcher(tac.getInstructions(i).getOperand1()).matches()) {  // Int
                        varValue.put(tac.getInstructions(i).getOperand2(), Integer.parseInt(tac.getInstructions(i).getOperand1()));
                    } else if (pattern_float.matcher(tac.getInstructions(i).getOperand1()).matches()) { // Float
                        varValue.put(tac.getInstructions(i).getOperand2(), Float.parseFloat(tac.getInstructions(i).getOperand1()));
                    } else if (pattern_ref.matcher(tac.getInstructions(i).getOperand1()).matches()) {   // Referencia a resultat en linia
                        // Extraiem el nombre de linia a mirar
                        Matcher matcher = pattern_ref.matcher(tac.getInstructions(i).getOperand1());
                        if (matcher.find()) {
                            int line = Integer.parseInt(matcher.group(1));
                            if (resLines.containsKey(line)) {
                                // Si es RESULT significa que es pot calcular
                                varValue.put(tac.getInstructions(i).getOperand2(), resLines.get(line));
                            } else {
                                // Si no, la variable no es constant
                                varIsConstant.put(tac.getInstructions(i).getOperand2(), false);
                                continue;
                            }
                        }
                    } else {    // Variable
                        // Com es una variable, mirem si es una constant.
                        if (varIsConstant.get(tac.getInstructions(i).getOperand1())) {
                            // Si es igualem la variable com a constant
                            varValue.put(tac.getInstructions(i).getOperand2(), varValue.get(tac.getInstructions(i).getOperand1()));
                        } else {
                            // Si no es constant
                            varIsConstant.put(tac.getInstructions(i).getOperand2(), false);
                            continue;
                        }
                    }

                    // Si la variable arriba fins aqui es constant.
                    varLastAssign.put(tac.getInstructions(i).getOperand2(), i);
                    varIsConstant.put(tac.getInstructions(i).getOperand2(), true);
                    break;
                case ADD, SUBTRACT, MULTIPLY, DIVIDE, EQUAL, NOT_EQUAL:
                    // Mirem si alguna dels dos operands es una no constant
                    if ((varIsConstant.containsKey(tac.getInstructions(i).getOperand1()) &&
                            !varIsConstant.get(tac.getInstructions(i).getOperand1())) ||
                            (varIsConstant.containsKey(tac.getInstructions(i).getOperand2()) &&
                            !varIsConstant.get(tac.getInstructions(i).getOperand2()))) {
                        break;
                    }

                    // Guardem el primer operador
                    Number op1 = null;
                    if (pattern_int.matcher(tac.getInstructions(i).getOperand1()).matches()) {  // Int
                        op1 = Integer.parseInt(tac.getInstructions(i).getOperand1());
                    } else if (pattern_float.matcher(tac.getInstructions(i).getOperand1()).matches()) { // Float
                        op1 = Float.parseFloat(tac.getInstructions(i).getOperand1());
                    } else if (pattern_ref.matcher(tac.getInstructions(i).getOperand1()).matches()) {   // Referencia a resultat en linia
                        // Extraiem el nombre de linia a mirar
                        Matcher matcher = pattern_ref.matcher(tac.getInstructions(i).getOperand1());
                        if (matcher.find()) {
                            int line = Integer.parseInt(matcher.group(1));
                            if (resLines.containsKey(line)) {
                                // Si es RESULT significa que es pot calcular
                                op1 = resLines.get(line);
                            } else {
                                continue;
                            }
                        }
                    } else {    // Variable
                        // Com es una variable, mirem si es una constant.
                        if (varIsConstant.get(tac.getInstructions(i).getOperand1())) {
                            // Si es igualem la variable com a constant
                            op1 = varValue.get(tac.getInstructions(i).getOperand1());
                        } else {
                            // Si no es constant
                            continue;
                        }
                    }

                    // Guardem el segon operador
                    Number op2 = null;
                    if (pattern_int.matcher(tac.getInstructions(i).getOperand2()).matches()) {  // Int
                        op2 = Integer.parseInt(tac.getInstructions(i).getOperand2());
                    } else if (pattern_float.matcher(tac.getInstructions(i).getOperand2()).matches()) { // Float
                        op2 = Float.parseFloat(tac.getInstructions(i).getOperand2());
                    } else if (pattern_ref.matcher(tac.getInstructions(i).getOperand2()).matches()) {   // Referencia a resultat en linia
                        // Extraiem el nombre de linia a mirar
                        Matcher matcher = pattern_ref.matcher(tac.getInstructions(i).getOperand2());
                        if (matcher.find()) {
                            int line = Integer.parseInt(matcher.group(1));
                            if (resLines.containsKey(line)) {
                                // Si es RESULT significa que es pot calcular
                                op2 = resLines.get(line);
                            } else {
                                continue;
                            }
                        }
                    } else {    // Variable
                        // Com es una variable, mirem si es una constant.
                        if (varIsConstant.get(tac.getInstructions(i).getOperand2())) {
                            // Si es igualem la variable com a constant
                            op2 = varValue.get(tac.getInstructions(i).getOperand2());
                        } else {
                            // Si no es constant
                            continue;
                        }
                    }

                    // Realitzem la operació
                    switch (tac.getInstructions(i).getOperator()) {
                        case ADD:
                            if (op1 instanceof Integer) {
                                resLines.put(i, op1.intValue() + op2.intValue());
                            } else {
                                resLines.put(i, op1.floatValue() + op2.floatValue());
                            }
                            break;
                        case SUBTRACT:
                            if (op1 instanceof Integer) {
                                resLines.put(i, op1.intValue() - op2.intValue());
                            } else {
                                resLines.put(i, op1.floatValue() - op2.floatValue());
                            }
                            break;
                        case MULTIPLY:
                            if (op1 instanceof Integer) {
                                resLines.put(i, op1.intValue() * op2.intValue());
                            } else {
                                resLines.put(i, op1.floatValue() * op2.floatValue());
                            }
                            break;
                        case DIVIDE:
                            if (op1 instanceof Integer) {
                                resLines.put(i, op1.intValue() / op2.intValue());
                            } else {
                                resLines.put(i, op1.floatValue() / op2.floatValue());
                            }
                            break;
                        case EQUAL:
                            if (op1 instanceof Integer) {
                                resBoolLines.put(i, op1.intValue() == op2.intValue());
                            } else if (op1 instanceof Float) {
                                resBoolLines.put(i, op1.floatValue() == op2.floatValue());
                            }
                            break;
                        case NOT_EQUAL:
                            if (op1 instanceof Integer) {
                                resBoolLines.put(i, op1.intValue() != op2.intValue());
                            } else if (op1 instanceof Float) {
                                resBoolLines.put(i, op1.floatValue() != op2.floatValue());
                            }
                            break;
                    }
                    break;
                case OR, AND:
                    // Mirem si les dos operacions de les linies anteriors son presents en el llistat de resultats booleans.
                    if (!resBoolLines.containsKey(i - 1) || !resBoolLines.containsKey(i - 2)) {
                        // Significa que no es pot calcular
                        continue;
                    }

                    // Si si que estan lliures calculem la operació
                    if (tac.getInstructions(i).getOperator().equals(OR)) {
                        resBoolLines.put(i, resBoolLines.get(i - 1) || resBoolLines.get(i - 2));
                    } else {    // AND
                        resBoolLines.put(i, resBoolLines.get(i - 1) && resBoolLines.get(i - 2));
                    }
                    break;
                    /*
                case IF_GOTO:
                    Matcher matcher = pattern_ref.matcher(tac.getInstructions(i).getOperand2());
                    if (matcher.find()) {
                        int line = Integer.parseInt(matcher.group(1));

                        // Mirem si el resultat del if es constant
                        if (!resBoolLines.containsKey(line)) {
                            // No podem comprobar si es sempre igual
                            continue;
                        }

                        // cambiem la instruccio de if-goto a nomes goto
                        eliminateIf(tac, optTac, i, resBoolLines.get(line));
                        addTriplet = false;
                    }
                    break;
                case LOOP_GOTO:
                    eliminateLoop(tac, i);
                    break;
                case RETURNED:
                    varIsConstant.put(tac.getInstructions(i).getOperand2(), false);
                    varLastAssign.remove(tac.getInstructions(i).getOperand2());
                    break;
            }

            if (addTriplet) {
                optTac.add(tac.getInstructions(i));
            }
        }

        tac.setInstructions(optTac);
        return tac;
    }

    private ArrayList<Triplet> eliminateIf(TAC tac, ArrayList<Triplet> optTac, int line, boolean condition) {
        // Calculem l'offset
        int offset_false = Integer.parseInt(tac.getInstructions(line).getOperand2().substring(1));

        if (condition) {
            //

        } else {

        }

        return optTac;
    }

    private ArrayList<Triplet> eliminateLoop(TAC tac, ArrayList<Triplet> optTac, int line) {

        return optTac;
    }
    */

    private void eliminateDeadFunctions(List<Triplet> optimized, HashMap<String,deadCodeFunction> functions) {
        for (deadCodeFunction func : functions.values()) {
            if (!func.found) {  //eliminate from 'optimized' + reorder number references
                List<Triplet> DCE = new ArrayList<>(); //(post)dead code eliminated set of instructions :3
                int currLine = -1;

                for (int i = 0; i < optimized.size(); i++) {
                    Triplet instr = optimized.get(i);

                    if (func.lines.contains(currLine)) {
                        continue;
                    }

                    currLine++;
                    if (instr.getOperand1() != null && instr.getOperand1().startsWith("(")) {
                        instr.setOperand1("("+(currLine-1)+")");
                    }
                    DCE.add(instr.clone());
                }

                optimized.clear();
                optimized.addAll(DCE);
            }
        }
    }
}
