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
import java.util.LinkedList;
import java.util.List;

public class Node_Registry {

    private static LinkedList<String> nodes = new LinkedList<>();
    private static final String REGISTRY_QUEUE = "registry"; 
    private static final String OVERLAY_QUEUE = "overlay"; 
    private static int count,num_nodes;
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

        send.queueDeclare(OVERLAY_QUEUE); 
        recv.queueDeclare(REGISTRY_QUEUE);

        //Obtain Physical Topology
        PhysicalTopology physicalTopology = new PhysicalTopology();
        topology = physicalTopology.getTopology_1();
        num_nodes = topology.length;

        // Calculate the Parameters to implement in the future Dijkstra
        calculateParametersForDijkstra(topology);
        System.out.println("Node Registry running...");

        Object monitor = new Object();
        count = 1;
        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), "UTF-8");
            String key = delivery.getProperties().getAppId(); 
            String replyTo = delivery.getProperties().getReplyTo();
            String corrID = delivery.getProperties().getCorrelationId();

            // Add node
            if (key.equals("new_node")) {
                String id = "node" + count;
                count += 1;
                nodes.add(id);
                send.queueDeclare(id);
                System.out.println("New node registered with id: " + id);

                //Send the id to the new node
                AMQP.BasicProperties newProps = new AMQP.BasicProperties.Builder().correlationId(corrID).build();
                send.basicPublish("", replyTo, newProps, id.getBytes("UTF-8"));

            // Delete node
            } else if (key.equals("delete_node")) {
                nodes.remove(message); // The node would have sent the ID in the 'message'
                System.out.println("Node removed with id: " + message);

            // Send the list of nodes
            } else if( key.equals("obtain_list_nodes")) {
                String list = obtainListNodes();
                AMQP.BasicProperties listProps = new AMQP.BasicProperties.Builder().appId("list").build();
                send.basicPublish("",OVERLAY_QUEUE, listProps, list.getBytes("UTF-8"));

            // Get the connections of the node requesting them and send them to that node
            } else if( key.equals("obtain_connections")){
                char index = message.charAt(4);
                ArrayList<Integer> table_routes = obtainConnections(Character.getNumericValue(index));
                Object[] x = table_routes.toArray();
                byte[] envelop = new byte[x.length];

                System.out.print("Table Routes");
                for (int i = 0; i < x.length; i++){
                    envelop[i] = (byte) x[i];
                    System.out.print(envelop[i] + "|");
                }
                System.out.println("");

                AMQP.BasicProperties listProps = new AMQP.BasicProperties.Builder().correlationId(corrID).build();
                send.basicPublish("", replyTo, replyProps, envelop);

            // Connect two nodes in the virtual topology
            } else if (key.equals("connect")) {
                // Decode nodes provided by the overlay
                String nodeX = delivery.getProperties().getUserId(); 
                String nodeY = delivery.getProperties().getClusterId();

                // connect the nodes in virtual topology //!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

            
            } else if (key.equals("disconnect")) {
                // Decode nodes provided by the overlay
                String nodeX = delivery.getProperties().getUserId(); 
                String nodeY = delivery.getProperties().getClusterId();

                // connect the nodes in virtual topology //!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!


                // Initiate the sending of a message 
            } else if( (key.equals("send")) || (key.equals("send_left")) || (key.equals("send_right"))) { 
                // Decode sender and receiver nodes provided by the overlay
                String srcNode = delivery.getProperties().getUserId(); 

                if (key.equals("send")) {
                    String destNode = delivery.getProperties().getClusterId();
                } else if (key.equals("send_left")) {
                    String destNode = ; // Get from virtual topology array //!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
                } else if (key.equals("send_right")) {
                    String destNode = ; // Get from virtual topology array //!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
                }

                // Encapsulate destination node in message and send message to source node
                AMQP.BasicProperties sendProps = new AMQP.BasicProperties.Builder().userId(destNode).build();         
                send.basicPublish("", srcNode, sendProps, message.getBytes());
            }


            synchronized (monitor) {
                monitor.notify();
            }
        };
        recv.basicConsume(REGISTRY_QUEUE, true, deliverCallback, consumerTag -> {}); 

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

    private static String obtainListNodes(){
        String result = " - ";
        for (String x : nodes) {
            result = result.concat(x) + " - ";
        }
        return result;
    }

    private static ArrayList<Integer> obtainConnections(int node) {
        ArrayList<Integer> connections = new ArrayList<Integer>();

        graph = Dijkstra.calculateShortestPathFromSource(graph, nodes_dijkstra.get(node));

        System.out.print("Table Route of Node " + node);
        for (int j = 0; j < num_nodes; j++) {

            if ( node == j){
                connections.set(j,-1); //It's the node
            } else {
                List<NodePath> shortest_path = nodes_dijkstra.get(j).getShortestPath();

                connections.set(j,Integer.parseInt(shortest_path.get(0).getName())-1);
            }
            System.out.print(connections.get(j) + "|");
        }
        System.out.println("");
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
}
