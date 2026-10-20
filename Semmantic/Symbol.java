package Semmantic;

import javax.xml.crypto.Data;
import java.util.List;
import java.util.Objects;

public class Symbol {
    private final String name;
    private Object value;
    private final String type; // "func" o "var"
    private final DataType dataType; // tipo de variable o q retorna funcion
    private final int scopeId; //scope que ocupa la funcion
    private final List<Argument> arguments;
    private final int ArraySize;



    // Constructor para funciones
    public Symbol(String name, String type, DataType dataType, int scopeId, List<Argument> arguments, int ArraySize) {
        this.name = name;
        this.type = type;
        this.scopeId = scopeId;
        this.arguments = arguments;
        this.ArraySize=ArraySize;
        if (this.ArraySize > 0) {
            String arrayType = dataType.getToken();
            this.dataType = DataType.ARRAY;
            this.value = this.dataType.getDefaultValue(arrayType, this.ArraySize);
        } else {
            this.dataType = dataType;
            this.value = dataType.getDefaultValue();
        }
    }

    public Object operation(Symbol operand2, String operator) throws Exception {
        /*
        try {
            Object convertedValue = TypeConverter.convert(operand2.value, this.dataType);
            return OperationEvaluator.evaluate(operand2.value, convertedValue, operator);   // canviat this.value per operand2.value
        } catch (Exception e) {
            throw new Exception("Error en la operación: " + e.getMessage());
        }

         */
        return null;
    }

    public void setValue(Object value, int index) throws Exception {
        if (value == null) {
            this.value = null;
            return;
        }

        try {
            if (dataType == DataType.ARRAY) {
                Object convertedVal = TypeConverter.convert(value, DataType.fromToken(String.valueOf(this.getArrayType())));
                java.lang.reflect.Array.set(this.value, index, convertedVal);
                return;
            }
            this.value = TypeConverter.convert(value, dataType);
        } catch (Exception e) {
            throw new Exception("Error en la operación: " + e.getMessage());
        }
    }

    // Getters
    public Object getValue() {
        if (this.dataType == DataType.ARRAY) {
            if (this.value instanceof int[] intArray && intArray.length > 0) {
                return intArray[0];
            } else if (this.value instanceof Object[] objArray && objArray.length > 0) {
                return objArray[0];
            }
            return this.value;
        }
        return value;
    }

    public int getArraySize(){return ArraySize;}
    public String getName() { return name; }
    public String getType() { return type; }
    public DataType getDataType() {
        return dataType;
    }

    public DataType getArrayType() {
        if (dataType == DataType.ARRAY) {
            return DataType.fromToken(String.valueOf(this.value.getClass().getComponentType()));
        }
        return null;
    }

    public int getScopeId() { return scopeId; }
    public List<Argument> getArguments() {return arguments;}



    @Override
    public String toString() {
        return String.format("%s [%s] -> %s (Scope: %d)",
                name, type, dataType, scopeId);
    }
}
