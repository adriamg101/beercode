package ThreeAddressCode;

import ParsingTree.ParsingTree;
import Semmantic.DataType;
import Semmantic.Symbol;
import Semmantic.SymbolTable;
import dictionary.Dictionary;

import java.util.*;

public class TAC {
    private List<Triplet> instruction;
    private final SymbolTable symbolTable;
    private final Set<String> constants;
    private int while_count = 0;
    private final int WORD_SIZE;
    private final Stack<Integer> stackReturn; //while=1, func=2, main=3, if=4 else=5
    private final Stack<String> stackLogicConditions;
    private final Stack<String> loopLabelStack = new Stack<>();
    private final Stack<Integer> conditionsCount = new Stack<>();



    // NON-TERMINALS
    private static final String OPERATION = "<OPERATION>";
    private static final String ASSIGNATION = "<ASSIGNATION>";
    private static final String DECLARATION = "<DECLARATION>";
    private static final String CONDITIONAL = "<CONDITIONAL>";
    private static final String MAIN = "<MAIN>";
    private static final String FUNCTION_CALL = "<FUNCTION_CALL>";
    private static final String RETURN_CALL = "<RETURN>";

    // TERMINALS
    private static final String EQUAL = "=";
    private static final String COMP_EQUAL = "==";
    private static final String COMP_NOT_EQUAL = "!=";
    private static final String FUNCTION = "FUNC";
    private static final String RETURN = "CB";
    private static final String ELSE = "ELSE";
    private static final String WHILE = "WHILE";

    // TAC
    private static final String GOTO = "goto";
    private static final String GOTO_LOOP = "goto-loop";
    private static final String ELSE_GOTO = "else-goto";
    private static final String END_FUNC = "END FUNC";
    private static final String END_IF = "END IF";
    private static final String END_ELSE = "END ELSE";
    private static final String DECLARE = "declare";

    private static final String ID_REGEX = "[a-zA-Z]{2,}";

    public TAC(SymbolTable symbolTable, Set<String> constants, int word_size) {
        this.instruction = new ArrayList<>();
        this.symbolTable = symbolTable;
        this.constants = constants;
        this.WORD_SIZE = word_size;
        this.stackReturn = new Stack<>();
        this.stackLogicConditions = new Stack<>();

    }

    private String getTempVar() {
        return "(" + (this.instruction.size() - 1) + ")";
    }

    private int getIntTempVar() {
        return this.instruction.size() - 1;
    }

    public void generate(ParsingTree node) {
        if (node == null) {
            return;
        }

        switch (node.getToken()) {
            case ASSIGNATION:
                generateTACFromAssignation(node);
                break;
            case OPERATION:
                generateTACFromOperation(node);
                break;
            case CONDITIONAL:
                generateTACFromConditional(node);
                this.stackReturn.add(4);
                break;
            case FUNCTION_CALL:
                generateTACFromFunctionCall(node);
                break;
            case FUNCTION:
                this.symbolTable.enterScope();
                generateTACFromFunction(node);
                this.stackReturn.add(3);
                break;
            case MAIN:
                this.symbolTable.enterScope();
                this.instruction.add(new Triplet(null, FUNC_START, "main"));
                this.stackReturn.add(3);
                break;
            case RETURN_CALL:
                generateTACFromReturn(node);
                break;
            case RETURN:
                int type_return = this.stackReturn.pop();
                switch (type_return) {
                    case 1:
                        if (!this.loopLabelStack.isEmpty()) {
                            String labelEnd = this.loopLabelStack.pop(); // SALT END
                            Triplet whileEnd = new Triplet(null, labelEnd, GOTO_LOOP);
                            instruction.add(whileEnd);
                        }
                        break;
                    case 3:
                        this.instruction.add(new Triplet(null, null, END_FUNC));
                        this.symbolTable.exitScope();
                        break;
                    case 4:
                        this.instruction.add(new Triplet(null, null, END_IF));
                        break;
                    case 5:
                        this.instruction.add(new Triplet(null, null, END_ELSE));
                        break;
                    default:
                        break;
                }
                break;
            case ELSE:
                this.stackReturn.add(5);
                generateTACFromElse(node);
                break;
            case WHILE:
                generateTACFromWhile(node);
                this.stackReturn.add(1);
                break;
            default:
                break;
        }

        for (ParsingTree child : node.getChildren()) {
            generate(child);
        }
    }

    private static final String VAR = "VAR";

    private Symbol getSymbol(String str) {
        Symbol symbol = this.symbolTable.getSymbolByName(str);
        try {
            if (symbol == null) {
                symbol = new Symbol(str, VAR, DataType.inferTypeFromValue(str), -1, new ArrayList<>(), 0);
                symbol.setValue(str, 0);
            }
        } catch (Exception e) {
            System.out.println("Error in TAC generation: " + e.getMessage());
        }
        return symbol;
    }

    private String getArrayString(String name, int index) {
        if (index > -1) {
            return name + "+" + index;
        }
        return name;
    }

    private static final int VALUE_ARR = 1;

    private int generateTACAssignationForArray(ParsingTree node) {
        // OC <VALUE_ARR> CC
        if (node == null || node.getChildren().isEmpty()) {
            return -1;
        }

        if (node.getChild(1).getLeaf(null).getValue() == null) {
            return -1;
        }
        String variable = node.getChild(VALUE_ARR).getLeaf(null).getValue();
        Symbol variableSymbol = this.getSymbol(variable);
        if (variableSymbol == null) {
            return -1;
        }

        return (int) variableSymbol.getValue();

    }

    private int getArraySize(ParsingTree node) {
        // OC <VALUE_ARR> CC
        if (node == null) {
            return -1;
        }

        if (node.getChild(1).getLeaf(null).getValue() == null) {
            return -1;
        }
        String variable = node.getChild(VALUE_ARR).getLeaf(null).getValue();
        Symbol variableSymbol = this.getSymbol(variable);
        if (variableSymbol == null) {
            return -1;
        }

        if (!(variableSymbol.getDataType().equals(DataType.ARRAY))) {
            return -1;
        }

        return variableSymbol.getArraySize();
    }

    private void generateTACFromDesignation(ParsingTree node) {
        String operand2 = node.getChildren().get(3).getLeaf(null).getValue();

        int size = this.getArraySize(node.getChild(3).getChild(1));
        Triplet triplet = new Triplet(String.valueOf(size), operand2 + this.symbolTable.getScopeOfVar(operand2), DECLARE);
        this.instruction.add(triplet);
    }

    private static final int VALUE_ARGUMENT = 1;
    private static final int ARGUMENTS = 2;
    private static final String ARGS = "args";

    private void generateTACFromArguments(ParsingTree node, int nArg) {
        // COMA <VALUE> <FUNC_ARGS’>
        if (node == null || node.getChildren().isEmpty()) return;

        ParsingTree valueNode = node.getChild(VALUE_ARGUMENT);
        String arg = valueNode.getLeaf(null).getValue();
        if (arg == null) return;
        Symbol argSymbol = this.getSymbol(arg);
        if (argSymbol == null) return;
        int index = this.generateTACAssignationForArray(valueNode.getChild(0).getChild(1));
        Triplet triplet = new Triplet(getArrayString(argSymbol.getName() + this.symbolTable.getScopeOfVar(argSymbol.getName()), index), ARGS + nArg, EQUAL);
        this.instruction.add(triplet);
        this.generateTACFromArguments(node.getChild(ARGUMENTS), nArg + 1);

    }

    private static final int STARTING_ARG = 0;

    private void generateTACFromFunctionArguments(ParsingTree node) {
        // WITH <VALUE> <FUNC_ARGS’>
        ParsingTree valueNode = node.getChild(VALUE_ARGUMENT);
        String arg1 = valueNode.getLeaf(null).getValue();
        if (arg1 == null) return;

        Symbol arg1Symbol = this.getSymbol(arg1);
        if (arg1Symbol == null) return;
        int index = this.generateTACAssignationForArray(valueNode.getChild(0).getChild(1));
        Triplet triplet = new Triplet(getArrayString(arg1Symbol.getName(), index) + this.symbolTable.getScopeOfVar(arg1Symbol.getName()), ARGS + STARTING_ARG, EQUAL);
        this.instruction.add(triplet);

        this.generateTACFromArguments(node.getChild(ARGUMENTS), STARTING_ARG + 1);

    }

    private static final String RETURN_FUNCTION = "return";

    private void generateTACFromReturn(ParsingTree node) {
        // IN <VARIABLE>
        String returnValue = node.getChild(1).getLeaf(null).getValue();
        if (returnValue == null) return;
        if (returnValue.matches(ID_REGEX)) {
            returnValue += this.symbolTable.getScopeOfVar(returnValue);
        }
        Symbol returnValueSymbol = this.getSymbol(returnValue);
        if (returnValueSymbol == null) return;
        Triplet triplet = new Triplet(null, returnValue, RETURN_FUNCTION);
        this.instruction.add(triplet);
    }

    private void generateTACReturnedValue(ParsingTree node) {
        // IN <VARIABLE>
        String idName = node.getChild(1).getLeaf(null).getValue();
        this.instruction.add(new Triplet(null, idName + this.symbolTable.getScopeOfVar(idName), "returned"));
    }

    private static final int FUNCTION_ARGUMENTS = 3;
    private static final int FUNCTION_NAME = 1;
    private static final int RETURN_FUNC = 2;

    private void generateTACFromFunctionCall(ParsingTree node) {
        // FUNC_CALL ID RETURN_FUNC FUNC_ARGS
        // Add the arguments to the function call
        this.generateTACFromFunctionArguments(node.getChild(FUNCTION_ARGUMENTS));

        // Add the function call to the instruction list
        String functionName = node.getChild(FUNCTION_NAME).getNode().getValue(); // ID
        Triplet triplet = new Triplet(null, functionName, GOTO);
        this.instruction.add(triplet);

        // Add the return value to the instruction list
        this.generateTACReturnedValue(node.getChild(RETURN_FUNC));
    }


    private static final int VARIABLE = 2;
    private static final int VALUE = 3;

    private void generateTACFromAssignation(ParsingTree node) {
        try {
            // Children: REFILL, OF, <VARIABLE>, <VALUE>
            String operand1 = node.getChildren().get(VALUE).getLeaf(null).getValue();
            if (operand1 != null && operand1.matches(ID_REGEX)) {
                operand1 += this.symbolTable.getScopeOfVar(operand1);
            }

            String operand2 = node.getChildren().get(VARIABLE).getLeaf(null).getValue();
            Symbol operand2Symbol = this.getSymbol(operand2);

            int index = this.generateTACAssignationForArray(node.getChild(VARIABLE).getChild(1));

            //operand2Symbol.setValue(operand1, index);
            Triplet triplet = new Triplet(operand1, getArrayString(operand2Symbol.getName() + this.symbolTable.getScopeOfVar(operand2Symbol.getName()), index), EQUAL);
            instruction.add(triplet);
        } catch (Exception e) {
            System.out.println("Error in TAC generation: " + e.getMessage());
        }
    }

    private static final int OPERATIONAL = 0;
    private static final int OP_ARGS = 2;
    private static final int OP_VARIABLE = 4;

    private void generateTACFromOperation(ParsingTree node) {
        // Children: <OPERATIONAL> THIS <OP_ARGS> IN <VARIABLE>
        int index = -1;
        String operator = node.getChildren().get(OPERATIONAL).getLeaf(null).getKey();
        String operand2 = node.getChildren().get(OP_VARIABLE).getLeaf(null).getValue();
        Symbol operand2Symbol = this.symbolTable.getSymbolByName(operand2);

        if (operand2Symbol == null) return;
        index = this.generateTACAssignationForArray(node.getChild(OP_VARIABLE).getChild(VALUE_ARR));

        Triplet triplet = new Triplet(null, null, operator);
        this.generateRecursiveOperation(node.getChild(OP_ARGS), triplet, operand2Symbol);

        triplet.setOperand2(node.getChildren().get(OP_VARIABLE).getLeaf(null).getValue());

        Triplet triplet2 = new Triplet(this.getTempVar(), this.getArrayString(operand2Symbol.getName() + this.symbolTable.getScopeOfVar(operand2Symbol.getName()), index), EQUAL);
        this.instruction.add(triplet2);
    }

    private String cleanVarName(String varName) {
        if (varName == null) return null;
        return varName.replaceAll("[^0-9]", "");
    }

    private static final String STOP_TERMINAL = "IN";

    private void generateRecursiveOperation(ParsingTree node, Triplet triplet, Symbol actualResult) {
        // Children <VALUE> COMA <VALUE> <OP_ARGS’>
        //           COMA <VALUE> <OP_ARGS’>
        try {
            if (node == null) return;

            if (constants.contains(node.getToken())) {
                String operand = node.getLeaf(null).getValue();
                actualResult = this.getSymbol(operand);
                if (triplet.getOperand1() == null) {
                    if (actualResult != null) {
                        int index = this.generateTACAssignationForArray(node.getChild(0).getChild(1));
                        String operand1Str = getArrayString(actualResult.getName(), index);
                        if (actualResult.getScopeId() != -1) {
                            operand1Str += this.symbolTable.getScopeOfVar(operand1Str);
                        }
                        triplet.setOperand1(operand1Str);
                    }
                } else {
                    String operand2Str = operand;
                    if (actualResult.getScopeId() != -1) {
                        operand2Str += this.symbolTable.getScopeOfVar(operand2Str);
                    }
                    triplet.setOperand2(operand2Str);
                }
            }

            if (triplet.isFull()) {
                this.instruction.add(triplet.clone());
                triplet.setOperand1(this.getTempVar());
                triplet.setOperand2(null);
            }

            if (node.getToken().equals(STOP_TERMINAL) && triplet.getOperand1() != null) return;
            node = node.getNextNode(node);
            generateRecursiveOperation(node, triplet, actualResult);

        } catch (Exception e) {
            System.out.println("Error in TAC generation: " + e.getMessage());
        }
    }

    private void generateRecursiveTACFromArguments(ParsingTree node, int nArgs) {
        // COMA <TYPE> <VARIABLE> <PARAMETERS’>
        if (node == null || node.getChildren().isEmpty()) return;

        String variable = node.getChild(2).getLeaf(null).getValue();
        if (variable == null) return;
        this.instruction.add(new Triplet("args" + nArgs, variable + this.symbolTable.getScopeOfVar(variable), EQUAL));
        generateRecursiveTACFromArguments(node.getChild(3), nArgs + 1);
    }

    private void generateTACFromArguments(ParsingTree node) {
        // COMA <VALUE> <FUNC_ARGS’>
        int nArgs = 0;
        String variable = node.getChildren().get(1).getLeaf(null).getValue();
        if (variable == null) return;
        this.instruction.add(new Triplet("args" + nArgs, variable + this.symbolTable.getScopeOfVar(variable), EQUAL));
        nArgs++;

        generateRecursiveTACFromArguments(node.getChild(ARGUMENTS), nArgs);
    }

    private static final String FUNC_START = "start";

    private void generateTACFromFunction(ParsingTree node) {
        try {
            ParsingTree parentNode = node.getParent();
            // ARGS -> 5
            String functionName = parentNode.getChild(1).getNode().getValue(); // ID
            this.instruction.add(new Triplet(null, FUNC_START, functionName));
            generateTACFromArguments(parentNode.getChild(5));

        } catch (Exception e) {
            System.out.println("Error in TAC generation for function: " + e.getMessage());
        }
    }


    private static final String IF_GOTO = "if-goto";

    private void generateTACFromWhile(ParsingTree node) {
        try {
            this.conditionsCount.add(0); // new condition
            Triplet previousLabelTriplet = new Triplet(null, null, "Label");
            this.instruction.add(previousLabelTriplet);
            this.loopLabelStack.add("L" + (this.instruction.size() - 1));


            evaluateCondition(node.getParent().getChild(1)); // "<CONDITION>"
            int count = this.conditionsCount.pop();
            count++;
            this.conditionsCount.push(count);
            generateTACFromIfConditions(node.getParent().getChild(1));
            int whileBlockSize = calculateBlockSize(node.getParent().getChild(3));
            String labelStart = "LL" + (whileBlockSize + instruction.size() + 2); // SALT STARt
            previousLabelTriplet.setOperand2(labelStart);
            Triplet whileStart = new Triplet(getTempVar(), labelStart, IF_GOTO);

            instruction.add(whileStart);
            this.while_count = instruction.size();
            this.conditionsCount.pop();
        } catch (Exception e) {
            System.out.println("Error in TAC generation: " + e.getMessage());
        }
    }

    private void generateTACFromIfConditions(ParsingTree node) {
        String conditionOperand = node.getChild(4).getLeaf(null).getKey();
        if (!this.conditionsCount.isEmpty() && this.conditionsCount.peek() > 1) {
            this.instruction.add(new Triplet(null, null, this.stackLogicConditions.pop()));
            int count = this.conditionsCount.pop();
            count--;
            this.conditionsCount.push(count);
        }

        if (conditionOperand != null && !"ε".equals(conditionOperand)) {
            this.stackLogicConditions.add(conditionOperand);
            evaluateCondition(node.getChild(4).getChild(1)); // "<CONDITION>"
            int count = this.conditionsCount.pop();
            count++;
            this.conditionsCount.push(count);

            generateTACFromIfConditions(node.getChild(4).getChild(1));
        }
    }

    private void generateTACFromConditional(ParsingTree node) {
        try {
            this.conditionsCount.add(0); // new condition
            Triplet previousLabelTriplet = new Triplet(null, null, "Label");
            this.instruction.add(previousLabelTriplet);
            evaluateCondition(node.getChild(1)); // "<CONDITION>"
            int count = this.conditionsCount.pop();
            count++;
            this.conditionsCount.push(count);

            boolean hasElse = node.getChildren().size() > 5 && node.getChild(5).getChild(0).getToken().equals(ELSE);
            int elseOffset = hasElse ? 1 : 0;


            generateTACFromIfConditions(node.getChild(1));

            int ifBlockSize = calculateBlockSize(node.getChild(3));
            String labelFalse = "L" + (ifBlockSize + instruction.size() + 1 + elseOffset); // SALT FALSE
            previousLabelTriplet.setOperand2(labelFalse);

            String tempVar = getTempVar();
            if (!this.conditionsCount.isEmpty() && this.conditionsCount.peek() > 1) {
                tempVar = "(" + (getIntTempVar() - 1) + ")";
            }
            Triplet conditionalJump = new Triplet(tempVar, labelFalse, IF_GOTO);
            instruction.add(conditionalJump);
            this.conditionsCount.pop();
        } catch (Exception e) {
            System.out.println("Error in TAC generation: " + e.getMessage());
        }

    }

    private void generateTACFromElse(ParsingTree node) {
        try {
            int elseBlockSize = calculateBlockSize(node.getParent().getChild(2));
            String labelEnd = "L" + (elseBlockSize + instruction.size()); // SALT END
            Triplet elseJump = new Triplet(null, labelEnd, ELSE_GOTO);
            instruction.add(elseJump);

        } catch (Exception e) {
            System.out.println("Error in TAC generation: " + e.getMessage());
        }
    }

    private static final int LEFT_OPERAND = 0;
    private static final int RIGHT_OPERAND = 3;

    private void evaluateCondition(ParsingTree node) {
        try {
            String operator = node.getChild(2).getLeaf(null).getKey();
            if (operator == null) return;

            if (operator.equals("NOT")) {
                operator += "_" + node.getChild(2).getChild(1).getLeaf(null).getKey();
            }

            String leftOperand = node.getChild(LEFT_OPERAND).getLeaf(null).getValue();
            Symbol leftOperandSymbol = this.getSymbol(leftOperand);
            if (leftOperandSymbol != null && leftOperandSymbol.getScopeId() != -1) {
                leftOperand += this.symbolTable.getScopeOfVar(leftOperand);

            }

            String rightOperand = node.getChild(RIGHT_OPERAND).getLeaf(null).getValue();
            Symbol rightOperandSymbol = this.getSymbol(rightOperand);
            if (rightOperandSymbol != null && rightOperandSymbol.getScopeId() != -1) {
                rightOperand += this.symbolTable.getScopeOfVar(rightOperand);
            }

            Triplet conditionTriplet = new Triplet(leftOperand, rightOperand, convertOperator(operator));
            instruction.add(conditionTriplet);
        } catch (Exception e) {
            System.out.println("Error in TAC generation: " + e.getMessage());
        }

    }

    private int calculateBlockSize(ParsingTree blockNode) {
        List<Triplet> backupInstructions = new ArrayList<>(instruction);
        int initialSize = instruction.size();

        generate(blockNode);
        int blockSize = instruction.size() - initialSize;

        instruction.clear();
        instruction.addAll(backupInstructions);

        return blockSize;
    }

    private static final String C_EQUAL = "C_EQUAL";
    private static final String NOT_C_EQUAL = "NOT_C_EQUAL";
    private static final String G_THAN = "GT";
    private static final String L_THAN = "LT";
    private static final String NOT = "NOT";

    private String convertOperator(String operator) {
        String realOperator = null;
        switch (operator) {
            case C_EQUAL:
                realOperator = COMP_EQUAL;
                break;
            case NOT_C_EQUAL:
                realOperator = COMP_NOT_EQUAL;
                break;
            case G_THAN:
            case NOT + L_THAN:
                realOperator = ">";
                break;
            case L_THAN:
            case NOT + G_THAN:
                realOperator = "<";
            default:
                break;
        }
        return realOperator;
    }

    public void showTAC() {
        for (int i = 0; i < instruction.size(); i++) {
            System.out.println("(" + i + ") " + instruction.get(i));
        }
    }

    public List<Triplet> getInstructions() {
        return this.instruction;
    }

    public void setInstructions(List<Triplet> ins) {
        this.instruction = ins;
    }

    public Triplet getInstructions(int pos) {
        return instruction.get(pos);
    }

    public int getInstructionsSize() {
        return instruction.size();
    }
}


