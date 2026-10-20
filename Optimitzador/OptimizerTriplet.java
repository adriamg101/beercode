package Optimitzador;

import ThreeAddressCode.Triplet;

public class OptimizerTriplet implements Cloneable {
    private Triplet instruction;
    private int line;

    public OptimizerTriplet(String operand1, String operand2, String operator, int line) {
        instruction = new Triplet(operand1, operand2, operator);
        this.line = line;
    }

    public OptimizerTriplet(Triplet instruction, int line) {
        this.instruction = instruction;
        this.line = line;
    }

    public void printInstruction() {
        System.out.println("(" + line + ") " + instruction.toString());
    }

    public Triplet getInstruction() {
        return instruction;
    }

    public void repositionLine(int actualLine) {
        this.line += actualLine;
    }

    public int getLine() {
        return line;
    }

    public void setLine(int line) {
        this.line = line;
    }

    @Override
    public OptimizerTriplet clone() {
        try {
            return (OptimizerTriplet) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }
}
