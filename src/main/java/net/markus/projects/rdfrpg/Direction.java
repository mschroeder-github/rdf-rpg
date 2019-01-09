package net.markus.projects.rdfrpg;

import java.awt.Point;

/**
 *
 * @author Markus Schr&ouml;der
 */
public enum Direction {
    Up,
    Right,
    Down,
    Left
    
    ;
    
    public static Direction opposite(Direction d) {
        switch(d) {
            case Up: return Down;
            case Down: return Up;
            case Left: return Right;
            case Right: return Left;
        }
        return null;
    }
    
    public static Axis axis(Direction d) {
        switch(d) {
            case Up: return Axis.Horizontal;
            case Down: return Axis.Horizontal;
            case Left: return Axis.Vertical;
            case Right: return Axis.Vertical;
        }
        return null;
    }
    
    public static int step(Direction d) {
        switch(d) {
            case Up: return -1;
            case Down: return 1;
            case Left: return -1;
            case Right: return 1;
        }
        return 0;
    }
    
    public static Point stepDir(Direction d) {
        switch(d) {
            case Up:    return new Point(0, step(d));
            case Down:  return new Point(0, step(d));
            case Left:  return new Point(step(d), 0);
            case Right: return new Point(step(d), 0);
        }
        return null;
    }
    
    public static Direction from(int srcx, int srcy, int dstx, int dsty) {
        if(srcy < dsty) {
            return Direction.Down;
        } else if(srcy > dsty) {
            return Direction.Up;
        } else if(srcx < dstx) {
            return Direction.Right;
        } else if(srcx > dstx) {
            return Direction.Left;
        }
        return null;
    }
}
