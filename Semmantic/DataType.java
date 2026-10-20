package Semmantic;



import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public enum DataType {
    INT("INT", 0),
    BOOL("BOOL", false),
    CHAR("CHAR",  '\0'),
    FLOAT("FLOAT",  0.0f),
    DOUBLE("DOUBLE",  0.0),
    LONG("LONG",  0L),
    ARRAY("ARRAY", ArrayType.UNKNOWN),
    VOID("VOID",  null),
    FUNC("FUNC",  null),
    UNKNOWN("UNKNOWN",  null);

    private static final Map<String, DataType> typeCache = new HashMap<>();
    private static final Pattern INT_PATTERN = Pattern.compile("^[0-9]+$");
    private static final Pattern DECIMAL_PATTERN = Pattern.compile("^[0-9]+\\.[0-9]+$");
    private static final Pattern CHAR_PATTERN = Pattern.compile("^'.'$");
    private static final Pattern STRING_PATTERN = Pattern.compile("^\".+\"$");

    private final String token;
    private final Object defaultValue;

    DataType(String token, Object defaultValue) {
        this.token = token;
        this.defaultValue = defaultValue;
    }

    public String getToken() {
        return token;
    }

    public Object getDefaultValue() {
        return defaultValue;
    }

    public Object getDefaultValue(String type, int size) {
        return ArrayType.fromString(type).createArray(size);
    }

    public static boolean isDataType(String token) {
        for (DataType type : values()) {
            if (type.token.equals(token)) {
                return true;
            }
        }
        return false;
    }

    public static DataType fromToken(String token) {
        String tokenUpper = token.toUpperCase();
        for (DataType type : values()) {
            if (type.token.equals(tokenUpper)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Tipo de dato no válido: " + token);
    }

    public static DataType inferTypeFromValue(String value) {
        if (value == null) return DataType.VOID;

        return typeCache.computeIfAbsent(value, v -> {
            if (INT_PATTERN.matcher(v).matches()) return DataType.INT;
            if (DECIMAL_PATTERN.matcher(v).matches()) return DataType.DOUBLE;
            if (CHAR_PATTERN.matcher(v).matches()) return DataType.CHAR;
            if (v.equals("true") || v.equals("false")) return DataType.BOOL;
            return DataType.UNKNOWN;
        });
    }

    public static Object parseValue(String value, DataType targetType) {
        try {
            if (value == null) return null;

            return switch (targetType) {
                case INT -> Integer.parseInt(value);
                case DOUBLE -> Double.parseDouble(value);
                case CHAR -> value.charAt(1); // Para formato 'a'
                case BOOL -> Boolean.parseBoolean(value);
                default -> value; // Para UNKNOWN o otros tipos
            };
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    String.format("No se puede convertir '%s' a %s", value, targetType.getToken()));
        }
    }
}

