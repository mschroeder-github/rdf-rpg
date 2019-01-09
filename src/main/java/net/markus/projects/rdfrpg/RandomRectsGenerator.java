package net.markus.projects.rdfrpg;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import static java.util.stream.Collectors.toList;
import org.apache.jena.rdf.model.Resource;

/**
 *
 * @author Markus Schr&ouml;der
 */
public class RandomRectsGenerator {

    public boolean debug;

    public RandomRects generate(List<Point> seedRects, Dimension maxBounds, Dimension maxRectSize, int maxRectDiff, int maxSteps, Random rnd) {
        RandomRects rr = new RandomRects();

        int rectIndex = 0;
        for(Point p : seedRects) {
            Rect r = new Rect();
            r.rect = new Rectangle(p, new Dimension(1, 1));

            r.c = String.valueOf(rectIndex++).charAt(0);
            
            rr.rects.add(r);
        }
        
        rr.moveToZeroPoint();
        
        if(debug) {
            rr.print();
        }

        boolean stop = false;

        //for some steps
        for (int i = 0; i < maxSteps; i++) {
            
            int continueCount = 0;

            for (Rect cur : rr.rects) {

                //no resize allowed
                if (cur.dirs.isEmpty()) {
                    continueCount++;
                    continue;
                }

                /*
                double otherAreaAvg = rr.rects.stream()
                        .filter(r -> r != cur)
                        .mapToInt(r -> r.area())
                        .average().getAsDouble();
                
                int otherAreaSum = rr.rects.stream()
                        .filter(r -> r != cur)
                        .mapToInt(r -> r.area())
                        .sum();
                
                if(cur.area() > otherAreaSum) {
                    continue;
                }
                 */
                //grow randomly idea
                Direction d = randomlySelect(cur.dirs, rnd);

                Rect tmp;
                //if(rnd.nextInt(2) == 0) {
                tmp = resize(cur, d);
                //} else {
                //    tmp = move(cur, d);
                //}

                //check if it is rectangular enough
                int diff = Math.abs(tmp.rect.width - tmp.rect.height);
                if (diff > maxRectDiff) {
                    continueCount++;
                    continue;
                }

                //check if the rectangle is in size
                if (tmp.rect.width > maxRectSize.width || 
                    tmp.rect.height > maxRectSize.height) {
                    continueCount++;
                    continue;
                }

                //list with tmp in it
                List<Rect> tmpList = new LinkedList<>(rr.rects);
                tmpList.remove(cur);
                tmpList.add(tmp);

                //not greater than max bounds
                Rectangle bb = RandomRects.bb(tmpList);
                if (bb.width > maxBounds.width
                        || bb.height > maxBounds.height) {
                    continueCount++;
                    continue;
                }

                //check if the changed rect intersects with another rect
                List<Rect> otherList = new LinkedList<>(rr.rects);
                otherList.remove(cur);
                for (Rect other : otherList) {
                    if (tmp.rect.intersects(other.rect)) {
                        //intersection = true;
                        postIntersection(cur, other, d, rr);
                    }
                }

                //update the change
                cur.rect.x = tmp.rect.x;
                cur.rect.y = tmp.rect.y;
                cur.rect.width = tmp.rect.width;
                cur.rect.height = tmp.rect.height;

                //calculate area to stop when enough room is made
                //float areaSum = rr.rects.stream().mapToInt(r -> r.rect.width * r.rect.height).sum();
                //float bbArea = rr.bbArea();
                //float filled = areaSum / bbArea;
                
                rr.moveToZeroPoint();
                rr.calculateDoors();

                //if every rect has min one door: stop algorithm
                //Set<Rect> rects = rr.intersections.stream().flatMap(inter -> inter.rects.stream()).collect(toSet());
                //System.out.println(rects);
                
                /*
                if (rr.intersections.size() > seedRects.size() - 1) {
                    boolean allHaveDoors = rr.intersections.stream().allMatch(inter -> !inter.doors.isEmpty());
                    if (allHaveDoors) {
                        stop = true;
                        rr.valid = true;
                        break;
                    }
                }*/
                
                if(allRectsReachableWithDoors(rr)) {
                    stop = true;
                    rr.valid = true;
                    break;
                }
                
            }//for all rects

            if (debug) {
                rr.print();
            }
            
            //if only continued
            if(continueCount == rr.rects.size()) {
                break;
            }

            if (stop) {
                break;
            }
        }

        return rr;
    }

    public RandomRects generateValid(List<Point> seedRects, Dimension maxBounds, Dimension maxRectSize, int maxRectDiff, int maxSteps, Random rnd) {
        RandomRects rr;
        do {
            rr = generate(seedRects, maxBounds, maxRectSize, maxRectDiff, maxSteps, rnd);
        } while(!rr.valid);
        return rr;
    }
    
    private boolean allRectsReachableWithDoors(RandomRects rr) {
        Rect rect = rr.rects.get(0);
        
        Queue<Rect> q = new LinkedList<>();
        Set<Rect> visited = new HashSet<>();
        
        q.add(rect);
        
        while(!q.isEmpty()) {
            Rect r = q.poll();
            
            visited.add(r);
            
            //add all rects from the intersections
            for(RectIntersection inter : rr.intersections) {
                if(inter.rects.contains(r)) {
                    for(Rect child : inter.rects) {
                        if(!visited.contains(child)) {
                            q.add(child);
                        }
                    }
                }
            }
        }
        
        return visited.containsAll(rr.rects) && //we could visit all
               rr.intersections.stream().allMatch(inter -> !inter.doors.isEmpty()); //all have at least one door
    }
    
    @Deprecated
    private void seedRectsV1(RandomRects rr, Dimension maxBounds, Dimension maxRectSize, int numRect, Random rnd) {
        //seed rects
        Point rectPoint = randomPoint(maxBounds.width, maxBounds.height, rnd);
        for (int i = 0; i < numRect; i++) {
            Rectangle realRect = new Rectangle(rectPoint, new Dimension(1, 1));
            Rect r = new Rect();
            r.rect = realRect;
            rr.rects.add(r);

            rectPoint.x += (-1 * maxRectSize.width / 2) + rnd.nextInt(maxRectSize.width / 4);
            rectPoint.y += (-1 * maxRectSize.height / 2) + rnd.nextInt(maxRectSize.height / 4);
        }
    }

    public static List<Point> seedRectsV2(Axis axis, int numRect, Dimension maxBounds) {
        List<Point> points = new LinkedList<>();
        
        int axisLen = axis == Axis.Horizontal ? maxBounds.width : maxBounds.height;

        // | | |
        // | | | |
        int step = axisLen / (numRect + 1);

        int cur = step;
        for (int i = 0; i < numRect; i++) {

            Point p;
            if (axis == Axis.Horizontal) {
                p = new Point(cur, maxBounds.height / 2);
            } else {
                p = new Point(maxBounds.width / 2, cur);
            }
            cur += step;

            points.add(p);
        }
        return points;
    }

    public static List<Point> seedRectsV3(int numRect, Dimension maxBounds) {
        int numRectHalf = numRect / 2;
        int sizeW = maxBounds.width / (numRectHalf+1);
        int sizeH = maxBounds.height / (numRectHalf+1);
        
        List<Point> ps = new LinkedList<>();
        for(int i = 0; i < numRectHalf; i++) {
            for(int j = 0; j < numRectHalf; j++) {
                ps.add(new Point(j*sizeH, i*sizeW));
                
                if(ps.size() == numRect)
                    break;
            }
            
            if(ps.size() == numRect)
                break;
        }
        return ps;
    } 
    
    public static List<Point> seedRectsTriangle(int missing, Dimension maxBounds) {
        List<Point> points = new LinkedList<>();
        
        int stepX = maxBounds.width / 4;
        int stepY = maxBounds.height / 4;
        
        for(int i = 0; i < 2; i++) {
            for(int j = 0; j < 2; j++) {
                int k = j+i*2;
                if(k != missing) {
                    points.add(new Point(stepX*(j+1), stepY*(i+1)));
                }
            }
        }
        
        return points;
    }
    
    private void postIntersection(Rect cur, Rect other, Direction d, RandomRects rr) {
        //to know what are the intersectors in the direction
        //if(cur.intersectors.get(d).contains(other) ||
        //   other.intersectors.get(Direction.opposite(d)).contains(cur)) {
        //    return;
        //}
        cur.intersectors.get(d).add(other);
        other.intersectors.get(Direction.opposite(d)).add(cur);

        //disallow growing in intersection direction
        cur.dirs.remove(d);
        other.dirs.remove(Direction.opposite(d));

        //no duplicate intersections
        boolean found = false;
        for (RectIntersection inter : rr.intersections) {
            if (inter.rects.contains(cur) && inter.rects.contains(other)) {
                found = true;
                break;
            }
        }

        if (!found) {
            //for the doors later
            RectIntersection ri = new RectIntersection(cur, other, Direction.axis(d));
            rr.intersections.add(ri);
            cur.interList.add(ri);
            other.interList.add(ri);
        }
    }

    private Rect resize(Rect rect, Direction d) {
        //a temp rectangle to change
        Rect tmp = new Rect();
        tmp.rect = new Rectangle(rect.rect);

        switch (d) {
            case Down:
                tmp.rect.height = tmp.rect.height + 1;
                break;
            case Right:
                tmp.rect.width = tmp.rect.width + 1;
                break;
            case Left:
                tmp.rect.x = tmp.rect.x - 1;
                tmp.rect.width = tmp.rect.width + 1;
                break;
            case Up:
                tmp.rect.y = tmp.rect.y - 1;
                tmp.rect.height = tmp.rect.height + 1;
                break;
        }

        return tmp;
    }

    private Rect move(Rect rect, Direction d) {
        //a temp rectangle to change
        Rect tmp = new Rect();
        tmp.rect = new Rectangle(rect.rect);

        switch (d) {
            case Down:
                tmp.rect.y = tmp.rect.y + 1;
                break;
            case Right:
                tmp.rect.x = tmp.rect.x + 1;
                break;
            case Left:
                tmp.rect.x = tmp.rect.x - 1;
                break;
            case Up:
                tmp.rect.y = tmp.rect.y - 1;
                break;
        }

        return tmp;
    }

    private <T> T randomlySelect(List<T> list, Random rnd) {
        return list.get(rnd.nextInt(list.size()));
    }

    private Point randomPoint(int w, int h, Random rnd) {
        return new Point(rnd.nextInt(w), rnd.nextInt(h));
    }

    public static class RandomRects {

        List<Rect> rects;
        List<RectIntersection> intersections;
        boolean valid;

        public List<Rect> getByType(String typeUri) {
            return rects.stream().filter(r -> r.types.contains(typeUri)).collect(toList());
        }
        
        public boolean intersects(Point p) {
            return rects.stream().anyMatch(r -> r.rect.contains(p));
        }
        
        public RandomRects() {
            rects = new ArrayList<>();
            intersections = new ArrayList<>();
        }

        public static Rectangle bb(List<Rect> givenRects) {
            Rectangle bb = new Rectangle();
            for (Rect r : givenRects) {
                bb.x = Math.min(r.rect.x, bb.x);
                bb.y = Math.min(r.rect.y, bb.y);
            }
            for (Rect r : givenRects) {
                bb.width = Math.max(bb.x + (r.rect.x - bb.x) + r.rect.width, bb.width);
                bb.height = Math.max(bb.y + (r.rect.y - bb.y) + r.rect.height, bb.height);
            }
            return bb;
        }

        public Rectangle bb() {
            return bb(this.rects);
        }

        public float bbArea() {
            Rectangle bb = bb();
            return bb.width * bb.height;
        }

        public void moveToZeroPoint() {
            int offsetX = Integer.MAX_VALUE;
            int offsetY = Integer.MAX_VALUE;

            for (Rect r : rects) {
                if (r.rect.x < offsetX) {
                    offsetX = r.rect.x;
                }
                if (r.rect.y < offsetY) {
                    offsetY = r.rect.y;
                }
            }

            if (offsetX == 0 && offsetY == 0) {
                return;
            }

            int ox = offsetX * -1;
            int oy = offsetY * -1;
            for (Rect r : rects) {
                r.rect.x += ox;
                r.rect.y += oy;
                intersections.stream().flatMap(i -> i.doors.stream()).forEach(
                        p -> {
                            p.x += ox;
                            p.y += oy;
                        }
                );
            }
        }

        private void calculateDoors() {
            for (RectIntersection intersection : intersections) {

                Rectangle ar = intersection.a.rect;
                Rectangle br = intersection.b.rect;

                if (intersection.axis == Axis.Horizontal) {
                    int x1 = Math.max(ar.x + 1, br.x + 1);
                    int x2 = Math.min((int) ar.getMaxX() - 2, (int) br.getMaxX() - 2);

                    int y;
                    if (ar.y < br.y) {
                        y = br.y;
                    } else {
                        y = ar.y;
                    }

                    intersection.doors.clear();
                    for (int i = x1; i <= x2; i++) {
                        intersection.doors.add(new Point(i, y));
                    }
                } else if (intersection.axis == Axis.Vertical) {
                    int y1 = Math.max(ar.y + 1, br.y + 1);
                    int y2 = Math.min((int) ar.getMaxY() - 2, (int) br.getMaxY() - 2);

                    int x;
                    if (ar.x < br.x) {
                        x = br.x;
                    } else {
                        x = ar.x;
                    }

                    intersection.doors.clear();
                    for (int i = y1; i <= y2; i++) {
                        intersection.doors.add(new Point(x, i));
                    }
                }
            }
        }

        public void print() {
            Rectangle bb = bb();

            char[][] matrix = new char[bb.height][bb.width];

            for (int i = 0; i < matrix.length; i++) {
                for (int j = 0; j < matrix[i].length; j++) {
                    matrix[i][j] = ' ';
                }
            }

            int rectIndex = 0;
            for (Rect r : rects) {
                for (int i = r.rect.y; i < r.rect.y + r.rect.height; i++) {
                    for (int j = r.rect.x; j < r.rect.x + r.rect.width; j++) {
                        matrix[i][j] = r.c;
                    }
                }
                rectIndex++;
            }

            char doorChar = '▯';
            for (List<Point> doors : intersections.stream().map(i -> i.doors).collect(toList())) {
                for (Point door : doors) {
                    matrix[door.y][door.x] = doorChar;
                }
            }

            for (int i = 0; i < matrix.length; i++) {
                for (int j = 0; j < matrix[i].length; j++) {
                    System.out.print(matrix[i][j]);
                    if (j == matrix[i].length - 1) {
                        System.out.print("|" + (i % 10));
                    }
                }
                System.out.println();

                if (i == matrix.length - 1) {
                    for (int k = 0; k < bb.width + 1; k++) {
                        System.out.print("-");
                    }
                    System.out.println();
                    for (int k = 0; k < bb.width; k++) {
                        System.out.print(k % 10);
                    }
                    System.out.println();
                }
            }

            System.out.println();
        }
        
        public List<Rect> sortByArea() {
            return rects.stream().sorted((o1, o2) -> {
                return Integer.compare(o2.area(), o1.area());
            }).collect(toList());
        }
        
        public List<Rect> sortByIntersection() {
            return rects.stream().sorted((o1, o2) -> {
                int cmp = Integer.compare(o2.intersections(), o1.intersections());
                if(cmp == 0) {
                    return Integer.compare(o2.area(), o1.area());
                }
                return cmp;
            }).collect(toList());
        }
        
    }

    public class Rect {

        List<Direction> dirs;
        Rectangle rect;
        Map<Direction, Set<Rect>> intersectors;
        List<RectIntersection> interList;
        char c;
        List<String> types;
        Set<Resource> owners; //room owners (could be a couple)
        String uri; //used to mark the field with a room id

        public Rect() {
            dirs = new ArrayList<>(Arrays.asList(Direction.values()));
            intersectors = new HashMap<>();
            for (Direction dir : Direction.values()) {
                intersectors.put(dir, new HashSet<>());
            }
            interList = new LinkedList<>();
            types = new LinkedList<>();
            this.uri = "a:room-" + UUID.randomUUID().toString();
        }

        public int area() {
            return rect.width * rect.height;
        }

        public int intersections() {
            return interList.size();
        }
        
        @Override
        public String toString() {
            return rect.toString();
        }
        
        
        
    }

    public class RectIntersection {

        Set<Rect> rects;
        Rect a;
        Rect b;
        Axis axis;
        List<Point> doors;

        public RectIntersection(Rect a, Rect b, Axis intersectionAxis) {
            this.a = a;
            this.b = b;
            rects = new HashSet<>();
            rects.add(a);
            rects.add(b);
            this.axis = intersectionAxis;
            this.doors = new ArrayList<>();
        }
    }

}
