import java.io.IOException;
import java.util.UUID;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

public class Node {

    private static final String REGISTRY_QUEUE = "registry";
    private static String id;
    private static String corrId, corrId2;
    private static int[] connections; 

    public static void main(String[] argv) throws Exception {
        
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");

        Connection connection = factory.newConnection();
        Channel send = connection.createChannel();
        Channel recv = connection.createChannel();

        String queueName = recv.queueDeclare().getQueue();
        send.queueDeclare(REGISTRY_QUEUE);

        id = "0";
        //The Runtime.getRuntime()... section catches Ctrl+C and tells the Node_Registry the node is deleted/disconnected
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                // Tell the Node_Registry that the node is being deleted/disconnected
                try {
                    if( !id.equals("0")){
                        AMQP.BasicProperties deleteProps = new AMQP.BasicProperties.Builder().appId("delete_node").build(); 
                        send.basicPublish("", REGISTRY_QUEUE, deleteProps, id.getBytes());   
                    }
                    System.out.println("Shutting down ...");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        System.out.println("Node is running...");

        // Register the Node by sending a message to the Node_Registry
        corrId = UUID.randomUUID().toString();
        AMQP.BasicProperties newProps = new AMQP.BasicProperties.Builder().appId("new_node").correlationId(corrId).replyTo(queueName).build(); 
        send.basicPublish("", REGISTRY_QUEUE, newProps, null); 
        
        
        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            // Receive the id of the node in the Node_Registry Linked List
            if (delivery.getProperties().getCorrelationId().equals(corrId)) {
                String id = new String(delivery.getBody(), "UTF-8");
                System.out.println("Node registered with id: " + id);

                //Create queues to receive messages from other Nodes and Node_Registry
                recv.queueDeclare(id);//This queue will receive messages from other nodes

                //Request its connections with other nodes
                corrId2 = UUID.randomUUID().toString();
                
                AMQP.BasicProperties connectionProps = new AMQP.BasicProperties.Builder().appId("obtain_connections").correlationId(corrId2).replyTo(queueName).build(); 
                send.basicPublish("", REGISTRY_QUEUE, connectionProps, id.getBytes()); 

            // Receive connections with other nodes
            } else if (delivery.getProperties().getCorrelationId().equals(corrId2)){
                byte[] table_route = delivery.getBody();

                //Translate byte[] to int[]
                for(int i = 0; i < table_route.length; i++){
                    connections[i] = (int) table_route[i];
                }

            // Receive normal message 
            } else {
                String message = new String(delivery.getBody(), "UTF-8");
                String destNode = delivery.getProperties().getUserId(); 
                if (destNode.equals(id)) {
                    System.out.println(message);
                } else {
                    // Encapsulate destination node in message
                    AMQP.BasicProperties nextProps = new AMQP.BasicProperties.Builder().userId(destNode).build();

                    // Lookup which node must send to in order to reach destNode and send
                    int id = Character.getNumericValue(destNode.charAt(4));
                    Integer link = connections[id-1];
                    String nextNode = "node" + link.toString();

                    // Send message to next node
                    send.basicPublish("", nextNode, nextProps, message.getBytes());
                }
            }
        };

        recv.basicConsume(queueName, true, deliverCallback, consumerTag -> {});
        recv.basicConsume(id,true,deliverCallback,consumerTag -> {}); 
    }

}