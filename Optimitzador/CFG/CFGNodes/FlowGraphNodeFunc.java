package Optimitzador.CFG.CFGNodes;

import Optimitzador.OptimizerTriplet;

import java.lang.reflect.Array;
import java.util.ArrayList;

public class FlowGraphNodeFunc extends FlowGraphNode {
    private boolean isRecursive;

    public FlowGraphNodeFunc() {
        super();
        this.isRecursive = false;
    }

    public void setRecursive() {
        this.isRecursive = true;
    }

    public boolean isRecursive() {
        return this.isRecursive;
    }
}
