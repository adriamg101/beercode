package Optimitzador.CFG.CFGNodes;

import Optimitzador.OptimizerTriplet;
import ThreeAddressCode.Triplet;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FlowGraphNode {
    protected ArrayList<OptimizerTriplet> instructions;

    private static final String REFERENCE_LINE_REGEX = "\\((\\d+)\\)";
    private static final String LABEL_LINE_REGEX = "L\\d+";

    public FlowGraphNode() {
        instructions = new ArrayList<>();
    }

    public void addInstruction(Triplet instruction, int line) {
        instructions.add(new OptimizerTriplet(instruction, line));
    }

    public boolean isEmpty() {
        return instructions.isEmpty();
    }

    public int instructionSize() {
        return instructions.size();
    }

    public OptimizerTriplet getInstruction(int pos) {
        return instructions.get(pos);
    }

    public void printInstructions() {
        for (OptimizerTriplet instruction : instructions) {
            instruction.printInstruction();
        }
    }

    // Funcio que reseteja les linies i les seves referencies per a que comencin desde 0
    public void resetLines() {
        // Mirem la quantitat de linies que hem de restar en cas de referencies
        /*int substract_lines = instructions.getFirst().getLine();
        Pattern pattern = Pattern.compile(REFERENCE_LINE_REGEX);
        Pattern pattern2 = Pattern.compile(LABEL_LINE_REGEX);

        for (int i = 0; i < instructions.size(); i++) {
            instructions.get(i).setLine(i);

            // Si en la funcio hi ha una linia que fa referencia a un calcul previ, actualitzem la referencia de la
            // linia
            String op1 = instructions.get(i).getInstruction().getOperand1();
            if (op1 != null && (pattern.matcher(op1).matches() || pattern2.matcher(op1).matches())) {
                Matcher matcher = pattern.matcher(op1);
                if (matcher.find()) {
                    int newLine = Integer.parseInt(matcher.group(1)) - substract_lines;
                    instructions.get(i).getInstruction().setOperand1("(" + newLine + ")");
                } else {
                    matcher = pattern2.matcher(op1);
                    if (matcher.find()) {
                        int newLine = Integer.parseInt(op1.substring(1)) - substract_lines;
                        instructions.get(i).getInstruction().setOperand1("L" + newLine);
                    }
                }
            }

            // Fem el mateix amb l'operand 2
            String op2 = instructions.get(i).getInstruction().getOperand2();
            if (op2 != null && (pattern.matcher(op2).matches() || pattern2.matcher(op2).matches())) {
                Matcher matcher = pattern.matcher(op2);
                if (matcher.find()) {
                    int newLine = Integer.parseInt(matcher.group(1)) - substract_lines;
                    instructions.get(i).getInstruction().setOperand2("(" + newLine + ")");
                } else {
                    matcher = pattern2.matcher(op2);
                    if (matcher.find()) {
                        int newLine = Integer.parseInt(op2.substring(1)) - substract_lines;
                        instructions.get(i).getInstruction().setOperand2("L" + newLine);
                    }
                }
            }
        }*/
    }

    public ArrayList<OptimizerTriplet> getInstructions() {
        return instructions;
    }
}
