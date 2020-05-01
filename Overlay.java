import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.UUID;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

public class Overlay {

    private static final String OVERLAY_QUEUE = "overlay"; 
    private static final String REGISTRY_QUEUE = "registry";

    public static void main(String[] argv) throws Exception {
        String corrId, nodeX, nodeY, userMessage;

        try (Scanner scan = new Scanner(System.in)) {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost("localhost");

            Connection connection = factory.newConnection();
            Channel send = connection.createChannel();
            Channel recv = connection.createChannel();

            boolean durable = true;
            //Connections
            recv.queueDeclare(OVERLAY_QUEUE,durable,false, false, null); 
            send.queueDeclare(REGISTRY_QUEUE,durable,false, false, null); 

            // Connection with Node_Registry
            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                String message = new String(delivery.getBody(), "UTF-8");
                String key = delivery.getProperties().getAppId(); 

                if(key.equals("list")){
                    System.out.println("List of nodes: " + message);
                } else if (key.equals("topology")){
                    String[] array = message.split(":");
                    Map<String,Object> headers = delivery.getProperties().getHeaders();
                    
                    int num_nodes = Integer.parseInt(headers.get("num_nodes").toString());

                    int[][] topology = new int[num_nodes][num_nodes];

                    System.out.println("Physical Topology");
                    int k = 0;
                    for(int i = 0; i < num_nodes; i++){
                        for (int j = 0; j < num_nodes; j++){
                            topology[i][j] = Integer.parseInt(array[k]);
                            k++;
                            System.out.print(topology[i][j] + " ");
                        }
                        System.out.println("");
                    }

                } else if (key.equals("virtual_topology")){
                    String[] array = message.split(":");
                    Map<String,Object> headers = delivery.getProperties().getHeaders();
                    
                    int num_nodes = Integer.parseInt(headers.get("num_nodes").toString());

                    int[][] virtual_topology = new int[num_nodes][num_nodes];

                    int k = 0;
                    System.out.println("Physical Topology");
                    for(int i = 0; i < num_nodes; i++){
                        for (int j = 0; j < num_nodes; j++){
                            virtual_topology[i][j] = Integer.parseInt(array[k]);
                            k++;
                            System.out.print(virtual_topology[i][j] + " ");
                        }
                        System.out.println("");
                    }
                }
                
            };
            recv.basicConsume(OVERLAY_QUEUE, true, deliverCallback, consumerTag -> {}); 

            // Menu
            /*
             * Menu options: 
             * 1) Obtain list of nodes 
             * 2) Connect in the virtual ring 
             * 3) Disconnect in the virtual ring 
             * 4) Show topology of the of the network (Physical Layer)
             * 5) Show topology of the overlaying (Logic Layer = Virtual Ring)
             * 6) Send msg from Node X to Node Y 
             * 7) Send Left 
             * 8) Send Right 
             * 9) Help: To see the commands available 
             * 10) Exit
             */
            System.out.println("------- Overlay Ring -------");
            showMenu();
            boolean connected = true;
            while (connected) {
                // Get and format message from user
                System.out.print("Overlay >> ");
                String input = scan.next();
                switch (input) {
                    case "list":
                        AMQP.BasicProperties listProps = new AMQP.BasicProperties.Builder().appId("obtain_list_nodes").build();
                        send.basicPublish("", REGISTRY_QUEUE, listProps, null);
                        break;

                    case "connect": 
                        nodeX = scan.next();
                        nodeY = scan.next();

                        //Set headers
                        Map<String, Object> headers = new HashMap<String, Object>();
                        headers.put("nodeX",nodeX);
                        headers.put("nodeY",nodeY);

                        //AMQP.BasicProperties connectProps = new AMQP.BasicProperties.Builder().appId("connect").userId(nodeX).clusterId(nodeY).build();
                        AMQP.BasicProperties connectProps = new AMQP.BasicProperties.Builder().headers(headers).appId("connect").build();
                        //AMQP.BasicProperties connectProps = new AMQP.BasicProperties.Builder().appId("connect").contentType(nodeX).clusterId(nodeY).build();
                        send.basicPublish("", REGISTRY_QUEUE, connectProps, null);
                        break;

                    case "disconnect":
                        nodeX = scan.next();
                        nodeY = scan.next();
                        AMQP.BasicProperties disconnectProps = new AMQP.BasicProperties.Builder().appId("disconnect").contentType(nodeX).clusterId(nodeY).build();
                        send.basicPublish("", REGISTRY_QUEUE, disconnectProps, null);
                        break;

                    case "show_topology":
                        AMQP.BasicProperties show_topologyProps = new AMQP.BasicProperties.Builder().appId("obtain_topology").build();
                        send.basicPublish("", REGISTRY_QUEUE, show_topologyProps, null);
                        break;

                    case "show_topology_overlay":
                        AMQP.BasicProperties show_virtualtopologyProps = new AMQP.BasicProperties.Builder().appId("obtain_virtual_topology").build();
                        send.basicPublish("", REGISTRY_QUEUE, show_virtualtopologyProps, null);
                        break;

                    case "send":
                        nodeX = scan.next();
                        nodeY = scan.next();
                        scan.skip(" ");
                        userMessage = scan.nextLine();
                        AMQP.BasicProperties sendProps = new AMQP.BasicProperties.Builder().appId("send").contentType(nodeX).clusterId(nodeY).build();
                        send.basicPublish("", REGISTRY_QUEUE, sendProps, userMessage.getBytes("UTF-8"));
                        break;

                    case "send_left":
                        nodeX = scan.next();
                        scan.skip(" ");
                        userMessage = scan.nextLine();
                        AMQP.BasicProperties sendLeftProps = new AMQP.BasicProperties.Builder()
                            .appId("send_left").contentType(nodeX).build();
                        send.basicPublish("", REGISTRY_QUEUE, sendLeftProps, userMessage.getBytes("UTF-8"));
                        break;

                    case "send_right":
                        nodeX = scan.next();
                        scan.skip(" ");
                        userMessage = scan.nextLine();
                        AMQP.BasicProperties sendRightProps = new AMQP.BasicProperties.Builder().appId("send_right").contentType(nodeX).build();
                        send.basicPublish("", REGISTRY_QUEUE, sendRightProps, userMessage.getBytes("UTF-8"));
                        break;

                    case "help":
                        showMenu();
                        break;

                    case "exit":
                        connected = false;
                        break;

                    default:
                        System.out.println("The command typed is not available");
                        break;
                }
            }

            System.out.println("Shutting down. Good bye!");
            System.exit(1);
        }
    }

    private static void showMenu() {
        System.out.println("Available commands");
        System.out.println("list : Obtain list of nodes");
        System.out.println("connect [nodeX] [nodeY] : Connect Node X with Node Y in the virtual ring");
        System.out.println("disconnect [nodeX] [nodeY] : Disconnect Node X of Node Y in the virtual ring");
        System.out.println("show_topology : Show topology of the of the network (Physical Layer)");
        System.out.println("show_topology_overlay : Show topology of the overlaying (Logic Layer = Virtual Ring)");
        System.out.println("send [nodeX] [nodeY] [message] : Send a message from Node X to Node Y");
        System.out.println("send_left [nodeX] [message]: Send Left from Node X");
        System.out.println("send_right [nodeX] [message]: Send Right from Node X");
        System.out.println("help : To see the commands available");
        System.out.println("exit : To exit the programm");
    }

}