package Semmantic;

public class Argument {
    private final String name;
    private final DataType dataType;
    private final int ArraySize;

    public Argument(String name, DataType dataType, int ArraySize) {
        this.name = name;
        this.dataType = dataType;
        this.ArraySize=ArraySize;
    }

    public String getName() {
        return name;
    }

    public DataType getDataType() {
        return dataType;
    }
    public int getArraySize() {
        return ArraySize;
    }
}

