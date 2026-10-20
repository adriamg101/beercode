package Semmantic;

final class OperationEvaluator {
    private OperationEvaluator() {}

    private static final String ADD = "ADD";
    private static final String SUBTRACT = "SUBTRACT";
    private static final String MULTIPLY = "MULTIPLY";
    private static final String DIVIDE = "DIVIDE";
    private static final String AND = "AND";
    private static final String OR = "OR";

    public static Object evaluate(Object operand1, Object operand2, String operator) {
        if (operand1 instanceof Number && operand2 instanceof Number) {
            return evaluateNumeric(
                    ((Number) operand1).doubleValue(),
                    ((Number) operand2).doubleValue(),
                    operator);
        }
        if (operand1 instanceof String || operand2 instanceof String) {
            if (!ADD.equals(operator)) throw new IllegalArgumentException("Strings only support ADD");
            return operand1.toString() + operand2.toString();
        }
        if (operand1 instanceof Character && operand2 instanceof Character) {
            return evaluateChars((char) operand1, (char) operand2, operator);
        }
        if (operand1 instanceof Boolean && operand2 instanceof Boolean) {
            return evaluateBooleans((boolean) operand1, (boolean) operand2, operator);
        }
        throw new IllegalArgumentException("Unsupported types for operation");
    }

    private static double evaluateNumeric(double operand1, double operand2, String operator) {
        return switch (operator) {
            case ADD -> operand1 + operand2;
            case SUBTRACT -> operand1 - operand2;
            case MULTIPLY -> operand1 * operand2;
            case DIVIDE -> {
                if (operand2 == 0) throw new ArithmeticException("Division by zero");
                yield operand1 / operand2;
            }
            default -> throw new IllegalArgumentException("Unsupported operator: " + operator);
        };
    }

    private static int evaluateChars(char operand1, char operand2, String operator) {
        return switch (operator) {
            case ADD -> operand1 + operand2;
            case SUBTRACT -> operand1 - operand2;
            default -> throw new IllegalArgumentException("Unsupported operator for chars: " + operator);
        };
    }

    private static boolean evaluateBooleans(boolean operand1, boolean operand2, String operator) {
        return switch (operator) {
            case AND -> operand1 && operand2;
            case OR -> operand1 || operand2;
            default -> throw new IllegalArgumentException("Unsupported operator for booleans: " + operator);
        };
    }
}
