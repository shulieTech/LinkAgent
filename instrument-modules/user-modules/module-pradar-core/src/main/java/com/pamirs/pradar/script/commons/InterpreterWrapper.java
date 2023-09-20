package com.pamirs.pradar.script.commons;

import bsh.Interpreter;

public class InterpreterWrapper {

    private Interpreter interpreter;
    private volatile boolean hold;

    public InterpreterWrapper(Interpreter interpreter, boolean hold) {
        this.interpreter = interpreter;
        this.hold = hold;
    }

    public Interpreter getInterpreter() {
        return interpreter;
    }

    public void setInterpreter(Interpreter interpreter) {
        this.interpreter = interpreter;
    }

    public boolean isHold() {
        return hold;
    }

    public void setHold(boolean hold) {
        this.hold = hold;
    }
}
