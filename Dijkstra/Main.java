import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class Main {

    public static void main(String[] argv) {

        PhysicalTopology ph = new PhysicalTopology();
        int[][] topology = ph.getTopology_1();
        int num_nodes = topology.length;

        ArrayList<NodePath> nodes = new ArrayList<>();
        GraphPath graph = new GraphPath();

        for (int i = 0; i < num_nodes; i++) {
            NodePath new_node = new NodePath(Integer.toString(i));
            nodes.add(new_node);
        }

        
        for (int i = 0; i < num_nodes; i++){
            NodePath node = nodes.get(i);
            for (int j = 0; j < num_nodes; j++) {
                if (topology[i][j] == 1) {
                    node.addDestination(nodes.get(j),1);
                }
            }
            graph.addNode(node);
        }

        
        graph = Dijkstra.calculateShortestPathFromSource(graph, nodes.get(0));
        List<NodePath> shortest_path = nodes.get(0).getShortestPath();
        for (NodePath x : shortest_path) {
            System.out.print(" > " + x.getName());
        }
        System.out.println("");
    }
}