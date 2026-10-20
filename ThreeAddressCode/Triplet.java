package ThreeAddressCode;

public class Triplet implements Cloneable{
    private String operator;
    private String operand1;
    private String operand2;

    public Triplet(String operand1, String operand2, String operator) {
        this.operator = operator;
        this.operand1 = operand1;
        this.operand2 = operand2;
    }

    public void setOperand1(String operand1) {
        this.operand1 = operand1;
    }

    public void setOperand2(String operand2) {
        this.operand2 = operand2;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public String getOperand1() {
        return operand1;
    }

    public String getOperator() {
        return operator;
    }

    public String getOperand2() {
        return operand2;
    }

    public boolean isFull() {
        return operand1 != null && operand2 != null;
    }


    @Override
    public String toString() {

        return (operand1 != null ? operand1 + " " : "")
                + (operand2 != null ? operand2 + " " : "")
                + operator;

        //return operand1 + " | " + operand2 + " | " + operator;
    }

    @Override
    public Triplet clone() {
        try {
            return (Triplet) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

}
