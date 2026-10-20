package Semmantic;

public enum ArrayType {
    INT {
        @Override
        public Object createArray(int size) {
            return new int[size];
        }
    },
    BOOL {
        @Override
        public Object createArray(int size) {
            return new boolean[size];
        }
    },
    CHAR {
        @Override
        public Object createArray(int size) {
            return new char[size];
        }
    },
    FLOAT {
        @Override
        public Object createArray(int size) {
            return new float[size];
        }
    },
    DOUBLE {
        @Override
        public Object createArray(int size) {
            return new double[size];
        }
    },
    LONG {
        @Override
        public Object createArray(int size) {
            return new long[size];
        }
    },
    UNKNOWN {
        @Override
        public Object createArray(int size) {
            return new Object[size];
        }
    };

    public abstract Object createArray(int size);

    public static ArrayType fromString(String type) {
        try {
            return ArrayType.valueOf(type);
        } catch (IllegalArgumentException e) {
            return UNKNOWN;
        }
    }
}
