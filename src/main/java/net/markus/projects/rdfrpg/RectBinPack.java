package net.markus.projects.rdfrpg;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.function.Function;
import javax.imageio.ImageIO;

/**
 *
 * @author Markus Schr&ouml;der
 */
public class RectBinPack {
    
    public static void main(String[] args) {
        
        //random test data
        Random rnd = new Random(1234);
        List<Dimension> input = new LinkedList<>();
        for(int i = 0; i < 100; i++) {
            input.add(new Dimension(1 + rnd.nextInt(100), 1 + rnd.nextInt(100)));
        }
        
        Result<Dimension> result =
        run(
                input, 
                d -> d
        );
        
        //System.out.println(result.minD);
        //System.out.println(result.as);
        
        
        saveImage(result);
        
    }
    
    public static <T> void saveImage(Result<T> result) {
        BufferedImage img = new BufferedImage(result.minD.width, result.minD.height, BufferedImage.TYPE_INT_ARGB);
        
        Graphics g = img.createGraphics();
        g.setColor(Color.white);
        g.fillRect(0, 0, img.getWidth(), img.getHeight());
        
        int c = 0;
        List<Color> cs = Arrays.asList(Color.blue, Color.green, Color.gray, Color.orange, Color.red, Color.yellow);
        
        for(A a : result.as) {
            g.setColor(cs.get(c % cs.size()));
            c++;
            
            System.out.println(a.p + " " + a.d);
            g.fillRect(a.p.x, a.p.y, a.d.width, a.d.height);
        }
        g.dispose();
        
        try {
            ImageIO.write(img, "png", new File("rectbintest.png"));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    public static <T> Result<T> run(List<T> l, Function<T, Dimension> f) {
        
        //https://en.wikipedia.org/wiki/Bin_packing_problem
        
        List<A<T>> as = new LinkedList<>();
        
        for(T t : l) {
            as.add(new A(t, f.apply(t)));
        }
        
        //first the largest areas
        as.sort((o1, o2) -> {
            return Integer.compare(o2.area(), o1.area());
        });
        
        Dimension minD = new Dimension();
        
        Result<T> result = new Result<>();
        
        while(!as.isEmpty()) {
            
            A<T> a = as.get(0);
            as.remove(a);
            
            //place it
            a.p = new Point(0,0);
            
            //try to place it on a free place
            boolean placed = false;
            
            for(int y = 0; y < minD.height - a.d.height; y++) {
                for(int x = 0; x < minD.width - a.d.width; x++) {

                    //move it
                    a.p.x = x;
                    a.p.y = y;
                    
                    if(!intersects(a, result.as)) {
                        placed = true;
                        break;
                    }
                }
                
                if(placed)
                    break;
            }
            
            if(!placed) {
                //have to extend the area
                a.p = new Point(0,0);
                
                if(minD.width < minD.height) {
                    //on the right
                    a.p.x = minD.width;
                    minD.height = Math.max(minD.height, a.d.height);
                    minD.width += a.d.width;
                } else {
                    //on the bottom
                    a.p.y = minD.height;
                    minD.width = Math.max(minD.width, a.d.width);
                    minD.height += a.d.height;
                }
            }
            
            //put placed point
            result.as.add(a);
        }
        
        result.minD = minD;
        
        return result;
    } 
    
    private static <T> boolean intersects(A<T> a, List<A<T>> l) {
        for(A other : l) {
            if(new Rectangle(a.p, a.d).intersects(new Rectangle(other.p, other.d))) {
                return true;
            }
        }
        return false;
    }
    
    //class
    
    public static class Result<T> {
        Dimension minD;
        List<A<T>> as;
        
        public Result() {
            as = new LinkedList<>();
        }
    }
    
    public static class A<T> {
        T t;
        Point p;
        Dimension d;

        public A(T t, Dimension d) {
            this.t = t;
            this.d = d;
        }
        
        public int area() {
            return d.width * d.height;
        }
        
        
    }
}
