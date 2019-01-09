package net.markus.projects.rdfrpg;

import java.awt.Point;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import net.markus.projects.rdfrpg.Path.Element;

/**
 *
 * @author Markus Schr&ouml;der
 */
public class Path extends LinkedList<Element> {
    
    public Path() {
        super();
    }
    
    public Path(Path copyFrom) {
        addAll(copyFrom);
    }
    
    public Path(Point start) {
        add(new Element(start));
    }
    
    public boolean isMoveable() {
        return this.size() >= 2;
    }
    
    public Direction nextDir() {
        return this.get(1).dir;
    }
    
    public Point nextStep() {
        return Direction.stepDir(nextDir());
    }
    
    public Point nextPoint() {
        return get(1).p;
    }
    
    public Field nextField(GameMap map) {
        Point p = get(1).p;
        return map.getField(p.x, p.y);
    }
    
    public static class Element {
        Direction dir;
        Point p;
        String endReason;
        Field end;
        
        public Element(Point p) {
            this.p = p;
        }

        public Element(Direction dir, Point p) {
            this.dir = dir;
            this.p = p;
        }

        public boolean isRoot() {
            return dir == null;
        }

        public boolean isEnd() {
            return endReason != null;
        }
        
        public boolean hasEnd() {
            return end != null;
        }
        
        @Override
        public String toString() {
            return (isRoot() ? "" : dir.toString()) + "(" + p.x + "," + p.y + ")" + (isEnd() ? endReason : "") + (hasEnd() ? "-" + end : "");
        }
        
    }
    
    public Set<Point> visited() {
        return this.stream().map(e -> e.p).collect(toSet());
    }
    
    public Path copy() {
        return new Path(this);
    }
    
    public Element last() {
        return this.getLast();
    }
    
    public Path withoutLast() {
        Path p = new Path(this);
        p.remove(p.last());
        return p;
    }
    
    public static List<Path> shortest(List<Path> paths, String endreason) {
        return paths.stream()
                .filter(p -> p.last().isEnd() && p.last().endReason.equals(endreason))
                .sorted((o1, o2) -> {
                    return Integer.compare(o1.size(), o2.size());
                }).collect(toList());
    }
    
}
