import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Toolkit;

import javax.swing.JComponent;
import javax.swing.JFrame;

class MyCanvas extends JComponent {

  private static final long serialVersionUID = 1L;
  private String image;

  public MyCanvas(String image) {
    this.image = image;
  }

  public void paint(Graphics g) {
    Graphics2D g2 = (Graphics2D) g;

    Image img1 = Toolkit.getDefaultToolkit().getImage(image);
    g2.drawImage(img1, 10, 10, this);
  }
}

public class DrawImage {
  public void draw(String image, int width, int height) {
    JFrame window = new JFrame(image);
    window.setBounds(30, 30, width, height);
    window.getContentPane().add(new MyCanvas(image));
    window.setVisible(true);
  }
}