package Image;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
 
import javax.imageio.ImageIO;
import java.awt.geom.QuadCurve2D;
 
public class CreateImage {

    private static final int NODE_SIZE = 100;

    public void create(int[][] topology, String filename, int width, int height) throws IOException {
        int num_nodes = topology.length;
        int[][] coords = new int[num_nodes][2];
 
        // Constructs a BufferedImage of one of the predefined image types.
        BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
 
        // Create a graphics which can be used to draw into the buffered image
        Graphics2D g2d = bufferedImage.createGraphics();
 
        // fill all the image with white
        g2d.setColor(Color.white);
        g2d.fillRect(0, 0, width, height);
 
        // get the node centre coordinates
        g2d.setColor(Color.black);
        for (int i = 0; i < topology.length; i++) {
            int x = (int) (i*1.5*NODE_SIZE);
            int y = (int) ((i%2)*1.5*NODE_SIZE) + 50;
            coords[i][0]=x+NODE_SIZE/2;
            coords[i][1]=y+NODE_SIZE/2;
        }

        // create the links
        for (int i = 0; i < topology.length; i++) {
            for (int j = 0; j < topology.length; j++) {
                if (topology[i][j]==1) {
                    QuadCurve2D curve = new QuadCurve2D.Float(coords[i][0], coords[i][1], (coords[i][0]+coords[j][0])/2, (coords[i][0]+coords[j][0])/4, coords[j][0], coords[j][1]);
                    g2d.draw(curve);
                }   
            }
        }
        
        // blank out the lines inside the nodes
        for (int i = 0; i < topology.length; i++) {
            int x = (int) (i*1.5*NODE_SIZE);
            int y = (int) ((i%2)*1.5*NODE_SIZE) + 50;
            g2d.setColor(Color.white);
            g2d.fillOval(x, y, NODE_SIZE, NODE_SIZE);
            g2d.setColor(Color.black);
            g2d.drawOval(x, y, NODE_SIZE, NODE_SIZE);
        }

        // add the node ids
        g2d.setColor(Color.black);
        for (int i = 0; i < topology.length; i++) {
            int id = i;
            id++;
            String nodeId = "node" + id;
            g2d.drawString(nodeId, coords[i][0]-15, coords[i][1]);
        }
        
 
        // Disposes of this graphics context and releases any system resources that it is using. 
        g2d.dispose();
 
        // Save as JPEG
        File file = new File(filename);
        ImageIO.write(bufferedImage, "jpg", file);
 
    }

    // public static void main(String[] args) throws IOException {
    //     PhysicalTopology physicalTopology = new PhysicalTopology();
    //     int[][] topology = physicalTopology.getTopology_1();

    //     int width = 1000;
    //     int height = 500;

    //     CreateImage creator = new CreateImage();
    //     creator.create(topology,width,height);
    
    //     DrawImage drawer = new DrawImage();
    //     drawer.draw("topology.jpg",width,height);
    // }
 
}