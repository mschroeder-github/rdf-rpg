package net.markus.projects.rdfrpg;

import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import static java.util.stream.Collectors.toSet;
import javax.imageio.ImageIO;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.vocabulary.RDFS;

/**
 *
 * @author Markus Schr&ouml;der
 */
public class GameMap {

    //if the GameMap is an object
    //this is used to store the type of it
    //this is helpful for horizontal and vertical objects
    public Resource type;

    //if it is a house we store here where the entrance was placed
    public Point entrance;

    public Set<Thing> reachables;

    //to focus on a field (by player)
    public Point cursor;
    
    //for the whole world, we save w and h separately
    public int w;
    public int h;

    public Field[][] fields;

    //size of field
    public final int size = 32;

    public static Set<Resource> walls;
    public static Set<Resource> chars;
    
    public GameMap(int w, int h) {
        this.w = w;
        this.h = h;
        fields = new Field[h][w];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                fields[y][x] = new Field(x, y);
            }
        }
        this.reachables = new HashSet<>();
        this.cursor = new Point();
        
        if(walls == null)
            walls = new HashSet<>(TBox.model.listResourcesWithProperty(RDFS.subClassOf, TBox.wall).toSet());
        
        if(chars == null)
            chars = new HashSet<>(TBox.model.listResourcesWithProperty(RDFS.subClassOf, TBox.tchar).toSet());
    }

    public static GameMap single(String type) {
        GameMap gm = new GameMap(1, 1);
        gm.draw(0, 0, withLayer(type));
        return gm;
    }

    public static GameMap multiHorizontal(String... types) {
        GameMap gm = new GameMap(types.length, 1);
        for (int i = 0; i < types.length; i++) {
            gm.draw(i, 0, withLayer(types[i]));
        }
        return gm;
    }

    public static GameMap multiVertical(String... types) {
        GameMap gm = new GameMap(1, types.length);
        for (int i = 0; i < types.length; i++) {
            gm.draw(0, i, withLayer(types[i]));
        }
        return gm;
    }

    public void draw(int x, int y, Consumer<Field> consumer) {
        fillRect(x, y, 1, 1, consumer);
    }

    public void drawRect(int x, int y, int w, int h, Consumer<Field> consumer) {
        drawLineH(x, y, w, consumer);
        drawLineV(x + w - 1, y, h, consumer);
        drawLineH(x, y + h - 1, w, consumer);
        drawLineV(x, y, h, consumer);
    }

    public void fillRect(int x, int y, int w, int h, Consumer<Field> consumer) {
        for (int i = y; i < y + h; i++) {
            for (int j = x; j < x + w; j++) {
                if (in(j, i)) {
                    consumer.accept(fields[i][j]);
                }
            }
        }
    }

    public void drawLineH(int x, int y, int w, Consumer<Field> consumer) {
        int begin = x;
        if (w < 0) {
            w = w * -1;
            begin = x - w;
        }
        for (int i = begin; i < begin + w; i++) {
            if (in(i, y)) {
                consumer.accept(fields[y][i]);
            }
        }
    }

    public void drawLineV(int x, int y, int h, Consumer<Field> consumer) {
        int begin = y;
        if (h < 0) {
            h = h * -1;
            begin = y - h;
        }
        for (int i = begin; i < begin + h; i++) {
            if (in(x, i)) {
                consumer.accept(fields[i][x]);
            }
        }
    }

    //drawer (pencils)
    public static Consumer<Field> wall(String type) {
        return (f) -> {
            f.hovering = new Thing(type);
            f.obstacle = new Thing(type);
        };
    }

    public static Consumer<Field> door(String type) {
        return (f) -> {
            f.hovering = null;
            f.obstacle = null;
            f.lying = new Thing(type);
        };
    }

    public static Consumer<Field> ground(String type) {
        return (f) -> {
            f.ground = new Thing(type);
        };
    }

    public static Consumer<Field> obstacle(String type) {
        return (f) -> {
            f.obstacle = new Thing(type);
        };
    }
    
    public static Consumer<Field> room(String roomUri) {
        Resource res = ResourceFactory.createResource(roomUri);
        return (f) -> {
            if(f.rooms == null) {
                f.rooms = new HashSet<>();
            }
            f.rooms.add(res);
        };
    }

    public static Consumer<Field> hovering(String type) {
        return (f) -> {
            f.hovering = new Thing(type);
        };
    }

    public static Consumer<Field> withLayer(String type) {
        return (f) -> {
            Resource r = ResourceFactory.createResource(type);

            if (TBox.model.contains(r, RDFS.subClassOf, TBox.hovering)) {
                f.hovering = new Thing(type);
            } else if (TBox.model.contains(r, RDFS.subClassOf, TBox.lying)) {
                f.lying = new Thing(type);
            } else if (TBox.model.contains(r, RDFS.subClassOf, TBox.ground)) {
                f.ground = new Thing(type);
            } else {
                f.obstacle = new Thing(type);
            }
        };
    }

    public static Consumer<Field> clearing() {
        return (f) -> {
            f.clear();
        };
    }

    public Predicate<Field> noDoor() {
        return f -> {
            List<Field> sides = getAllSides(f.x, f.y);
            //no side has a door -> field is ok
            return !sides.stream().anyMatch(ff
                    -> ff.has(TBox.lying)
                    && TBox.model.contains(ff.lying.type, RDFS.subClassOf, TBox.door)
            );
        };
    }
    
    @Deprecated
    public void test() {
        for (Field f : getFields()) {
            f.ground = new Thing("a:field-0-0", "t:shortGras");
        }
        /*
        fields[0][0].ground = new Thing("a:field-0-0", "t:longGras");
        
        fields[2][4].obstacle = new Thing("a:bla", "t:table");
        
        fields[2][5].obstacle = new Thing("a:uiae", "t:blueHairGuy");
        fields[2][5].obstacle.direction = Direction.Left;
        
        
        fields[3][5].obstacle = new Thing("a:uiae", "t:doorWood");
        
        fields[3][4].hovering = new Thing("a:uiae", "t:brickWallAncient");
        fields[3][4].obstacle = new Thing("a:uiae", "t:brickWallAncient");
        
        fields[3][6].hovering = new Thing("a:uiae", "t:brickWallAncient");
        fields[3][6].obstacle = new Thing("a:uiae", "t:brickWallAncient");
        
        fields[3][7].hovering = new Thing("a:uiae", "t:brickWallAncient");
        fields[3][7].obstacle = new Thing("a:uiae", "t:brickWallAncient");
         */

        drawRect(1, 1, 5, 6, wall("t:brickWallAncient"));

        drawLineH(1, 10, 5, wall("t:brickWallAncient"));

        //map[2][4].hovering = new Thing("a:bla", "t:table");
    }

    public void clear() {
        getFields().forEach(f -> f.clear());
    }

    public void paste(GameMap map, int x, int y) {
        for (int i = 0; i < map.h; i++) {
            for (int j = 0; j < map.w; j++) {
                if (in(x + j, y + i)) {
                    fields[y + i][x + j].paste(map.fields[i][j]);
                }
            }
        }
    }

    public void undo(GameMap map, int x, int y) {
        for (int i = 0; i < map.h; i++) {
            for (int j = 0; j < map.w; j++) {
                if (in(x + j, y + i)) {
                    fields[y + i][x + j].undo(map.fields[i][j]);
                }
            }
        }
    }

    public boolean in(int x, int y) {
        return x >= 0 && x < w && y >= 0 && y < h;
    }

    public Field getField(int x, int y) {
        return fields[y][x];
    }

    public List<Field> getFields() {
        List<Field> f = new ArrayList<>(w * h);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                f.add(fields[y][x]);
            }
        }
        return f;
    }
    
    public List<Field> getFields(Predicate<Field> p) {
        List<Field> f = new ArrayList<>(w * h);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if(p.test(fields[y][x])) {
                    f.add(fields[y][x]);
                }
            }
        }
        return f;
    }

    public List<Field> getFields(Rectangle rect) {
        List<Field> f = new ArrayList<>(rect.width * rect.height);
        for (int y = rect.y; y < rect.y + rect.height; y++) {
            for (int x = rect.x; x < rect.x + rect.width; x++) {
                f.add(fields[y][x]);
            }
        }
        return f;
    }

    public List<Field> getFieldsCoord(Rectangle srcCoords) {
        List<Field> f = new ArrayList<>();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                Field ff = fields[y][x];

                if (ff.x * size >= srcCoords.x - size
                        && ff.y * size >= srcCoords.y - size
                        && ff.x * size <= srcCoords.x + srcCoords.width
                        && ff.y * size <= srcCoords.y + srcCoords.height) {

                    f.add(ff);
                }
            }
        }
        return f;
    }

    public static void move(Field src, Field dst, Resource layer) {
        if(layer.equals(TBox.obstacle)) {
            dst.obstacle = src.obstacle;
            src.obstacle = null;
            dst.obstacle.direction = Direction.from(src.x, src.y, dst.x, dst.y);
        }
    }
    
    @Deprecated
    public List<Field> getFreeRoomFields(Rectangle roomRect, Predicate<Field> pred) {
        List<Field> f = new ArrayList<>();
        for (int y = roomRect.y + 1; y < roomRect.y + roomRect.height - 2; y++) {
            for (int x = roomRect.x + 1; x < roomRect.x + roomRect.width - 2; x++) {
                Field ff = fields[y][x];

                if (!ff.hasObstacle()) {
                    if (pred == null || pred.test(ff)) {
                        f.add(ff);
                    }
                }
            }
        }
        return f;
    }

    public List<Field> getFreeRoomFields(Rectangle roomRect, int fieldW, int fieldH, Resource layer, Predicate<Field> pred) {
        List<Field> f = new ArrayList<>();
        for (int y = roomRect.y + 1; y < roomRect.y + roomRect.height - 1; y++) {
            for (int x = roomRect.x + 1; x < roomRect.x + roomRect.width - 1; x++) {

                boolean found = false;
                List<Field> allFields = new LinkedList<>();

                //test all other fields
                for (int i = 0; i < fieldH; i++) {
                    for (int j = 0; j < fieldW; j++) {

                        int a = x + j;
                        int b = y + i;
                        if (in(a, b)) {
                            Field ff = fields[b][a];

                            if (layer == null) {
                                if (ff.hasNonGround()) {
                                    found = true;
                                }
                            } else {
                                if (ff.has(layer)) {
                                    found = true;
                                }
                            }

                            //if one is not free then not possible
                            if (found) {
                                j = fieldW;
                                i = fieldH;
                                break;
                            }

                            //collect the valid fields to check them all with predicate
                            allFields.add(ff);
                        }
                    }
                }

                if (found) {
                    continue;
                }

                boolean allPred = allFields.stream().allMatch(ff -> (pred == null || pred.test(ff)));

                if (allPred) {
                    f.add(fields[y][x]);
                }
            }
        }
        return f;
    }

    public List<Field> getAllSides(int x, int y) {
        List<Field> l = new LinkedList<>();
        for (Direction dir : Direction.values()) {
            Point p = Direction.stepDir(dir);
            p.x += x;
            p.y += y;

            if (in(p.x, p.y)) {
                l.add(getField(p.x, p.y));
            }
        }
        return l;
    }

    public List<Field> getSurrounding(int x, int y) {
        List<Field> l = new LinkedList<>();
        for(int i = y-1; i <= y+1; i++) {
            for(int j = x-1; j <= x+1; j++) {
                if (in(j, i)) {
                    l.add(getField(j, i));
                }
            }
        }
        return l;
    }
    
    public Set<Field> getSurroundingSet(int x, int y) {
        Set<Field> l = new HashSet<>();
        for(int i = y-1; i <= y+1; i++) {
            for(int j = x-1; j <= x+1; j++) {
                if (in(j, i)) {
                    l.add(getField(j, i));
                }
            }
        }
        return l;
    }
    
    public Set<Thing> reachableThings(Point start) {

        Model tbox = TBox.model;

        Set<Thing> things = new HashSet<>();
        Queue<Point> q = new LinkedList<>();
        q.add(start);

        Set<Point> visited = new HashSet<>();

        Set<Resource> walls = new HashSet<>(tbox.listResourcesWithProperty(RDFS.subClassOf, TBox.wall).toSet());
        Set<Resource> doors = new HashSet<>(tbox.listResourcesWithProperty(RDFS.subClassOf, TBox.door).toSet());

        int visitedCount = 0;
        int alreadyVisitedCount = 0;
        int notInMapCount = 0;

        while (!q.isEmpty()) {
            Point p = q.poll();

            if (visited.contains(p)) {
                continue;
            }

            visited.add(p);

            visitedCount++;

            //for all directions
            for (Direction dir : Direction.values()) {
                Point next = Direction.stepDir(dir);
                next.x += p.x;
                next.y += p.y;

                //System.out.println("next: " + next);
                //already visited
                if (visited.contains(next)) {
                    alreadyVisitedCount++;
                    continue;
                }

                //not in map
                if (!in(next.x, next.y)) {
                    notInMapCount++;
                    continue;
                }

                //try to add the next
                boolean moveFurther = true;

                Field f = getField(next.x, next.y);
                for (Thing thing : Arrays.asList(f.lying, f.obstacle, f.hovering)) {

                    if (thing == null) {
                        continue;
                    }

                    //System.out.println(thing.type);
                    boolean isDoor = doors.contains(thing.type); //tbox.contains(thing.type, RDFS.subClassOf, TBox.door);
                    boolean isWall = walls.contains(thing.type); //tbox.contains(thing.type, RDFS.subClassOf, TBox.wall);

                    //if it is a obstacle and a door: move further
                    if (thing == f.obstacle) {
                        moveFurther = isDoor;
                    }

                    //if it is not a wall on this field add it to things
                    if (!isWall) {
                        things.add(thing);
                    }
                }

                //a free field to go
                if (moveFurther) {
                    q.add(next);
                }
            }
        }

        //System.out.println("w*h: " + w*h);
        //System.out.println("visitedCount: " + visitedCount);
        //System.out.println("alreadyVisitedCount: " + alreadyVisitedCount);
        //System.out.println("notInMapCount: " + notInMapCount);
        //System.out.println("visited.size(): " + visited.size());
        return things;
    }

    public Set<Thing> nonGroundThings() {
        return getFields().stream().flatMap(f -> f.getNonGroundThings().stream()).collect(toSet());
    }

    public List<Path> allPathFrom(Point start) {
        Queue<Path> q = new LinkedList<>();
        q.add(new Path(start));

        List<Path> paths = new LinkedList<>();

        Set<Resource> walls = new HashSet<>(TBox.model.listResourcesWithProperty(RDFS.subClassOf, TBox.wall).toSet());
        Set<Resource> doors = new HashSet<>(TBox.model.listResourcesWithProperty(RDFS.subClassOf, TBox.door).toSet());

        Set<Point> visited = new HashSet<>();

        while (!q.isEmpty()) {
            Path path = q.poll();

            if (visited.contains(path.last().p)) {
                continue;
            }

            visited.add(path.last().p);

            //for all directions
            for (Direction dir : Direction.values()) {
                Path nextPath = path.copy();

                Point nextPoint = Direction.stepDir(dir);
                nextPoint.x += path.last().p.x;
                nextPoint.y += path.last().p.y;

                if (visited.contains(nextPoint)) {
                    continue;
                }

                nextPath.add(new Path.Element(dir, nextPoint));

                //System.out.println("next: " + nextPath);
                //try to add the next
                boolean moveFurther = true;

                //already visited
                //if(path.visited().contains(nextPoint)) {
                //    continue;
                //}
                //not in map
                if (!in(nextPoint.x, nextPoint.y)) {
                    nextPath.last().endReason = "outer";
                    moveFurther = false;
                }

                if (moveFurther) {
                    Field f = getField(nextPoint.x, nextPoint.y);
                    for (Thing thing : Arrays.asList(f.lying, f.obstacle, f.hovering)) {
                        if (thing == null) {
                            continue;
                        }

                        boolean isObstracle = thing == f.obstacle;
                        boolean isWall = walls.contains(thing.type);

                        if (isWall || isObstracle) {
                            moveFurther = false;
                            nextPath.last().end = f;
                        }

                        if (isWall) {
                            nextPath.last().endReason = "wall";
                        } else if (isObstracle) {
                            nextPath.last().endReason = "obstacle";
                        }
                    }
                }

                //a free field to go
                if (moveFurther) {
                    q.add(nextPath);
                } else {
                    paths.add(nextPath);
                }

            }
        }

        return paths;
    }

    
    boolean astarDebug = false;
    
    public Path astar(Point start, Point end) {
        List<Path> q = new LinkedList<>();
        q.add(new Path(start));
        
        if(start.equals(end)) {
            return q.get(0);
        }
        
        Set<Point> visited = new HashSet<>();
        
        Path nearest = q.get(0);
        
        while(!q.isEmpty()) {
            q.sort((a, b) -> {
                return Double.compare(
                        a.size() + a.last().p.distance(end), 
                        b.size() + b.last().p.distance(end)
                );
            });
            
            //pop best
            Path path = q.get(0);
            q.remove(0);
            
            if(astarDebug) {
                System.out.println("left: " + q.size());
                System.out.println("path: " + path);
            }
            
            if (visited.contains(path.last().p)) {
                continue;
            }
            visited.add(path.last().p);
            
            //for all directions
            for (Direction dir : Direction.values()) {
                Path nextPath = path.copy();

                Point nextPoint = Direction.stepDir(dir);
                nextPoint.x += path.last().p.x;
                nextPoint.y += path.last().p.y;

                if (visited.contains(nextPoint)) {
                    continue;
                }

                nextPath.add(new Path.Element(dir, nextPoint));

                //System.out.println("next: " + nextPath);
                //try to add the next
                boolean moveFurther = true;

                //already visited
                //if(path.visited().contains(nextPoint)) {
                //    continue;
                //}
                //not in map
                if (!in(nextPoint.x, nextPoint.y)) {
                    nextPath.last().endReason = "outer";
                    moveFurther = false;
                }

                if (moveFurther) {
                    Field f = getField(nextPoint.x, nextPoint.y);
                    for (Thing thing : Arrays.asList(f.lying, f.obstacle, f.hovering)) {
                        if (thing == null) {
                            continue;
                        }

                        boolean isObstracle = thing == f.obstacle;
                        boolean isWall = walls.contains(thing.type);
                        boolean isChar = chars.contains(thing.type);

                        if (isWall || (isObstracle && !isChar)) {
                            moveFurther = false;
                            nextPath.last().end = f;
                        }

                        if (isWall) {
                            nextPath.last().endReason = "wall";
                        } else if (isChar) {
                            nextPath.last().endReason = "char";
                        } else if (isObstracle) {
                            nextPath.last().endReason = "obstacle";
                        }
                    }
                }
                
                if(astarDebug)
                    System.out.println(dir + "=" + moveFurther + " " + nextPath.last().endReason);

                //a free field to go
                if (moveFurther) {
                    q.add(nextPath);
                } else {
                    //you can not move but its the neares
                    //if(path.last().p.distance(end) < nearest.last().p.distance(end)) {
                    //    nearest = path;
                    //}
                }

                //found: direct return
                if(nextPath.last().p.equals(end)) {
                    nextPath.last().endReason = "found";
                    return nextPath;
                }
            }
        }
        return nearest;
    }
    
    public int countType(Resource type) {
        int sum = 0;
        for (Field f : getFields()) {
            sum += f.hasThingWithType(type) ? 1 : 0;
        }
        return sum;
    }

    public int countType(Resource type, Rectangle inArea) {
        int sum = 0;
        for (Field f : getFields(inArea)) {
            sum += f.hasThingWithType(type) ? 1 : 0;
        }
        return sum;
    }

    public static Resource getLayer(GameMap object) {
        for (Resource layer : Arrays.asList(TBox.hovering, TBox.lying, TBox.ground)) {
            if (TBox.model.contains(object.type, RDFS.subClassOf, layer)) {
                return layer;
            }
        }
        return TBox.obstacle;
    }

    public void saveImage(File output) {
        BufferedImage bi = new BufferedImage(w * size, h * size, BufferedImage.TYPE_INT_RGB);
        Graphics g = bi.createGraphics();
        GameMapDrawer.drawGameMap(null, this, new Rectangle(0, 0, w * size, h * size), g);
        g.dispose();
        try {
            ImageIO.write(bi, "png", output);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    
    public Rectangle toRect() {
        return new Rectangle(0, 0, w, h);
    }
}
