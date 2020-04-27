import java.util.HashSet;
import java.util.Set;

public class GraphPath {

    private Set<NodePath> nodes = new HashSet<>();

    public void addNode(NodePath nodeA) {
        nodes.add(nodeA);
    }

    public Set<NodePath> getNodes() {
        return nodes;
    }

    public void setNodes(Set<NodePath> nodes) {
        this.nodes = nodes;
    }
}