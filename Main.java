
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import Dijkstra.Dijkstra;
import Dijkstra.GraphPath;
import Dijkstra.NodePath;

public class Main {

    public static void main(final String[] argv) {

        PhysicalTopology ph = new PhysicalTopology();
        int[][] topology = ph.getTopology_1();
        int num_nodes = topology.length;

        ArrayList<NodePath> nodes = new ArrayList<>();
        GraphPath graph = new GraphPath();

        for (int i = 0; i < num_nodes; i++) {
            NodePath new_node = new NodePath(Integer.toString(i));
            nodes.add(new_node);
        }

        for (int i = 0; i < num_nodes; i++) {
            NodePath node = nodes.get(i);
            for (int j = 0; j < num_nodes; j++) {
                if (topology[i][j] == 1) {
                    node.addDestination(nodes.get(j), 1);
                }
            }
            graph.addNode(node);
        }

        graph = Dijkstra.calculateShortestPathFromSource(graph, nodes.get(0));
        List<NodePath> shortest_path = nodes.get(1).getShortestPath();

        for (NodePath x : shortest_path) {
            System.out.print(" > " + x.getName());
        }
        System.out.println("");
        /*
         * final NodePath nodeA = new NodePath("A"); final NodePath nodeB = new
         * NodePath("B"); final NodePath nodeC = new NodePath("C"); final NodePath nodeD
         * = new NodePath("D"); final NodePath nodeE = new NodePath("E"); final NodePath
         * nodeF = new NodePath("F");
         * 
         * nodeA.addDestination(nodeB, 10); nodeA.addDestination(nodeC, 15);
         * 
         * nodeB.addDestination(nodeD, 12); nodeB.addDestination(nodeF, 15);
         * 
         * nodeC.addDestination(nodeE, 10);
         * 
         * nodeD.addDestination(nodeE, 2); nodeD.addDestination(nodeF, 1);
         * 
         * nodeF.addDestination(nodeE, 5);
         * 
         * GraphPath graph = new GraphPath();
         * 
         * graph.addNode(nodeA); graph.addNode(nodeB); graph.addNode(nodeC);
         * graph.addNode(nodeD); graph.addNode(nodeE); graph.addNode(nodeF);
         */

        /*graph = Dijkstra.calculateShortestPathFromSource(graph, nodeA);

        List<NodePath> list = nodeE.getShortestPath();

        for (NodePath nodePath : list) {
            System.out.println(nodePath.getName());
        }*/
    }
}