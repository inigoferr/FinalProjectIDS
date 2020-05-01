import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

import Dijkstra.Dijkstra;
import Dijkstra.GraphPath;
import Dijkstra.NodePath;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.io.IOException;

public class Node_Registry {

    private static LinkedList<String> nodes = new LinkedList<>();
    private static final String REGISTRY_QUEUE = "registry";
    private static final String OVERLAY_QUEUE = "overlay";
    private static int count, num_nodes;
    private static int[][] topology;
    private static int[][] topology_virtual;

    private static GraphPath graph;
    private static ArrayList<NodePath> nodes_dijkstra;

    public static void main(String[] argv) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");

        Connection connection = factory.newConnection();
        Channel send = connection.createChannel();
        Channel recv = connection.createChannel();

        boolean durable = true;
        send.queueDeclare(OVERLAY_QUEUE, durable, false, false, null);
        recv.queueDeclare(REGISTRY_QUEUE, durable, false, false, null);

        // Obtain Physical Topology
        PhysicalTopology physicalTopology = new PhysicalTopology();
        topology = physicalTopology.getTopology_2();
        num_nodes = topology.length;

        // Calculate the Parameters to implement in the future Dijkstra
        calculateParametersForDijkstra(topology);

        // Initialize Virtual Topology
        initializeVirtualTopology();

        System.out.println("Node Registry running...");

        Object monitor = new Object();
        count = 1;
        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), "UTF-8");
            String key = delivery.getProperties().getAppId();
            String replyTo = delivery.getProperties().getReplyTo();
            String corrID = delivery.getProperties().getCorrelationId();
            Map<String,Object> headers = delivery.getProperties().getHeaders();

            // Add node
            if (key.equals("new_node")) {
                String id = "node" + count;
                count += 1;
                nodes.add(id);
                send.queueDeclare(id, durable, false, false, null);
                System.out.println("New node registered with id: " + id);

                // Send the id to the new node
                AMQP.BasicProperties newProps = new AMQP.BasicProperties.Builder().correlationId(corrID).build();
                send.basicPublish("", replyTo, newProps, id.getBytes("UTF-8"));

                // Delete node
            } else if (key.equals("delete_node")) {
                nodes.remove(message); // The node would have sent the ID in the 'message'
                System.out.println("Node removed with id: " + message);

                // Send the list of nodes
            } else if (key.equals("obtain_list_nodes")) {
                String list = obtainListNodes();
                AMQP.BasicProperties listProps = new AMQP.BasicProperties.Builder().appId("list").build();
                send.basicPublish("", OVERLAY_QUEUE, listProps, list.getBytes("UTF-8"));

                // Get the connections of the node requesting them and send them to that node
            } else if (key.equals("obtain_connections")) {
                int index = Integer.parseInt(message.substring(4));

                String table_routes = obtainConnections(index);

                AMQP.BasicProperties listProps = new AMQP.BasicProperties.Builder().correlationId(corrID).build();
                send.basicPublish("", replyTo, listProps, table_routes.getBytes());

                // Connect two nodes in the virtual topology
            } else if (key.equals("connect")) {
                // Decode nodes provided by the overlay
                String nodeX = headers.get("nodeX").toString();
                String nodeY = headers.get("nodeY").toString();

                if (isIdCorrect(nodeX) && isIdCorrect(nodeY)) {
                    connectNodesVirtualTopology(nodeX, nodeY);
                    System.out.println(nodeX + " connected to " + nodeY);
                } else {
                    // Notify the user
                    System.out.println("Error in the Id's");
                }
                
                // Disconnect two nodes in virtual overlay
            } else if (key.equals("disconnect")) {
                // Decode nodes provided by the overlay
                String nodeX = headers.get("nodeX").toString();
                String nodeY = headers.get("nodeY").toString();

                if (isIdCorrect(nodeX) && isIdCorrect(nodeY)) {
                    disconnectNodesVirtualTopology(nodeX, nodeY);
                    System.out.println(nodeX + " disconnected from " + nodeY);
                } else {
                    // Notify the user
                    System.out.println("Error in the Id's");
                }
                

                // Initiate the sending of a message
            } else if ((key.equals("send")) || (key.equals("send_left")) || (key.equals("send_right"))) {
                // Decode sender and receiver nodes provided by the overlay
                String srcNode = headers.get("srcNode").toString().trim();
                String destNode = null;

                if (key.equals("send")) {
                    destNode = headers.get("destNode").toString().trim();
                } else if (key.equals("send_left")) {
                    destNode = obtainLeft(srcNode); // Get from virtual topology array
                } else if (key.equals("send_right")) {
                    destNode = obtainRight(srcNode); // Get from virtual topology array
                }

                if (destNode.equals("error")) { // The node is not connected
                    // Notify the user
                    System.out.println("Node not connected");
                } else {
                    if (isIdCorrect(srcNode) && isIdCorrect(destNode)) {
                        // Encapsulate destination node in message and send message to source node
                        AMQP.BasicProperties sendProps = new AMQP.BasicProperties.Builder().headers(headers)
                                .build();
                        send.basicPublish("", srcNode, sendProps, message.getBytes());
                    } else {
                        System.out.println("Error in the Id's");
                        // Notify the user
                    }
                }
                System.out.println(srcNode + "|" + destNode);


            // Obtain physical topology
            } else if (key.equals("obtain_topology")) {
                try {
                    showTopology(topology,false);
                } catch (Exception e) {
                    System.out.println("Error in diplaying topology.");
                }

            // Obtain virtual topology
            } else if (key.equals("obtain_virtual_topology")) {
                try {
                    showTopology(topology_virtual,true);
                } catch (Exception e) {
                    System.out.println("Error in diplaying topology.");
                }
            }

            synchronized (monitor) {
                monitor.notify();
            }
        };
        recv.basicConsume(REGISTRY_QUEUE, true, deliverCallback, consumerTag -> {
        });

        // Wait and be prepared to consume the message from RPC client.
        while (true) {
            synchronized (monitor) {
                try {
                    monitor.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static String obtainListNodes() {
        String result = " - ";
        for (String x : nodes) {
            result = result.concat(x) + " - ";
        }
        return result;
    }

    private static String obtainConnections(int node) {
        // ArrayList<Integer> connections = new ArrayList<Integer>();
        String connections = "";
        // Substract one from the value of node
        node -= 1;

        graph = Dijkstra.calculateShortestPathFromSource(graph, nodes_dijkstra.get(node));

        System.out.println("Table Route of Node " + (node+1));
        for (int j = 0; j < num_nodes; j++) {
            if (node == j) {
                // It's the node
                if (j == (num_nodes - 1)) {
                    connections = connections.concat("-1");
                } else {
                    connections = connections.concat("-1:");
                }
            } else {
                List<NodePath> shortest_path = nodes_dijkstra.get(j).getShortestPath();
                int num;

                if (shortest_path.size() == 1) {
                    num = j + 1;
                } else {
                    num = Integer.parseInt(shortest_path.get(1).getName()) + 1;
                }

                if (j == (num_nodes - 1)) {
                    connections = connections.concat(Integer.toString(num));
                } else {
                    connections = connections.concat(Integer.toString(num)).concat(":");
                }
            }
        }

        System.out.println(connections);

        // Reinitialize the graph
        calculateParametersForDijkstra(topology);

        return connections;
    }

    private static void calculateParametersForDijkstra(int[][] matrix) {

        nodes_dijkstra = new ArrayList<>();
        graph = new GraphPath();
        num_nodes = matrix.length;

        for (int i = 0; i < num_nodes; i++) {
            NodePath new_node = new NodePath(Integer.toString(i));
            nodes_dijkstra.add(new_node);
        }

        for (int i = 0; i < num_nodes; i++) {
            NodePath node = nodes_dijkstra.get(i);
            for (int j = 0; j < num_nodes; j++) {
                if (topology[i][j] == 1) {
                    node.addDestination(nodes_dijkstra.get(j), 1);
                }
            }
            graph.addNode(node);
        }
    }

    private static void initializeVirtualTopology() {
        topology_virtual = new int[num_nodes][num_nodes];
        for (int i = 0; i < num_nodes; i++) {
            for (int j = 0; j < num_nodes; j++) {
                topology_virtual[i][j] = 0;
            }
        }
    }

    private static boolean connectNodesVirtualTopology(String nodeX, String nodeY) {
        boolean result = true;
        int x = Integer.parseInt(nodeX.substring(4));
        int y = Integer.parseInt(nodeY.substring(4));

        // Check if those nodes are connected to other nodes
        boolean leftX = false, rightX = false, leftY = false, rightY = false;

        for (int j = 0; j < num_nodes; j++) {
            if (topology_virtual[x - 1][j] == -1) { // nodeX has a node connected in its left
                leftX = true;
            } else if (topology_virtual[x - 1][j] == 1) { // nodeX has a node connected in its right
                rightX = true;
            } else if (topology_virtual[y - 1][j] == -1) { // nodeY has a node connected in its left
                leftY = true;
            } else if (topology_virtual[y - 1][j] == 1) { // nodeX has a node connected in its right
                rightY = true;
            }
        }

        // By default, the nodes are connected by its left

        if (!leftX && !rightX) { // nodeX is not connected with any node
            if (!leftY && !rightY) { // nodeY is not connected with any node
                topology_virtual[x - 1][y - 1] = -1;
                topology_virtual[y - 1][x - 1] = 1;
            } else {
                if (leftY && rightY) { // nodeY full of connections
                    result = false;
                } else {
                    topology_virtual[x - 1][y - 1] = -1;
                    if (leftY) { // nodeY has a node in its left
                        topology_virtual[y - 1][x - 1] = 1;
                    } else { // nodeY has a node in its right
                        topology_virtual[y - 1][x - 1] = -1;
                    }
                }
            }
        } else {
            if (leftX && rightX) { // nodeX full of connections
                result = false;
            } else {
                if (leftX) { // nodeX has a node in its left
                    if (!leftY && !rightY) { // nodeY is not connected with any node
                        topology_virtual[x - 1][y - 1] = 1;
                        topology_virtual[y - 1][x - 1] = -1;
                    } else {
                        if (leftY && rightY) {
                            result = false;
                        } else {
                            topology_virtual[x - 1][y - 1] = 1;
                            if (leftY) { // nodeY has a node in its left
                                topology_virtual[y - 1][x - 1] = 1;
                            } else { // nodeY has a node in its right
                                topology_virtual[y - 1][x - 1] = -1;
                            }
                        }

                    }
                } else { // nodeX has a node in its right
                    if (!leftY && !rightY) { // nodeY is not connected with any node
                        topology_virtual[x - 1][y - 1] = -1;
                        topology_virtual[y - 1][x - 1] = -1;
                    } else {
                        if (leftY && rightY) { // nodeY full of connections
                            result = false;
                        } else {
                            topology_virtual[x - 1][y - 1] = -1;
                            if (leftY) { // nodeY has a node in its left
                                topology_virtual[y - 1][x - 1] = 1;
                            } else { // nodeY has a node in its right
                                topology_virtual[y - 1][x - 1] = -1;
                            }
                        }
                    }
                }
            }
        }

        return result;
    }

    private static void disconnectNodesVirtualTopology(String nodeX, String nodeY) {
        int x = Integer.parseInt(nodeX.substring(4));
        int y = Integer.parseInt(nodeY.substring(4));

        topology_virtual[x - 1][y - 1] = 0;
        topology_virtual[y - 1][x - 1] = 0;

        return;
    }

    private static String obtainLeft(String source) {
        int start = Integer.parseInt(source.substring(4));
        String result = "error";
        boolean found = false;

        for (int j = 0; j < num_nodes; j++) {
            if (topology_virtual[start - 1][j] == -1) {
                result = "node" + (j + 1);
                found = true;
            }
        }

        if (found) {
            return result;
        } else { // No nodes connected or the only node connected is in its right
            return result;
        }
    }

    private static String obtainRight(String source) {
        int start = Integer.parseInt(source.substring(4));
        String result = "error";
        boolean found = false;

        for (int j = 0; j < num_nodes; j++) {
            if (topology_virtual[start - 1][j] == 1) {
                result = "node" + (j + 1);
                found = true;
            }
        }

        if (found) {
            return result;
        } else { // No nodes connected or the only node connected is in its right
            return result;
        }
    }

    private static boolean isIdCorrect(String id) {
        boolean result = false;
        String node = id.substring(0, 4);

        if (node.equals("node")) {
            int num = Integer.parseInt(id.substring(4));

            if (num >= 1 && num <= count) { // count stores the number of nodes initialized
                result = true;
            }
        }

        return result;
    }

    private static void showTopology(int[][] topology, boolean virtual) throws IOException {
        int width = 1000;
        int height = 500;
        String filename;

        if (virtual) {
            filename = "virtual_topology.jpg";
        } else {
            filename = "topology.jpg";
        }

        CreateImage creator = new CreateImage();
        creator.create(topology,filename,width,height);
    
        DrawImage drawer = new DrawImage();
        drawer.draw(filename,width,height);
    }

}
