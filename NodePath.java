
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class NodePath {

    private String name;

    private LinkedList<NodePath> shortestPath = new LinkedList<>();

    private Integer distance = Integer.MAX_VALUE;

    private Map<NodePath, Integer> adjacentNodes = new HashMap<>();

    public NodePath(String name) {
        this.name = name;
    }

    public void addDestination(NodePath destination, int distance) {
        adjacentNodes.put(destination, distance);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<NodePath, Integer> getAdjacentNodes() {
        return adjacentNodes;
    }

    public void setAdjacentNodes(Map<NodePath, Integer> adjacentNodes) {
        this.adjacentNodes = adjacentNodes;
    }

    public Integer getDistance() {
        return distance;
    }

    public void setDistance(Integer distance) {
        this.distance = distance;
    }

    public List<NodePath> getShortestPath() {
        return shortestPath;
    }

    public void setShortestPath(LinkedList<NodePath> shortestPath) {
        this.shortestPath = shortestPath;
    }
}