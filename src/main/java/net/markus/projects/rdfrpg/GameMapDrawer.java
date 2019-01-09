package net.markus.projects.rdfrpg;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageFilter;
import java.awt.image.ImageProducer;
import java.awt.image.RGBImageFilter;
import java.io.IOException;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.imageio.ImageIO;
import net.markus.projects.rdfrpg.tools.CharsetFrame;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDFS;

/**
 *
 * @author Markus Schr&ouml;der
 */
public class GameMapDrawer {
    
    private static Map<String, Image> uri2img;
    private static Map<String, Image> uri2imgDyed;
    
    private static final boolean drawRoof = true;
    
    static {
        loadImages();
    }
    
    private static void loadImages() {
        uri2img = new HashMap<>();
        uri2imgDyed = new HashMap<>();
        loadImages("chipset", Resources.chipsets, uri2img);
        loadImages("charset", Resources.charsets, uri2img);
    }
    
    private static void loadImages(String type, List<String> names, Map<String, Image> uri2img) {
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

    private static Image makeColorTransparent(Image im, final Color color) {
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

    private static void loadDyedImage(Game game) {
        
        if(game != null) {
            LocalTime localTime = game.localTime();
            
            //6:00 - 20:00 = 14 (hell)
            //20:00 - 6:00 = 10 (dunkel)
            boolean night = localTime.getHour() < 5 || localTime.getHour() >= 20;
            int r = 255, g = 255, b = 255, a = 0;
            
            if(night) {
                //dark if night
                r = g = b = 20;
                a = 128;
            } else if(localTime.getHour() == 5) {
                //from dark to morning sun
                r = g = b = 20;
                a = 128;
                
                float min = localTime.getMinute() / 60.0f;
                
                r = 20 + (int) ((255-20) * min);
                g = 20 + (int) ((128-20) * min);
                b = 20 - (int) (20 * min);
                
            } else if(localTime.getHour() == 6) {
                //sun rise
                
                float min = localTime.getMinute() / 60.0f;
                
                r = 255;
                g = 128 + (int) (40 * min);
                b = 0;
                
                a = 128 - (int) (128 * min);
                
            } else if(localTime.getHour() == 19) {
                //sun goes down
                
                r = 180;
                g = 20;
                b = 20;
                
                float min = localTime.getMinute() / 60.0f;
                
                r = 180 - (int)(60 * min);
                a = (int) (128 * min);
                
            } else {
                //day light
                r = 255;
                g = 255;
                b = 255;
                a = 0;
            }
            
            System.out.println(Arrays.asList(r,g,b,a));
            
            for(String key : uri2img.keySet()) {
                uri2imgDyed.put(key, dye(uri2img.get(key), new Color(r,g,b,a)));
            }
        } else {
            //copy
            for(String key : uri2img.keySet()) {
                uri2imgDyed.put(key, uri2img.get(key));
            }
        }
    }
    
    //==============================
    
    public static void drawGameMap(Game game, GameMap map, Rectangle offsetViewportCoord, Graphics g) {
        
        //dye all chips based on time if game is available
        loadDyedImage(game);
        
        //TODO use in draw* method the dyed image if not in room
        
        //g.setColor(Color.black);
        //g.fillRect(0, 0, offsetViewportCoord.width, offsetViewportCoord.height);
        
        //only fields that are visible
        List<Field> fields = map.getFieldsCoord(offsetViewportCoord);

        //room
        Field atField;
        Set<Resource> inRoom = new HashSet<>();
        if(map.in(map.cursor.x, map.cursor.y)) {
            atField = map.getField(map.cursor.x, map.cursor.y);
            if(atField.hasRoom()) {
                inRoom.addAll(atField.rooms);
            }
        }
        
        
        //System.out.println(fields.size());
        for (Field f : fields) {
            if (f.ground != null) {
                drawThing(f, f.ground, inRoom, "t:ground", false, game, map, g);
            }
        }

        for (Field f : fields) {
            if (f.lying != null) {
                drawThing(f, f.lying, inRoom, "t:lying", false, game, map, g);
            }
        }

        for (Field f : fields) {
            if (f.obstacle != null) {
                drawThing(f, f.obstacle, inRoom, "t:obstacle", false, game, map, g);
            }
            if (f.lying != null && TBox.model.contains(f.lying.type, RDFS.subClassOf, TBox.door)) {
                drawThing(f, f.lying, inRoom, "t:lying", false, game, map, g);
            }
        }

        for (Field f : fields) {
            if (f.hovering != null) {
                drawThing(f, f.hovering, inRoom, "t:hovering", true, game, map, g);
            }
        }
        
        Set<Field> surrounding = map.getSurroundingSet(map.cursor.x, map.cursor.y);
        
        //System.out.println(surrounding);
        
        //roof
        if(drawRoof) {
            for (Field f : fields) {
                if (!f.reveal(inRoom)) {

                    if(surrounding.contains(f)) {
                        blend(0.25f, g);
                    } else {
                        blend(1.0f, g);
                    }

                    drawChip("res:/chipset/Basis.png", 13, 15, f.x, f.y-1, map, true, g);
                }
            }
        }
        
        blend(1.0f, g);
        
        //cursor
        g.setColor(Color.red);
        g.drawRect(map.cursor.x * map.size, map.cursor.y * map.size, map.size, map.size);
    }
    
    private static void drawThing(Field field, Thing thing, Set<Resource> inRoom, String layerUri, boolean up, Game game, GameMap map, Graphics g) {

        boolean dyed = !field.hasRoom();
        
        //does asking tbox take long?
        if (!TBox.model.containsResource(thing.type)) {
            throw new RuntimeException("drawThing: " + thing.type + " not found");
        }

        if (!TBox.model.contains(thing.type, TBox.resource)) {
            throw new RuntimeException("drawThing: " + thing.type + " has not t:resouce");
        }

        //get resource and where it is exactly
        String resourceUri = TBox.model.listObjectsOfProperty(thing.type, TBox.resource).toList().get(0).asResource().getURI();
        String[] chipsetLocationArray = TBox.model.listObjectsOfProperty(thing.type, TBox.resourceLocation).toList().get(0).asLiteral().getLexicalForm().split(",");
        int fromX = Integer.parseInt(chipsetLocationArray[0]);
        int fromY = Integer.parseInt(chipsetLocationArray[1]);

        //based on type we draw differently
        if (TBox.model.contains(thing.type, RDFS.subClassOf, TBox.tchip)) {
            
            //hide the wall hovering
            
            //for all hovering walls
            if(TBox.model.contains(thing.type, RDFS.subClassOf, TBox.wall) &&
               layerUri.equals("t:hovering")) {
                
                //if we have to reveal the room
                //and it is above the player
                if(field.reveal(inRoom) &&
                   field.y > map.cursor.y) {
                    
                    //do not draw
                    return;
                }
                
                if(field.y == map.cursor.y+1) {
                    //do not draw
                    return;
                }
            } 
            
            drawChip(resourceUri, fromX, fromY, field.x, field.y + (up ? -1 : 0), map, dyed, g);

            //extra someting on top
            if (TBox.model.contains(thing.type, RDFS.subClassOf, TBox.chiphigh)) {
                drawChip(resourceUri, fromX, fromY - 1, field.x, field.y - 1, map, dyed, g);
            }

        } else if (TBox.model.contains(thing.type, RDFS.subClassOf, TBox.tchar)) {
            drawChar(resourceUri, thing.direction, fromX, fromY, field.x, field.y, map, dyed, g);
        }
    }

    private static void drawChip(String resourceUri, int fromX, int fromY, int toX, int toY, GameMap map, boolean dyed, Graphics g) {
        
        Point to = new Point(
                toX * map.size, toY * map.size
        );
        
        Point too = new Point(
                (toX * map.size) + map.size, (toY * map.size) + map.size
        );
        
        Rectangle rect = new Rectangle(to.x, to.y, too.x - to.x, too.y - to.y);
        
        Map<String, Image> source = dyed ? uri2imgDyed : uri2img;
        
        int s = 16;
        g.drawImage(source.get(resourceUri),
                to.x, to.y, too.x, too.y,
                fromX * s, fromY * s, (fromX * s) + s, (fromY * s) + s,
                null);
    }
    
    private static void drawChar(String resourceUri, Direction direction, int fromX, int fromY, int toX, int toY, GameMap map, boolean dyed, Graphics g) {
        int w = CharsetFrame.charsetSize.width;
        int h = CharsetFrame.charsetSize.height;

        int cw = CharsetFrame.charSize.width;
        int ch = CharsetFrame.charSize.height;

        Map<String, Image> source = dyed ? uri2imgDyed : uri2img;
        
        g.drawImage(source.get(resourceUri),
                (toX * map.size) - 8, (toY * map.size) - 32, ((toX * map.size) - 8) + cw * 2, ((toY * map.size) - 32) + ch * 2,
                (fromX * w) + cw, (fromY * h) + (ch * direction.ordinal()), ((fromX * w) + cw) + cw, ((fromY * h) + (ch * direction.ordinal())) + ch,
                null);
    }

    private static void blend(float alpha, Graphics g) {
        Graphics2D g2 = (Graphics2D)g;
        Composite comp = AlphaComposite.getInstance(AlphaComposite.SRC_OVER , alpha );
        g2.setComposite(comp );
    }
    
    private static BufferedImage dye(Image image, Color color) {
        int w = image.getWidth(null);
        int h = image.getHeight(null);
        BufferedImage dyed = new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = dyed.createGraphics();
        g.drawImage(image, 0,0, null);
        g.setComposite(AlphaComposite.SrcAtop);
        g.setColor(color);
        g.fillRect(0,0,w,h);
        g.dispose();
        return dyed;
    }

    //can not be used: creates not a alpha
    @Deprecated
    private static void dye(Color c, Rectangle rect, Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        Composite comp = g2.getComposite();
        g2.setComposite(AlphaComposite.SrcAtop);
        g2.setColor(c);
        g2.fillRect(rect.x,rect.y,rect.width,rect.height);
        g2.setComposite(comp);
    }
    
}
