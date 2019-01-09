package net.markus.projects.rdfrpg;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import static java.util.stream.Collectors.toList;
import net.markus.projects.rdfrpg.RandomRectsGenerator.RandomRects;
import net.markus.projects.rdfrpg.RandomRectsGenerator.Rect;
import net.markus.projects.rdfrpg.RandomRectsGenerator.RectIntersection;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFList;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDFS;

/**
 *
 * @author Markus Schr&ouml;der
 */
public class InteriorFurnishing {

    private Model tbox;
    
    public boolean debug;

    public InteriorFurnishing() {
        this.tbox = TBox.model;

        //minimal is
        //Entrance E (Gang)
        //kitchen K
        //oven
        //table
        //chair
        //bedroom B
        //bed
        //toilet T
        //toilet
        //waschbecken
        //(badewanne)
        //store S
        //bag
        //crate
    }

    public GameMap generate(RandomRects rr, Random rnd) {

        Rectangle bb = rr.bb();
        GameMap map = new GameMap(bb.width, bb.height);

        String wallUri = Rnd.randomlySelect(tbox.listSubjectsWithProperty(RDFS.subClassOf, TBox.wall).toList(), rnd).getURI();
        //TODO put in TBox
        String floor = "t:woodFloor";
        String roomdoor = "t:doorWood";
        String entrancedoor = "t:doorPlanks";

        /*
        map.fillRect(
                bb.x, bb.y,
                bb.width, bb.height,
                GameMap.ground("t:shortGras"));
        */
        
        //walls and floors
        for (Rect room : rr.rects) {
            //walls
            map.drawRect(
                    room.rect.x, room.rect.y,
                    room.rect.width, room.rect.height,
                    GameMap.wall(wallUri));

            //floors (may switch between rooms?)
            map.fillRect(
                    room.rect.x, room.rect.y,
                    room.rect.width, room.rect.height,
                    GameMap.ground(floor));
            
            //every field gets a unique ID
            map.fillRect(
                    room.rect.x, room.rect.y,
                    room.rect.width, room.rect.height,
                    GameMap.room(room.uri));
        }

        //room doors
        for (RectIntersection inter : rr.intersections) {
            Point selected = Rnd.randomlySelect(inter.doors, rnd);
            inter.doors.removeIf(d -> d != selected);

            map.draw(selected.x, selected.y, GameMap.door(roomdoor));
        }
        
        //doors have to be reachables
        Set<Thing> doors = map.nonGroundThings();
        doors.removeIf(d -> !tbox.contains(d.type, RDFS.subClassOf, TBox.door));
        map.reachables.addAll(doors);

        //2 rooms (1 single, 1 couple)
        //1) Entrance, Kitchen
        //2) Bedroom, Toilet (1-2 beds)
        //3 rooms (1 single, 1 couple)
        //1) Entrance, Kitchen
        //2) Bedroom (1-2 beds)
        //3) Toilet
        //4 rooms (1 couple)
        //1) Entrance
        //2) Kitchen
        //3) Bedroom (2 beds)
        //4) Toilet    
        //5 rooms (1 couple + 1 kid)
        //1) Entrance
        //2) Kitchen
        //3) Bedroom (couple)
        //4) Bedroom (kid)
        //5) Toilet   
        //set types for the rooms
        List<Rect> rects = rr.sortByIntersection();
        rects.get(0).types.add("t:roomEntrance");
        if (rects.size() <= 3) {
            rects.get(0).types.add("t:roomKitchen");
        } else {
            rects.get(1).types.add("t:roomKitchen");
        }
        if (rects.size() == 2) {
            rects.get(1).types.add("t:roomToilet");
            rects.get(1).types.add("t:roomBedroom");
        }
        for (int i = rects.size() - 1; rects.size() == 3 ? (i >= 1) : (i > 1); i--) {
            if (i == rects.size() - 1) {
                //last one is toilet
                rects.get(i).types.add("t:roomToilet");
            } else {
                rects.get(i).types.add("t:roomBedroom");
            }
        }

        if(debug) {
            for (Rect rect : rects) {
                System.out.println(rect + " " + rect.intersections() + " " + rect.types);
            }
        }

        //put char to visualize in console
        for (Rect rect : rects) {
            rect.c = rect.types.get(0).toString().substring(6).charAt(0);
        }

        //set the entrance door
        List<Point> possibleEntrances = possibleEntrances(rects.get(0), rr);
        if(possibleEntrances.isEmpty()) {
            for (Rect rect : rects) {
                System.out.println(rect + " " + rect.intersections() + " " + rect.types);
            }
            rr.print();
            int a = 0;
        }
        
        Point selectedEntrance = Rnd.randomlySelect(possibleEntrances, rnd);
        map.draw(selectedEntrance.x, selectedEntrance.y, GameMap.door(entrancedoor));

        //store it for later processing
        map.entrance = selectedEntrance;
        
        //for each room configuration
        List<Resource> roomTypes = tbox.listSubjectsWithProperty(RDFS.subClassOf, TBox.room).toList();
        //shuffel to start with always another room
        Collections.shuffle(roomTypes, rnd);
        for (Resource roomType : roomTypes) {
            //get all rooms
            for (Rect room : rr.getByType(roomType.getURI())) {
                //try to put the objects from the config in the room
                interiorFurnishing(room, roomType, map, selectedEntrance, rnd);
            }
        }

        //map.draw(6, 1, GameMap.hovering("t:shelfPotion"));
        if(debug) {
            rr.print();
        }

        return map;
    }

    private void interiorFurnishing(Rect roomRect, Resource roomType, GameMap map, Point entrance, Random rnd) {
        //for each room type try to add the minimal requirement
        for (RDFNode objectNode : tbox.listObjectsOfProperty(roomType, TBox.mandatory).toList()) {
            //get synset
            GameMapSynset objectSynset = toSynset(objectNode.asResource());
            furnishObject(objectSynset, roomType, roomRect, map, entrance, rnd);
        }
        
        float lastOccupiedRate = -1;
        
        while(true) {
            int free = map.getFreeRoomFields(roomRect.rect, 1, 1, null, f -> true).size();
            int area = (roomRect.rect.width-2) * (roomRect.rect.height-2);
            float occupiedRate = 1.0f - (free / (float) area);

            //nothing changed
            if(lastOccupiedRate == occupiedRate) {
                break;
            }
            
            if(occupiedRate > 0.25f) {
                break;
            }
            
            //to check if something changed after furnishing
            lastOccupiedRate = occupiedRate;
            
            //for all optional objects in the room
            List<RDFNode> optionalNodes = tbox.listObjectsOfProperty(roomType, TBox.optional).toList();
            List<GameMapSynset> optionals = optionalNodes.stream().map(t -> toSynset(t.asResource())).collect(toList());
            
            //count all already existing types in the map
            //idea is to select the infrequent ones
            for(GameMapSynset s : optionals) {
                for(GameMap gm : s) {
                    int count = map.countType(gm.type);
                    s.gm2count.put(gm, count);
                }
            }

            //sort synsets by avg count: select infreqent synsets
            optionals.sort((o1, o2) -> {
                return Double.compare(o1.avgCount(), o2.avgCount());
            });

            //infrequent synsets using infrequent synonyms
            for(GameMapSynset objectSynset : optionals) {
                furnishObject(objectSynset, roomType, roomRect, map, entrance, rnd);
            }
        }
        
    }

    private void furnishObject(GameMapSynset objectSynset, Resource roomType, Rect roomRect, GameMap map, Point entrance, Random rnd) {
        boolean placed = false;
        while (!objectSynset.isEmpty()) {

            GameMap object = objectSynset.randomlyRemoveInfrequent(rnd);
            
            //where will the object be placed
            Resource layer = GameMap.getLayer(object);
            
            //all possible free fields of the map (checks also free door)
            List<Field> free = map.getFreeRoomFields(roomRect.rect, object.w, object.h, layer, map.noDoor());
            
            if(free.isEmpty()) {
                if(debug)
                    System.out.println(object.type + " has no free field in " + roomType);
                continue;
            }

            free = applyContraints(object, roomRect, map, free);
            
            if(free.isEmpty()) {
                if(debug)
                    System.out.println(object.type + " has no free field after contraints in " + roomType);
                continue;
            }

            List<Field> tries = new LinkedList<>();
            
            //try all free fields
            while (!free.isEmpty()) {

                //random select and remove
                Field selected = Rnd.randomlySelect(free, rnd);
                free.remove(selected);
                
                tries.add(selected);
                
                Set<Thing> placedThings = object.nonGroundThings();
                
                if(debug)
                    System.out.println("try: " + placedThings + " at " + selected);

                //paste object in the map
                map.paste(object, selected.x, selected.y);
                
                //Field f81 = map.getField(8, 1);
                
                //check all reachable things from going from entrance
                Set<Thing> reachables = map.reachableThings(entrance);
                
                placedThings.removeAll(reachables);
                Set<Thing> nonReachableMapThings = new HashSet<>(map.reachables);
                nonReachableMapThings.removeAll(reachables);
                
                boolean reachable = true;
                
                if(!placedThings.isEmpty()) {
                    if(debug)
                        System.out.println("now in the map the placed thing is not reachable: " + placedThings);
                    reachable = false;
                }
                
                if(!nonReachableMapThings.isEmpty()) {
                    if(debug)
                        System.out.println("now in the map it is not reachable: " + nonReachableMapThings);
                    reachable = false;
                }
                
                //the whole object is reachable if all non ground things of the 
                //object are in the reachable set
                //and check if the already reachables are again reachable
                //boolean reachable = reachables.containsAll(object.nonGroundThings()) &&
                //                    reachables.containsAll(map.reachables);

                //System.out.println("reachables: " + reachables);
                //System.out.println("map.reachables: " + map.reachables);
                //System.out.println("reachable: " + reachable);
                //System.out.println(selected);
                if (!reachable) {
                    //undo
                    map.undo(object, selected.x, selected.y);
                } else {
                    //System.out.println("was placed: " + object.nonGroundThings() + " at " + selected);
                    //let the object in the map and continue with next object
                    placed = true;
                    //all now reachable have to be reachable in another room
                    map.reachables.addAll(reachables);
                    break;
                }
            }
            
            if(placed) {
                //no other object from the synset needed
                break;
            } else {
                if(debug)
                    System.out.println(object.type + " was tried at " + tries + " because it was not reachable in " + roomType);
            }
        }
    }

    private List<Field> applyContraints(GameMap object, Rect roomRect, GameMap map, List<Field> free) {
        //position constraints check
        if (tbox.contains(object.type, RDFS.subClassOf, TBox.mustWallUp)) {
            free = free.stream().filter(f -> f.y == roomRect.rect.y + 1).collect(toList());
        }
        if (tbox.contains(object.type, RDFS.subClassOf, TBox.mustWallLeft)) {
            free = free.stream().filter(f -> f.x == roomRect.rect.x + 1).collect(toList());
        }
        if (tbox.contains(object.type, RDFS.subClassOf, TBox.mustWallRight)) {
            free = free.stream().filter(f -> f.x + object.w == roomRect.rect.x + roomRect.rect.width - 2).collect(toList());
        }
        if (tbox.contains(object.type, RDFS.subClassOf, TBox.mustWallDown)) {
            free = free.stream().filter(f -> f.y + object.h == roomRect.rect.y + roomRect.rect.height - 2).collect(toList());
        }
        if (tbox.contains(object.type, RDFS.subClassOf, TBox.hovering)) {
            for(Field f : free.toArray(new Field[0])) {
                //if there is a chiphigh obstacle already
                if(f.hasObstacle()) {
                    if(tbox.contains(f.obstacle.type, RDFS.subClassOf, TBox.chiphigh)) {
                        free.remove(f);
                    }
                }
            }
        }
        if (tbox.contains(object.type, RDFS.subClassOf, TBox.chiphigh)) {
            for(Field f : free.toArray(new Field[0])) {
                //if there is a hovering already
                if(f.hasHovering()) {
                    free.remove(f);
                }
            }
        }
        return free;
    }
    
    private GameMapSynset toSynset(Resource object) {
        GameMapSynset synset = new GameMapSynset();
        if (tbox.contains(object, RDFS.subClassOf, TBox.objectSynset)) {
            for (RDFNode objectInSynset : tbox.listObjectsOfProperty(object, TBox.synonym).toList()) {
                synset.add(toObject(objectInSynset.asResource()));
            }
        } else {
            synset.add(toObject(object));
        }
        return synset;
    }

    public static GameMap toObject(Resource object) {
        GameMap gm;

        if (TBox.model.contains(object, RDFS.subClassOf, TBox.objectHorizontal)) {
            gm = GameMap.multiHorizontal(
                    toArray(TBox.model.getRequiredProperty(object, TBox.chipList).getObject())
            );
        } else if (TBox.model.contains(object, RDFS.subClassOf, TBox.objectVertical)) {
            gm = GameMap.multiVertical(
                    toArray(TBox.model.getRequiredProperty(object, TBox.chipList).getObject())
            );
        } else {
            gm = GameMap.single(object.getURI());
        }
        gm.type = object;
        return gm;
    }

    private static String[] toArray(RDFNode chipList) {
        RDFList rdfList = chipList.as(RDFList.class);
        List<RDFNode> l = rdfList.asJavaList();
        String[] array = new String[l.size()];
        for (int i = 0; i < array.length; i++) {
            array[i] = l.get(i).asResource().getURI();
        }
        return array;
    }

    private List<Point> possibleEntrances(Rect entranceRect, RandomRects rr) {
        List<Point> l = new LinkedList<>();

        //up down
        for (int x = entranceRect.rect.x + 1; x < entranceRect.rect.x + entranceRect.rect.width - 2; x++) {
            Point p = goodEntrance(x, entranceRect.rect.y, Direction.Up, rr);
            if (p != null) {
                l.add(p);
            }

            p = goodEntrance(x, entranceRect.rect.y + entranceRect.rect.height - 1, Direction.Down, rr);
            if (p != null) {
                l.add(p);
            }
        }

        //left right
        for (int y = entranceRect.rect.y + 1; y < entranceRect.rect.y + entranceRect.rect.height - 2; y++) {
            Point p = goodEntrance(entranceRect.rect.x, y, Direction.Left, rr);
            if (p != null) {
                l.add(p);
            }

            p = goodEntrance(entranceRect.rect.x + entranceRect.rect.width - 1, y, Direction.Right, rr);
            if (p != null) {
                l.add(p);
            }
        }

        return l;
    }

    private Point goodEntrance(int x, int y, Direction dir, RandomRects rr) {
        Point diff = Direction.stepDir(dir);
        Point p = new Point(x, y);
        Point inPoint = new Point(p.x + diff.x, p.y + diff.y);
        if (!rr.intersects(inPoint)) {
            return p;
        }
        return null;
    }

    @Deprecated
    private List<Point> calculateEntrances(Rect entranceRect, Direction entranceDir, RandomRects rr) {

        List<Point> entrances = new LinkedList<>();

        Point diff = Direction.stepDir(entranceDir);

        if (Direction.axis(entranceDir) == Axis.Horizontal) {
            int x1 = entranceRect.rect.x + 1;
            int x2 = ((int) entranceRect.rect.getMaxX()) - 2;

            int y;
            if (Direction.Up == entranceDir) {
                y = entranceRect.rect.y;
            } else {
                y = entranceRect.rect.y + entranceRect.rect.height - 1;
            }

            for (int i = x1; i <= x2; i++) {
                Point p = new Point(i, y);
                Point inPoint = new Point(p.x + diff.x, p.y + diff.y);

                if (!rr.intersects(inPoint)) {
                    entrances.add(p);
                }
            }
        } else if (Direction.axis(entranceDir) == Axis.Vertical) {
            int y1 = entranceRect.rect.y + 1;
            int y2 = ((int) entranceRect.rect.getMaxY()) - 2;

            int x;
            if (Direction.Left == entranceDir) {
                x = entranceRect.rect.x;
            } else {
                x = entranceRect.rect.x + entranceRect.rect.width - 1;
            }

            for (int i = y1; i <= y2; i++) {
                Point p = new Point(x, i);
                Point inPoint = new Point(p.x + diff.x, p.y + diff.y);

                if (!rr.intersects(inPoint)) {
                    entrances.add(p);
                }
            }
        }

        return entrances;
    }

    public class RoomConfig {

        String type;

        //we use game map because there could be multi-field objects
        List<GameMap> mandatory;
        List<GameMap> optional;

        public RoomConfig(String type) {
            this.type = type;
            mandatory = new ArrayList<>();
            optional = new ArrayList<>();
        }
    }

}
