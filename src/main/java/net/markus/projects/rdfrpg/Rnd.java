package net.markus.projects.rdfrpg;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 *
 * @author Markus Schr&ouml;der
 */
public class Rnd {
    
    public static Random rnd;
    
    public static <T> T  randomlyRemove(List<T> list, Random rnd) {
        T t = Rnd.randomlySelect(list, rnd);
        list.remove(t);
        return t;
    }
    
    public static <T> T randomlySelect(List<T> list, Random rnd) {
        return list.get(rnd.nextInt(list.size()));
    }
    
    public static <T> T randomlySelect(float[] props, T[] ts, Random rnd) {
        float f = rnd.nextFloat();
        float acc = 0;
        for (int i = 0; i < props.length; i++) {
            float diff = acc + props[i];

            if (acc <= f && f <= diff) {
                return ts[i];
            }

            acc = diff;
        }
        return ts[0];
    }

    public static <T> T randomlySelect(List<T> list) {
        return list.get(rnd.nextInt(list.size()));
    }

    public static double randomG() {
        double g = rnd.nextGaussian();
        if (g > 1.0) {
            g = 1.0;
        }
        if (g < -1.0) {
            g = -1.0;
        }
        return g;
    }

    public static long randomLong(long mean, long sd) {
        return mean + (long) (randomG() * (double) sd);
    }

    public static int random(int mean, int sd) {
        return mean + (int) (randomG() * (double) sd);
    }

    public static Direction randomDirection() {
        return randomlySelect(Arrays.asList(Direction.values()));
    }
    
    public static int randomInt(int beginIncl, int endExcl, Random rnd) {
        int l = endExcl - beginIncl;
        return beginIncl + rnd.nextInt(l);
    }
}
