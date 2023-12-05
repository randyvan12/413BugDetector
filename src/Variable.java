class Variable {
    String name;
    boolean isPointer;
    boolean isNull;

    public Variable(String name, boolean isPointer, boolean isNull) {
        this.name = name;
        this.isPointer = isPointer;
        this.isNull = isNull;
    }
}
