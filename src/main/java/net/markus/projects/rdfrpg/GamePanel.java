package net.markus.projects.rdfrpg;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Rectangle;
import java.util.Map;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

/**
 *
 * @author Markus Schr&ouml;der
 */
public class GamePanel extends JPanel {

    private Game game;
    private Map<String, Image> uri2img;

    public GamePanel(Game game) {
        this.game = game;
        //loadImages();
        setPreferredSize(new Dimension(game.map.w * game.map.size, game.map.h * game.map.size));
    }

    /*
    private void loadImages() {
        uri2img = new HashMap<>();
        loadImages("chipset", Resources.chipsets, uri2img);
        loadImages("charset", Resources.charsets, uri2img);
    }
    
    private void loadImages(String type, List<String> names, Map<String, Image> uri2img) {
        for (String name : names) {
            String res = "/"+type+"/" + name + ".png";
            String uri = "res:" + res;
            try {
                BufferedImage img = ImageIO.read(GamePanel.class.getResourceAsStream(res));
                Color c = new Color(img.getRGB(img.getWidth()-1, img.getHeight()-1));
                Image timg = makeColorTransparent(img, c);
                uri2img.put(uri, timg);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
    }
    */
    
    /*
    public static Image makeColorTransparent(Image im, final Color color) {
        ImageFilter filter = new RGBImageFilter() {
            // the color we are looking for... Alpha bits are set to opaque
            public int markerRGB = color.getRGB() | 0xFF000000;

            public final int filterRGB(int x, int y, int rgb) {
                if ((rgb | 0xFF000000) == markerRGB) {
                    // Mark the alpha bits as zero - transparent
                    return 0x00FFFFFF & rgb;
                } else {
                    // nothing to do
                    return rgb;
                }
            }
        };

        ImageProducer ip = new FilteredImageSource(im.getSource(), filter);
        return Toolkit.getDefaultToolkit().createImage(ip);
    }
*/
    @Override
    protected void paintComponent(Graphics g) {
        //super.paintComponent(g);

        long begin = System.currentTimeMillis();
        
        g.setColor(Color.black);
        g.fillRect(0, 0, getWidth(), getHeight());
        
        //view port
        JScrollPane scrollPane = (JScrollPane) this.getParent().getParent();
        int visY = scrollPane.getVerticalScrollBar().getModel().getValue();
        int visX = scrollPane.getHorizontalScrollBar().getModel().getValue();
        Rectangle visible = new Rectangle(visX, visY, scrollPane.getWidth(), scrollPane.getHeight());

        //draw
        GameMapDrawer.drawGameMap(game, game.map, visible, g);
        
        long end = System.currentTimeMillis();
        long dur = end - begin;
        System.out.println("paintComponent " + dur + " ms");
    }
}
