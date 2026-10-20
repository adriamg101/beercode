package ParsingTree;

import java.util.ArrayList;
import java.util.Set;

public class ParsingTreeNavigator {
    private ParsingTree current;
    private final Set<String> constants;
    private boolean optimize;

    public ParsingTreeNavigator(ParsingTree root, Set<String> constants, boolean optimize) {
        this.current = root;
        this.constants = constants;
        this.optimize = optimize;
    }

    public void setError(String tok) {
        this.getObjectiveNode(tok).setError();
    }
    private ParsingTree getObjectiveNode(String node) {
        if (this.current.getToken().equals(node)) {
            return this.current;
        }

        ParsingTree actualNode = this.current;
        this.goToParent();

        if (this.current == null) return actualNode;

        int index = this.current.getChildren().indexOf(actualNode);
        for (int i = index + 1; i < this.current.getChildren().size(); i++) {
            ParsingTree cNode = this.current.getChild(i);
            if (cNode != null && cNode.getToken().equals(node)) {
                return cNode;
            }
        }
        return getObjectiveNode(node);
    }

    private void getActualNode(String node) {
        if (node == null) return;

        // Check if the actual is the good one
        if (this.current.getToken().equals(node) && this.current.getChildren().isEmpty()) {
            return;
        }

        // Check if brothers are the ones good
        ParsingTree actualNode = this.current;
        this.goToParent();

        // Check if the parent is null, if so, we are at the root
        if (this.current == null) this.current = actualNode;

        int index = this.current.getChildren().indexOf(actualNode);
        for (int i = index + 1; i < this.current.getChildren().size(); i++) {
            ParsingTree cNode = this.current.getChild(i);
            if (cNode != null && cNode.getToken().equals(node) && cNode.getChildren().isEmpty()) {
                this.current = cNode;
                return;
            }
        }
        // Call recursively for finding on top of the tree
        getActualNode(node);
    }

    /**
     * Function that sets the value of the token of the Valuable node (such ID -> "EstrelaDamm" or CTE_ENTER -> 5)
     * @param value String that represents the value of the token
     * @param node String that represents the node name
     */
    public void setTokenValue(String value, String node) {
        if (!constants.contains(node)) return;
        this.getActualNode(node);
        String processedValue = value.replaceAll("^['\"]|['\"]$", "");
        this.current.setNodeValue(processedValue);

    }

    /**
     * Function in charge of adding all the children to the correct node
     * @param children ArrayList of children to add
     * @param node String that represents the node name
     */
    public void addChildren(ArrayList<String> children, String node) {
        ArrayList<ParsingTree> childrenNodes = new ArrayList<>();
        for (String child : children) {
            ParsingTree childNode = new ParsingTree(child, null);
            childrenNodes.add(childNode);
        }

        this.getActualNode(node);
        ParsingTree originalCurrent = this.current; //Si acabem no afegint el node ens caldrà tornar enrera (No he trobat manera més neta de fer-ho :/)

        //Si només té un fill a afegir...
        if (optimize && children.size() == 1) {
            String childToken = children.get(0);

            if (childToken.equals("ε")) { //I és conjunt buit...
                ParsingTree insertionPoint = findInsertionPoint(current);

                //Podem branques i no afegim el nou node (mantenim el current)
                if (insertionPoint != null) {
                    ParsingTree parent = insertionPoint.getParent();
                    if (parent != null) {
                        int i = parent.getChildren().indexOf(insertionPoint);
                        if (i != -1) {
                            parent.getChildren().remove(i);
                            this.current = originalCurrent;
                            return;
                        }
                    }
                }
            }
            else if (!childToken.startsWith("<")) { //I és un no terminal...
                ParsingTree singleChild = childrenNodes.get(0);
                ParsingTree insertionPoint = findInsertionPoint(current);

                //Podem branques i afegim el nou node ""inlined""
                if (insertionPoint != null) {
                    ParsingTree parent = insertionPoint.getParent();
                    if (parent != null) {
                        int i = parent.getChildren().indexOf(insertionPoint);
                        if (i != -1) {
                            parent.getChildren().set(i, singleChild);
                            singleChild.setParent(parent);
                            this.current = singleChild; //Movem el current al nou node per a no perdre la ref!
                            return;
                        }
                    }
                }
            }
        }

        this.current.addChildren(childrenNodes);
        this.current = this.current.getChildren().getFirst();
    }


    private ParsingTree findInsertionPoint(ParsingTree node) {
        ParsingTree currentNode = node;
        while (currentNode.getParent() != null) {
            ParsingTree parent = currentNode.getParent();
            if (parent.getChildren().size() > 1) {
                return currentNode;
            }
            currentNode = parent;
        }
        return currentNode; //No hauriem d'arribar aquí ja que el root (axioma) mai entrarà en aquest cas...
    }

    public void goToParent() {
        if (current.getParent() != null) {
            current = current.getParent();
        }
    }

    public String getCurrentToken() {
        return current.getToken();
    }
}
