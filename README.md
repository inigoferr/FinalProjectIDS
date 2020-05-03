# FinalProjectIDS

### Commands for JAR

Node:
jar cfm node.jar manifest_node.txt Node.class Node$1.class amqp-client-5.7.1.jar slf4j-api-1.7.26.jar slf4j-simple-1.7.26.jar

java -jar node.jar

Node_Registry:
jar cfm node_registry.jar manifest_node_registry.txt Node_Registry.class Node_Registry$1.class Dijkstra/Dijkstra.class Dijkstra/GraphPath.class Dijkstra/NodePath.class PhysicalTopology.class amqp-client-5.7.1.jar slf4j-api-1.7.26.jar slf4j-simple-1.7.26.jar

java -jar node_registry.jar

Overlay:
jar cfm overlay.jar manifest_overlay.txt Overlay.class Overlay$1.class Image/CreateImage.class Image/DrawImage.class Image/MyCanvas.class amqp-client-5.7.1.jar slf4j-api-1.7.26.jar slf4j-simple-1.7.26.jar

java -jar overlay.jar