import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;

import java.util.*;

public class ControlFlowGraph {
    CFGNode startNode;

    public void printGraph() {
        printNode(startNode, new HashSet<>());
    }

    private void printNode(CFGNode node, Set<CFGNode> visited) {
        if (node == null || visited.contains(node)) {
            return;
        }
        visited.add(node);
        System.out.println("Node: " + node.code);
        for (CFGNode successor : node.successors) {
            System.out.println("  Successor: " + successor.code);
            printNode(successor, visited);
        }
    }

    public CFGNode[] getAllNodes() {
        if (startNode == null) {
            return new CFGNode[0];
        }

        List<CFGNode> allNodes = new ArrayList<>();
        Set<CFGNode> visited = new HashSet<>();
        Queue<CFGNode> queue = new LinkedList<>();

        // Start BFS from the startNode
        queue.add(startNode);
        visited.add(startNode);

        while (!queue.isEmpty()) {
            CFGNode currentNode = queue.poll();
            allNodes.add(currentNode);

            // Enqueue successors that haven't been visited yet
            for (CFGNode successor : currentNode.successors) {
                if (!visited.contains(successor)) {
                    queue.add(successor);
                    visited.add(successor);
                }
            }
        }

        // Convert list to array and return
        return allNodes.toArray(new CFGNode[0]);
    }
}
