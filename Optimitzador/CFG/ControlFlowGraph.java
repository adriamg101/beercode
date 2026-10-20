package Optimitzador.CFG;

import Optimitzador.CFG.CFGNodes.*;
import Optimitzador.OptimizerTriplet;
import ThreeAddressCode.TAC;
import ThreeAddressCode.Triplet;

import java.sql.Array;
import java.util.*;

public class ControlFlowGraph {
    private FlowGraph flowGraph;

    // Operadors del TAC
    private static final String START_FUNCTION = "start";
    private static final String END_FUNCTION = "END FUNC";
    private static final String GOTO = "goto";

    public ControlFlowGraph(TAC tac) {
        flowGraph = new FlowGraph();

        // Primer de tot fem el flowgraph de les funcions
        generateFlowGraphFuncs(tac);

        checkDeadCodeFuncs();

        // Busquem loops i fem loop-unrolling i en el cas de les funcions, posem el contingut de la funció
        // en la funcio on es crida.
        inliningFunctions(tac);
    }

    private void generateFlowGraphFuncs(TAC tac) {
        FlowGraphNodeFunc actualNode = null;
        String funcName = "";

        // Corremos el codigo para mirar de separar en nodos para ser analizados separadamente
        for (int i = 0; i < tac.getInstructionsSize(); i++) {
            // Comprobamos el operador
            if (tac.getInstructions(i).getOperand2() != null && tac.getInstructions(i).getOperand2().equals(START_FUNCTION)) { // Inici de funció
                // Comença una funció, hem de generar un nou node.
                actualNode = new FlowGraphNodeFunc();
                funcName = tac.getInstructions(i).getOperator();
                actualNode.addInstruction(tac.getInstructions(i), i);
            } else if (tac.getInstructions(i).getOperator().equals(END_FUNCTION)) {  // Fi de funció
                // Acabem el node de funcio i l'afegim a
                actualNode.addInstruction(tac.getInstructions(i), i);
                actualNode.resetLines();
                flowGraph.newNode(funcName, actualNode);
                actualNode = null;
            } else {
                actualNode.addInstruction(tac.getInstructions(i), i);
            }
        }
    }

    // Funció que fa check de funcions que mai es criden
    private void checkDeadCodeFuncs() {
        // Comencem mirant el main
        HashSet<String> usedFuncs = new HashSet<>();
        LinkedList<String> funcsToCheck = new LinkedList<>();
        funcsToCheck.add("main");
        FlowGraphNodeFunc actualNode;

        while (!funcsToCheck.isEmpty()) {
            // Primer busquem
            actualNode = flowGraph.getNode(funcsToCheck.getFirst());

            for (int i = 0; i < actualNode.instructionSize(); i++) {
                // Si ens trobem amb una funció de call
                if (actualNode.getInstruction(i).getInstruction().getOperator().equals(GOTO)) {
                    // Si encara no habiem comprobat la funcio ni esta en la cua a revisar
                    if (!usedFuncs.contains(actualNode.getInstruction(i).getInstruction().getOperand2()) &&
                    !funcsToCheck.contains(actualNode.getInstruction(i).getInstruction().getOperand2())) {
                        // L'afegim a la cua de funcions a revisar
                        funcsToCheck.addLast(actualNode.getInstruction(i).getInstruction().getOperand2());
                    }

                    // Tambe mirem si la funcio es recursiva
                    if (actualNode.getInstruction(i).getInstruction().getOperand2().equals(funcsToCheck.getFirst())) {
                        actualNode.setRecursive();
                    }
                }
            }

            // Guardem el nom de la funcio i eliminem de la llista de funcions a revisar.
            usedFuncs.add(funcsToCheck.getFirst());
            funcsToCheck.removeFirst();
        }

        // Eliminem les funcions que no s'utilitzen
        flowGraph.eliminateUnusedFuncs(usedFuncs);
    }

    // Funcio que desfa la funcio introduint el contingut en la funcio on es crida. Si no son funcions recursives.
    private TAC inliningFunctions(TAC tac) {
        ArrayList<OptimizerTriplet> reducedCode = flowGraph.inliningLines();
        ArrayList<Triplet> optTac = new ArrayList<>();
        for (OptimizerTriplet instruction : reducedCode) {
            optTac.add(instruction.getInstruction());
        }
        tac.setInstructions(optTac);
        return tac;
    }
}
