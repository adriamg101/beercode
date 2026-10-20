package Semmantic;

import java.util.Objects;

public final class TypeConverter {
    private TypeConverter() {} // Clase de utilidad, no instanciable

    public static Object convert(Object value, DataType targetType) throws Exception {
        Objects.requireNonNull(targetType, "Target type cannot be null");

        if (value == null) {
            return null;
        }

        try {
            return switch (targetType) {
                case INT -> convertToInteger(value);
                case DOUBLE -> convertToDouble(value);
                case FLOAT -> convertToFloat(value);
                case LONG -> convertToLong(value);
                case BOOL -> convertToBoolean(value);
                case CHAR -> convertToChar(value);
                case ARRAY -> handleArrayConversion(value);
                case VOID -> null;
                default -> value;
            };
        } catch (Exception e) {
            throw new Exception("Error converting value: " + value + " to type: " + targetType, e);
        }
    }

    private static Integer convertToInteger(Object value) {
        if (value instanceof Number num) return num.intValue();
        if (value instanceof Boolean bool) return bool ? 1 : 0;
        return Integer.parseInt(value.toString());
    }

    private static Double convertToDouble(Object value) {
        if (value instanceof Number num) return num.doubleValue();
        if (value instanceof Boolean bool) return bool ? 1.0 : 0.0;
        return Double.parseDouble(value.toString());
    }

    private static Float convertToFloat(Object value) {
        if (value instanceof Number num) return num.floatValue();
        if (value instanceof Boolean bool) return bool ? 1.0f : 0.0f;
        return Float.parseFloat(value.toString());
    }

    private static Long convertToLong(Object value) {
        if (value instanceof Number num) return num.longValue();
        if (value instanceof Boolean bool) return bool ? 1L : 0L;
        return Long.parseLong(value.toString());
    }

    private static Boolean convertToBoolean(Object value) {
        if (value instanceof Boolean bool) return bool;
        if (value instanceof Number num) return num.doubleValue() != 0.0;
        if (value instanceof Character ch) return ch != '\0';

        String strValue = value.toString().trim();
        return !strValue.equals("0");

        //return Boolean.parseBoolean(strValue);
    }

    private static Character convertToChar(Object value) {
        if (value instanceof Character ch) {
            return ch;
        }

        String str = value.toString();

        if (str.length() == 1) return str.charAt(0);

        try {
            int ascii = (value instanceof Number) ?
                    ((Number) value).intValue() :
                    Integer.parseInt(str);
            return (char) ascii;
        } catch (NumberFormatException e) {
            return '\0';
        }
    }

    private static Object handleArrayConversion(Object value) {
        if (value.getClass().isArray()) {
            return value;
        }
        throw new UnsupportedOperationException("Array conversion requires actual array input");
    }

    private static Object handleStructConversion(Object value) {
        throw new UnsupportedOperationException("Struct conversion not yet implemented");
    }
}