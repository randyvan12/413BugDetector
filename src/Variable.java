class Variable {
    String name;
    boolean isPointer;
    PointerState state;
    public Variable(String name, boolean isPointer, PointerState state) {
        this.name = name;
        this.isPointer = isPointer;
        this.state = state;
    }
    enum PointerState {
        ASSIGNED, NULL, POTENTIALLY_NULL
    }
}
