import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.LinkedList;

public class Node_Registry {

    private static LinkedList<String> nodes = new LinkedList<>();
    private static final String EXCHANGE_NAME = "node_logs";
    private static int count,num_nodes;
    private static int[][] topology;
    private static int[][] topology_virtual;

    public static void main(String[] argv) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");

        Connection connection = factory.newConnection();
        Channel send = connection.createChannel();
        Channel recv = connection.createChannel();

        send.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.DIRECT);
        recv.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.DIRECT);
        String queueName = recv.queueDeclare().getQueue();

        recv.queueBind(queueName, EXCHANGE_NAME, "new_node");
        recv.queueBind(queueName, EXCHANGE_NAME, "delete_node");
        recv.queueBind(queueName, EXCHANGE_NAME, "obtain_list_nodes");
        recv.queueBind(queueName, EXCHANGE_NAME, "obtain_connections");
        recv.queueBind(queueName, EXCHANGE_NAME, "send_message");

        System.out.println("Node Registry running...");

        //Obtain Physical Topology
        PhysicalTopology physicalTopology = new PhysicalTopology();
        topology = physicalTopology.getTopology_1();
        num_nodes = topology.length;

        Object monitor = new Object();
        count = 1;
        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), "UTF-8");
            String key = delivery.getEnvelope().getRoutingKey();
            String replyTo = delivery.getProperties().getReplyTo();

            // Set up the properties with the same correlation id
            AMQP.BasicProperties replyProps = new AMQP.BasicProperties.Builder()
            .correlationId(delivery.getProperties().getCorrelationId()).build();

            // Add node
            if (key.equals("new_node")) {
                String id = "node" + count;
                count += 1;
                nodes.add(id);
                System.out.println("New node registered with id: " + id);
                
                //Send the id to the new node
                send.basicPublish("", replyTo, replyProps, id.getBytes("UTF-8"));
                //send.basicAck(delivery.getEnvelope().getDeliveryTag(), false);

            // Delete node
            } else if (key.equals("delete_node")) {
                nodes.remove(message); // The node would have sent the ID in the 'message'
                System.out.println("Node removed with id: " + message);

            // Send the list of nodes
            } else if( key.equals("obtain_list_nodes")){
                String list = obtainListNodes();
                send.basicPublish(EXCHANGE_NAME, "list", null, list.getBytes("UTF-8"));

            // Get the connections of the node requesting them and send them to that node
            } else if( key.equals("obtain_connections")){
                char index = message.charAt(4);
                String connections = "",aux;

                for(int j = 0; j < num_nodes; j++){
                    aux = (topology[index - 1][j]).toString();
                    connections = connections.concat() + "/";
                }
            
                send.basicPublish("", replyTo, replyProps, connections.toString().getBytes("UTF-8"));

               // Initiate the sending of a message ///////////////////////////////////////////////////////////////////
            } else if( key.equals("send_message")){ 
                // Decode sender and receiver nodes provided by the overlay
                String srcNode = delivery.getProperties().getAppId();
                String destNode = delivery.getProperties().getUserId(); 

                // Decrement to repesent the actual node numbers
                Integer temp = Integer.parseInt(srcNode.trim());
                temp--;
                srcNode = temp.toString();
                temp = Integer.parseInt(destNode.trim());
                temp--;
                destNode = temp.toString();
                
                // Encapsulate destination node in message
                AMQP.BasicProperties sendProps = new AMQP.BasicProperties.Builder().userId(destNode).build();
                
                // Send message to source node
                send.basicPublish("", srcNode, sendProps, message.getBytes());
            }
            ///////////////////////////////////////////////////////////////

            synchronized (monitor) {
                monitor.notify();
            }
        };

        recv.basicConsume(queueName, true, deliverCallback, consumerTag -> {
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

    private static String obtainListNodes(){
        String result = " - ";
        for (String x : nodes) {
            result = result.concat(x) + " - ";
        }
        return result;
    }

    // Shouldn't this be a private function? //////////////////////////////////////////////////////////////////
    public static String obtainConnections(int node){
        ArrayList<Integer> connections = new ArrayList<Integer>();
        for(int j = 0; j < num_nodes; j++){

            if (node == j){
                connections.set(j, -1);
            } else {
                if (topology[node][j] == 0){
                    //Implement algorithm to obtain the shortest path
                } else {
                    connections.set(j,j);
                }
            }
        }
        return "Some String"; ////////////////////////////////////////////////////////////
    }
}
