package net.markus.projects.rdfrpg;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Predicate;
import static java.util.stream.Collectors.toList;
import net.markus.projects.rdfrpg.Path.Element;
import net.markus.projects.rdfrpg.RandomRectsGenerator.Rect;
import net.markus.projects.rdfrpg.RectBinPack.A;
import net.markus.projects.rdfrpg.RectBinPack.Result;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;

/**
 *
 * @author Markus Schr&ouml;der
 */
public class GenerateGameContent {

    //takes a game and fills it
    private Game game;
    private Random rnd;

    private RandomRectsGenerator rrg;
    private InteriorFurnishing interfur;
    private NameGenerator nameGenerator;
    
    String street = "t:stoneWayLight";
    String sidewalk = "t:stoneWayGray";

    public GenerateGameContent(Game game, int seed) {
        this.game = game;
        this.rnd = new Random(seed);

        rrg = new RandomRectsGenerator();
        interfur = new InteriorFurnishing();
        nameGenerator = new NameGenerator(rnd);
    }

    public void generate(int maxFamilies) {
        List<Family> families = new ArrayList<>();
        
        //for each entrance orientation a family list
        Map<Direction, List<Family>> dir2fam = new HashMap<>();
        for(Direction dir : Direction.values()) {
            dir2fam.put(dir, new ArrayList<>());
        }
        
        for(int i = 0; i < maxFamilies; i++) {
            Family family = generateFamily();
            
            dir2fam.get(family.entranceOrientation).add(family);
            
            //family.save();
            //randomRects.print();

            //TODO remove later
            //game.map.paste(house, 0, 0);
        }
        
        //create districts and fill city map
        GameMap city = urbanPlanning(families, dir2fam, game.map);
        
        //full game map
        
        GameMap full = new GameMap(city.w*2, city.h*2);
        int cityX = city.w/2;
        int cityY = city.h/2;
        int surroundW = (int) Math.ceil(city.w/2.0);
        int beachH = city.h/4;
        int sandH = 5;
        full.paste(city, cityX, cityY);
        
        //to have 2 tiled street
        full.drawRect(cityX-1, cityY-1, city.w+2, city.h+2, GameMap.ground(this.street));
        
        //city wall
        Resource cityWall = Rnd.randomlySelect(new LinkedList<>(GameMap.walls), rnd);
        full.drawRect(cityX-2, cityY-2, city.w+4, city.h+4+beachH, GameMap.wall(cityWall.getURI()));
        int waterBegin = cityY-2+city.h+4+beachH-1;
        full.drawLineH(cityX-2, waterBegin, city.w+4, (f) -> {
            f.obstacle = null;
            f.hovering = null;
        });
        
        //beach and water
        full.fillRect(0, waterBegin-sandH, full.w, sandH, GameMap.ground("t:sand"));
        full.fillRect(0, waterBegin-2, full.w, 2, GameMap.ground("t:sandBeach"));
        full.fillRect(0, waterBegin, full.w, beachH, GameMap.obstacle("t:water"));
        
        //surrounding
        full.fillRect(0, 0, surroundW-2, waterBegin-sandH, GameMap.ground("t:longGras"));
        full.fillRect(surroundW-2 + city.w + 3, 0, surroundW, waterBegin-sandH, GameMap.ground("t:longGras"));
        full.fillRect(0, 0, full.w, cityY-2, GameMap.ground("t:longGras"));
        full.fillRect(cityX-2, cityY+city.h+1, city.w+4, (waterBegin-sandH)-(cityY+city.h+1), GameMap.ground("t:shortGras"));
        
        generateNature(full);
        
        game.map = full;
        
        System.out.println("city: " + city.w + "x" + city.h);
        System.out.println("full: " + full.w + "x" + full.h);
    }

    private void generateNature(GameMap map) {
        
        List<Field> possibleFields = 
        map.getFreeRoomFields(map.toRect(), 2, 1, TBox.obstacle, f -> {
            Predicate<Field> noDoor = map.noDoor();
            return noDoor.test(f) && f.has(TBox.ground) && f.ground.type.getURI().endsWith("Gras");
        });
        
        int max = possibleFields.size();
        int count = (int) (possibleFields.size() * 0.05f);
        
        while((max - possibleFields.size()) < count) {

            Field f = Rnd.randomlyRemove(possibleFields, rnd);
            Field f2 = map.getField(f.x+1, f.y);
            if(f.has(TBox.obstacle) || f2.has(TBox.obstacle))
                continue;
            
            GameMap treeObj = InteriorFurnishing.toObject(ResourceFactory.createResource("t:tree"));
            map.paste(treeObj, f.x, f.y);
        }
        
        //stones
        
        List<String> rockTypes = Arrays.asList("t:rock", "t:rockBlue", "t:rockBroken");
        
        possibleFields = 
        map.getFreeRoomFields(map.toRect(), 2, 1, TBox.obstacle, f -> {
            Predicate<Field> noDoor = map.noDoor();
            return noDoor.test(f) && !f.hasRoom();
        });
        max = possibleFields.size();
        count = (int) (possibleFields.size() * 0.01f);
        
        while((max - possibleFields.size()) < count) {
            Field f = Rnd.randomlyRemove(possibleFields, rnd);
            map.draw(f.x, f.y, GameMap.obstacle(Rnd.randomlySelect(rockTypes, rnd)));
        }
        
    }
    
    //every family has grandparents, parents and kids
    //some are dead because of reasons
    //they live in one house (each couple a bedroom)
    //a family contains the family rdf model, house map, room assignments, stuff in container-items
    private Family generateFamily() {
        
        Model model = ModelFactory.createDefaultModel();
        
        Family family = new Family();
        family.model = model;
        
        //kidEnd depends on yungest parent - 20 (becuse 20 is the youngest giving birth age)
        
        //adult
            //old    75-85 (-,m,f,mf) (mütter/väter)-licherseits
            //middle 30-50 (-,m,f,mf)
        
        //old
        String familyA = nameGenerator.familyName();
        String maleA = nameGenerator.maleName();
        Resource grandpaFromFather = ABox.person(maleA, familyA, Rnd.randomInt(ABox.oldBegin, ABox.oldEnd, rnd), TBox.male, model);
        String femaleA = nameGenerator.femaleName();
        Resource grandmaFromFather = ABox.person(femaleA, familyA, Rnd.randomInt(ABox.oldBegin, ABox.oldEnd, rnd), TBox.female, model);
        
        model.add(grandpaFromFather, TBox.partner, grandmaFromFather);
        model.add(grandmaFromFather, TBox.partner, grandpaFromFather);
        
        //old
        String familyB = nameGenerator.familyName();
        String maleB = nameGenerator.maleName();
        Resource grandpaFromMother = ABox.person(maleB, familyB, Rnd.randomInt(ABox.oldBegin, ABox.oldEnd, rnd), TBox.male, model);
        String femaleB = nameGenerator.femaleName();
        Resource grandmaFromMother = ABox.person(femaleB, familyB, Rnd.randomInt(ABox.oldBegin, ABox.oldEnd, rnd), TBox.female, model);
        
        model.add(grandpaFromMother, TBox.partner, grandmaFromMother);
        model.add(grandmaFromMother, TBox.partner, grandpaFromMother);
        
        family.familyNames = Arrays.asList(familyA, familyB);
        
        //middle
        String familyC = familyA; //used the name from father's family
        String maleC = nameGenerator.maleName();
        Resource father = ABox.person(maleC, familyC, Rnd.randomInt(ABox.midBegin, ABox.midEnd, rnd), TBox.male, model);
        String femaleC = nameGenerator.femaleName();
        Resource mother = ABox.person(femaleC, familyC, Rnd.randomInt(ABox.midBegin, ABox.midEnd, rnd), TBox.female, model);
        
        model.add(father, TBox.partner, mother);
        model.add(mother, TBox.partner, father);
        
        model.add(mother, TBox.parent, grandpaFromMother);
        model.add(grandpaFromMother, TBox.child, mother);
        model.add(mother, TBox.parent, grandmaFromMother);
        model.add(grandmaFromMother, TBox.child, mother);
        
        model.add(father, TBox.parent, grandpaFromFather);
        model.add(grandpaFromFather, TBox.child, father);
        model.add(father, TBox.parent, grandmaFromFather);
        model.add(grandmaFromFather, TBox.child, father);
        
        Resource[] livingOrDead = new Resource[] { TBox.personLiving, TBox.personDead };
        
        //kids (can only exist if adult in house)
            //teen  13-25 (0-2) (m,f)
            //young  6-12 (0-2) (m,f)
        
        int kidEnd = Math.min(model.getRequiredProperty(mother, TBox.age).getInt(), model.getRequiredProperty(father, TBox.age).getInt())-20;
        kidEnd = Math.max(25, kidEnd);
        
        //kids (can only exist if adult in house)
            //teen  13-25 (0-2) (m,f)
            //young  6-12 (0-2) (m,f)
        int kidsNumber = rnd.nextInt(3); //0, 1 or 2 kids
        List<Resource> kids = new ArrayList<>();
        List<Resource> livingKids = new ArrayList<>();
        for(int i = 0; i < kidsNumber; i++) {
            
            Resource gender = Rnd.randomlySelect(Arrays.asList(TBox.male, TBox.female), rnd);
            String name;
            if(gender.equals(TBox.male)) {
                name = nameGenerator.maleName();
            } else {
                name = nameGenerator.femaleName();
            }
            
            int age = Rnd.randomInt(ABox.kidBegin, kidEnd, rnd);
            Resource kid = ABox.person(name, familyC, age, gender, model);
            
            model.add(kid, RDF.type, Rnd.randomlySelect(new float[] { 0.95f, 0.05f }, livingOrDead, rnd));
            
            model.add(kid, TBox.parent, father);
            model.add(kid, TBox.parent, mother);
            
            model.add(father, TBox.child, kid);
            model.add(mother, TBox.child, kid);
            
            //siblings
            for(Resource otherKid : kids) {
                model.add(kid, TBox.sibling, otherKid);
                model.add(otherKid, TBox.sibling, kid);
            }
            
            kids.add(kid);
            
            if(model.contains(kid, RDF.type, TBox.personLiving)) {
                livingKids.add(kid);
            }
        }
            
        //sort kids by age
        livingKids.sort((o1, o2) -> {
            return Integer.compare(model.getRequiredProperty(o2, TBox.age).getInt(), model.getRequiredProperty(o1, TBox.age).getInt());
        });
        
        //decide who lives and is dead
        model.add(grandpaFromFather, RDF.type, Rnd.randomlySelect(new float[] { 0.2f, 0.8f }, livingOrDead, rnd));
        model.add(grandmaFromFather, RDF.type, Rnd.randomlySelect(new float[] { 0.2f, 0.8f }, livingOrDead, rnd));
        model.add(grandpaFromMother, RDF.type, Rnd.randomlySelect(new float[] { 0.2f, 0.8f }, livingOrDead, rnd));
        model.add(grandmaFromMother, RDF.type, Rnd.randomlySelect(new float[] { 0.2f, 0.8f }, livingOrDead, rnd));
        model.add(father, RDF.type, Rnd.randomlySelect(new float[] { 0.8f, 0.2f }, livingOrDead, rnd));
        model.add(mother, RDF.type, Rnd.randomlySelect(new float[] { 0.8f, 0.2f }, livingOrDead, rnd));
        
        //model.write(System.out, "TTL");
        
        List<Resource> deads   = model.listSubjectsWithProperty(RDF.type, TBox.personDead).toList();
        List<Resource> livings = model.listSubjectsWithProperty(RDF.type, TBox.personLiving).toList();
        
        //give all livings a look (char from charset) based on gender and agePhase
        
        Map<Resource, Map<Resource, List<Resource>>> gender2age2looks = new HashMap<>();
        List<Resource> agePhases = Arrays.asList(TBox.personYoung, TBox.personTeen, TBox.personMiddle, TBox.personOld);
        List<Resource> genders = Arrays.asList(TBox.male, TBox.female);
        
        List<Resource> chars = TBox.model.listSubjectsWithProperty(RDFS.subClassOf, TBox.tchar).toList();
        for(Resource gender : genders) {
            List<Resource> genderChars = chars.stream().filter(c -> c.hasProperty(RDFS.subClassOf, gender)).collect(toList());
            
            Map<Resource, List<Resource>> age2looks;
            if(gender2age2looks.containsKey(gender)) {
                age2looks = gender2age2looks.get(gender);
            } else {
                age2looks = new HashMap<>();
                gender2age2looks.put(gender, age2looks);
            }
            
            for(Resource agePhase : agePhases) {
                List<Resource> agePhaseChars = genderChars.stream().filter(c -> c.hasProperty(RDFS.subClassOf, agePhase)).collect(toList());
                age2looks.put(agePhase, agePhaseChars);
            }
        }
        
        //give every living a look
        for(Resource living : livings) {
            List<Resource> looks = gender2age2looks.get(living.getRequiredProperty(TBox.gender).getResource()).get(living.getRequiredProperty(TBox.agePhase).getResource());
            living.addProperty(TBox.look, Rnd.randomlyRemove(looks, rnd));
        }
        
        //======================================================================
        //couple to assign bedroom
        
        //sort livings by age (desc)
        livings.sort((o1, o2) -> {
            return Integer.compare(o2.getRequiredProperty(TBox.age).getInt(), o1.getRequiredProperty(TBox.age).getInt());
        });
        
        List<Set<Resource>> livingCouples = new LinkedList<>();
        
        addCouple(grandpaFromFather, grandmaFromFather, model, livingCouples);
        addCouple(grandpaFromMother, grandmaFromMother, model, livingCouples);
        addCouple(father, mother, model, livingCouples);
        
        int livingCouplesCount = livingCouples.size();
        
        for(Resource kid : livingKids) {
            addCouple(kid, null, model, livingCouples);
        }
        
        //each living couple and kid need a bedroom
        int bedroomCount = livingCouplesCount;
        bedroomCount += livingKids.size();
        
        //if all are dead or do not exist
        if(bedroomCount == 0) {
            bedroomCount = 1 + rnd.nextInt(3); // 1 - 3
        }
        
        //1 entrance (maybe with kitchen)
        //1 kitchen
        //n bedrooms
        //1 toilet
        //(optional: store)
        //number of rooms
        int numberOfRooms = 2 + bedroomCount + 1;
        
        
        
        //System.out.println(family);
        //System.out.println(livings.size() + " livings, " + deads.size() + " deads, " + bedroomCount + " bedrooms, " + numberOfRooms + " rooms");
        
        //house generation and assignment ======================================
        
        //House Generation Config
        int maxRectSize = 4;
        int maxBoundSize = numberOfRooms * maxRectSize;
        rrg.debug = false;
        interfur.debug = false;
        //tbox bed objects
        Resource bedheadVertical = ResourceFactory.createResource("t:bedheadVertical");
        Resource bedheadHorizontal = ResourceFactory.createResource("t:bedheadHorizontal");
        
        //this could produce too small rooms which is why we have to check this
        boolean redo;
        RandomRectsGenerator.RandomRects randomRects;
        GameMap house;
        
        List<Path> shortestEntrancePaths;
        do {
            //we assume that this time we do not have to redo generation
            redo = false;
            
            //we generate valid (connected) rooms
            randomRects = rrg.generateValid(
                //    RandomRectsGenerator.seedRectsV2( Axis.Horizontal,numberOfRooms,new Dimension(maxBoundSize,maxBoundSize)),
                RandomRectsGenerator.seedRectsV3(numberOfRooms, new Dimension(maxBoundSize,maxBoundSize)),
                //RandomRectsGenerator.seedRectsTriangle(4, new Dimension(20, 20)),
                new Dimension(maxBoundSize, maxBoundSize),
                new Dimension(maxRectSize*2, maxRectSize*2),
                3, 10000, rnd);
            
            //we assign room types and insert stuff
            house = interfur.generate(randomRects, rnd);

            //try find the shortest path from entrance to outer
            //could be the case that it is sourrounded by rooms
            shortestEntrancePaths = house.allPathFrom(house.entrance);
            shortestEntrancePaths = Path.shortest(shortestEntrancePaths, "outer");
            if(shortestEntrancePaths.isEmpty()) {
                redo = true;
                continue;
            }
            
            //assign ownage
            //for each bedroom assign a couple and kids
            int coupleIndex = 0;
            for(Rect room : randomRects.sortByArea()) {
                if(room.types.contains("t:roomBedroom")) {
                    
                    if(coupleIndex >= livingCouples.size()) {
                        room.owners = new HashSet<>(); //empty (dead)
                    } else {
                        room.owners = livingCouples.get(coupleIndex++);
                    }
                    //System.out.println(room + " " + room.types + " " + room.owners);

                    //check if there is a bed in the bedroom
                    int count = house.countType(bedheadVertical, room.rect) + house.countType(bedheadHorizontal, room.rect);
                    if(count == 0) {
                        redo = true;
                        break;
                    }

                    //place owners of room in rooms
                    for(Resource owner : room.owners) {
                        
                        List<Field> ownerFields = house.getFreeRoomFields(room.rect, 1, 1, TBox.obstacle, f -> true);
                        
                        boolean foundOwnerField = false;
                        while(!ownerFields.isEmpty()) {
                            Field free = Rnd.randomlyRemove(ownerFields, rnd);
                            
                            if(house.astar(free.toPoint(), house.entrance).isMoveable()) {
                                free.obstacle = new Thing(owner.getURI(), model.getRequiredProperty(owner, TBox.look).getResource().getURI());
                                foundOwnerField = true;
                                break;
                            }
                        }
                        
                        //we could not find a free field that can be used to go
                        //to entrance
                        if(!foundOwnerField) {
                            redo = true;
                            break;
                        }
                    }
                }
            }
            
        } while(redo);
        
        family.house = house;
        family.randomRects = randomRects;
        family.livingCouples = livingCouples;
        
        //======================================================================
        //way to entrance
        
        //pick one of shortest way to outer from entrance and draw way
        //calculate the orientation for city creation
        String way = "t:pebbles";
        Path shortestEntrancePath = Rnd.randomlySelect(shortestEntrancePaths.subList(0, Math.min(5, shortestEntrancePaths.size())), rnd);
        for(Element e : shortestEntrancePath.withoutLast()) {
            house.draw(e.p.x, e.p.y, GameMap.ground(way));
        }
        Element last = shortestEntrancePath.last();
        Direction entranceOrientation = null;
        if(last.p.x < 0) {
            entranceOrientation = Direction.Left;
        } else if(last.p.y < 0) {
            entranceOrientation = Direction.Up;
        } else if(last.p.x >= house.w) {
            entranceOrientation = Direction.Right;
        } else if(last.p.y >= house.h) {
            entranceOrientation = Direction.Down;
        }
        family.entranceOrientation = entranceOrientation;
        
        
        //System.out.println(shortestEntrancePath);
        //System.out.println(entranceOrientation);
        
        //======================================================================
        //triples for each living
        
        
        
        //create graph for each living person
        for(Resource living : livings) {
            Model npcModel = game.dataset.getNamedModel(living.getURI());
            
            //the whole family tree
            npcModel.add(model);
            
            //create schedule (set job)
            /*
                0 - 6 sleep (find your bed, go to bed, sleep)
                6 - 7 eat (find kitchen, go randomly to kitchen objects)
                7 - 11 cut tree (find next tree, go to tree, cut)
                11 - 12 eat (find kitchen, go randomly to kitchen objects)
                12 - 16 cut tree (find next tree, go to tree, cut)
                16 - 20 inn (go to inn, go randomly to inn objects)
                20 - 6 sleep (find your bed, go to bed, sleep)
            */
            addRoutine(living, 0, TBox.routineSleep, npcModel);
            addRoutine(living, 6, TBox.routineEat, npcModel);
            addRoutine(living, 7, TBox.routineCutTree, npcModel);
            addRoutine(living, 11, TBox.routineEat, npcModel);
            addRoutine(living, 12, TBox.routineCutTree, npcModel);
            addRoutine(living, 16, TBox.routineInn, npcModel);
            addRoutine(living, 20, TBox.routineSleep, npcModel);
        }
            
        //all:
        //TODO put people in pictures
        
        //dead:
        //TODO add reason for death
            //TODO outside: skeleton
            //TODO inside: put to graveyard
        //TODO add grave if person is dead?
        
        //living:
        //assign people to (bed)room OK
        //assign char(set) to person if not dead OK
        
        
        return family;
    }
    
    private void addRoutine(Resource living, int hourBegin, Resource routine, Model model) {
        //(own uri) t:dailyRoutine a:routine1234 .
        //a:routine1234 t:begin 0.35 ;  
        //              t:routine t:sleep . # go to sleep
        //
        //(own uri) t:doing (uri of routine) # to indicate in which main routine you are
        //(own uri) t:subdoing [  ] # because in a routine can be different stages or phases

        //(own uri) t:routine t:sleep .
        //(own uri) t:routineLine "1" .
        
        Resource routineRes = ABox.resource(routine.getLocalName() + "-" + hourBegin);
        model.add(living, TBox.dailyRoutine, routineRes);
        
        model.addLiteral(routineRes, TBox.begin, Game.convertToRatio(LocalTime.of(hourBegin, 0, 0)));
        model.add(routineRes, TBox.routine, routine);
    }
    
    private void addCouple(Resource m, Resource f, Model model, List<Set<Resource>> livingCouples) {
        Set<Resource> s = new HashSet<>();
        if(model.contains(m, RDF.type, TBox.personLiving)) {
            s.add(m);
        }
        if(f != null) {
            if(model.contains(f, RDF.type, TBox.personLiving)) {
                s.add(f);
            }
        }
        if(!s.isEmpty()) {
            livingCouples.add(s);
        }
    }
    
    //generate from houses districts
    private GameMap urbanPlanning(List<Family> families, Map<Direction, List<Family>> dir2fam, GameMap map) {
        
        //dir2fam.entrySet().forEach(e -> System.out.println(e.getKey() + " " + e.getValue().size()));

        List<District> districts = new ArrayList<>();
        
        //we build many districts
        while(!isEmpty(dir2fam)) {
            
            District dist;
            if(hasDirs(dir2fam, Direction.values())) {
                dist = createDistrict4(dir2fam);
                
            } else if(hasDirs(dir2fam, Direction.Left, Direction.Right)) {
                dist = createDistrict2(dir2fam, Axis.Horizontal);
                
            } else if(hasDirs(dir2fam, Direction.Up, Direction.Down)) {
                dist = createDistrict2(dir2fam, Axis.Vertical);
                
            } else {
                //single district
                List<Family> left = dir2fam.values().stream().flatMap(l -> l.stream()).collect(toList());
                Family f = left.get(0);
                dist = new District();
                dist.family = Arrays.asList(f);
                dist.map = f.house;
                
                dir2fam.get(f.entranceOrientation).remove(f);
            }
            
            districts.add(dist);
            
            //System.out.println();
            //dir2fam.entrySet().forEach(e -> System.out.println(e.getKey() + " " + e.getValue().size()));
        }
        
        //dir2fam.entrySet().forEach(e -> System.out.println(e.getKey() + " " + e.getValue().size()));
        System.out.println(districts.size() + " districts");
            
        
        List<Resource> districtFloors = TBox.model.listSubjectsWithProperty(RDFS.subClassOf, TBox.districtFloor).toList();
        
        //give every district a street and ground
        int streetBorder = 2;
        for(District distict : districts) {
            
            GameMap fresh = new GameMap(distict.map.w + (streetBorder*2), distict.map.h + (streetBorder*2));
            
            fresh.fillRect(
                    streetBorder, streetBorder, 
                    fresh.w - streetBorder, fresh.h - streetBorder, 
                    GameMap.ground(Rnd.randomlySelect(districtFloors, rnd).getURI())
            );
            
            for(int i = 0; i < streetBorder; i++) {
                fresh.drawRect(i, i, 
                    fresh.w - (i*2), fresh.h - (i*2), 
                    GameMap.ground((i == streetBorder-1) ? sidewalk : street
                    )
                );
            }
            
            fresh.paste(distict.map, streetBorder, streetBorder);
            
            distict.map = fresh;
        }
        
        
        
        Result<District> result = RectBinPack.run(districts, (d) -> new Dimension(d.map.w, d.map.h));
        
        GameMap city = new GameMap(result.minD.width, result.minD.height);
        for(A<District> a : result.as) {
            city.paste(a.t.map, a.p.x, a.p.y);
        }
        
        Rectangle rect = new Rectangle(0, 0, city.w+1, city.h+1);
        
        for(Field nonGround : city.getFreeRoomFields(rect, 1, 1, TBox.ground, null)) {
            city.draw(nonGround.x, nonGround.y, GameMap.ground(street));
        }
        
        //District district = createDistrict4(dir2fam);
        //map.paste(city, 0, 0);
        
        //city.saveImage(new File("city.png"));
        
        //GameFrame.showGUI(game);
        
        return city;
    }
    
    private District createDistrict4(Map<Direction, List<Family>> dir2fam) {
        Family up = Rnd.randomlyRemove(dir2fam.get(Direction.Up), rnd);
        Family left = Rnd.randomlyRemove(dir2fam.get(Direction.Left), rnd);
        Family right = Rnd.randomlyRemove(dir2fam.get(Direction.Right), rnd);
        Family down = Rnd.randomlyRemove(dir2fam.get(Direction.Down), rnd);

        GameMap map = new GameMap(
                left.house.w + right.house.w,
                up.house.h + Math.max(left.house.h, right.house.h) + down.house.h
        );
        
        int x1 = (left.house.w + right.house.w - up.house.w) / 2;
        int x2 = (left.house.w + right.house.w - down.house.w) / 2;
        
        if(x1 < 0 || x2 < 0) {
            //should not happen
            int a = 0;
        }
        
        map.paste(up.house, x1, 0);
        map.paste(left.house,             0, up.house.h);
        map.paste(right.house, left.house.w, up.house.h);
        map.paste(down.house, x2, up.house.h + Math.max(left.house.h, right.house.h));
        
        District district = new District();
        district.family = Arrays.asList(up, left, right, down);
        district.map = map;
        return district;
    }
    
    private District createDistrict2(Map<Direction, List<Family>> dir2fam, Axis axis) {
        
        Family a = null;
        Family b = null;
        
        int x1 = 0;
        int y1 = 0;
        int x2 = 0;
        int y2 = 0;
        
        int w = 0;
        int h = 0;
        
        switch(axis) {
            case Horizontal: {
                a = Rnd.randomlyRemove(dir2fam.get(Direction.Left), rnd);
                b = Rnd.randomlyRemove(dir2fam.get(Direction.Right), rnd);
                
                w = a.house.w + b.house.w;
                h = Math.max(a.house.h, b.house.h);
                
                int min = Math.min(a.house.h, b.house.h);
                
                x2 = a.house.w;
                
                if(a.house.w > b.house.w) {
                    y2 = (h - min) / 2;
                } else {
                    y1 = (h - min) / 2;
                }
                break;
            }
            case Vertical: {
                a = Rnd.randomlyRemove(dir2fam.get(Direction.Up), rnd);
                b = Rnd.randomlyRemove(dir2fam.get(Direction.Down), rnd);
                
                w = Math.max(a.house.w, b.house.w);
                h = a.house.h + b.house.h;
                
                int min = Math.min(a.house.w, b.house.w);
                
                y2 = a.house.h;
                
                if(a.house.h > b.house.h) {
                    x2 = (w - min) / 2;
                } else {
                    x1 = (w - min) / 2;
                }
                break;
            }
        }
        
        GameMap map = new GameMap(w, h);
        map.paste(a.house, x1, y1);
        map.paste(b.house, x2, y2);
        
        District district = new District();
        district.family = Arrays.asList(a, b);
        district.map = map;
        return district;
    }
    
    /*
    private District createDistrictMix(Map<Direction, List<Family>> dir2fam) {
        List<Entry<Direction, List<Family>>> l = dir2fam
                .entrySet()
                .stream()
                .filter(e -> !e.getValue().isEmpty())
                .collect(toList());
        
        
        
        District district = new District();
        
        if(count(dir2fam) == 1) {
            Family a = dir2fam.entrySet().iterator().next().getValue().get(0);
            dir2fam.get(a.entranceOrientation).remove(a);
            district.family = Arrays.asList(a);
            //district is only one house
            district.map = a.house;
            
        } else {
            Family a = Rnd.randomlyRemove(l, rnd);
            Family b = Rnd.randomlyRemove(l, rnd);
            
            dir2fam.get(a.entranceOrientation).remove(a);
            dir2fam.get(b.entranceOrientation).remove(b);
            
            district.family = Arrays.asList(a, b);
        }
        
        return district;
    }
    */
    
    private boolean hasDirs(Map<Direction, List<Family>> dir2fam, Direction... dirs) {
        for(Direction dir : dirs) {
            if(dir2fam.get(dir).isEmpty()) {
                return false;
            }
        }
        return true;
    } 
    
    private boolean isEmpty(Map<Direction, List<Family>> dir2fam) {
        return count(dir2fam) == 0;
    }
    
    private int count(Map<Direction, List<Family>> dir2fam) {
        return dir2fam.entrySet().stream().mapToInt(e -> e.getValue().size()).sum();
    }
    
    
    
}
