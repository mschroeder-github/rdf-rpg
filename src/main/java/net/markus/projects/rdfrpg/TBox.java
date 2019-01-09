package net.markus.projects.rdfrpg;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;

/**
 *
 * @author Markus Schr&ouml;der
 */
public class TBox {
    
    public static final String graph="t:box";
    public static final String ns="t:";

    public static Model model;
    static {
        model = ModelFactory.createDefaultModel();
        model.read(Game.class.getResourceAsStream("/t.ttl"), null, "TTL");
        //dataset.addNamedModel(TBox.graph, tbox);
    }
    
    protected static final Resource resource( String local )
        { return ResourceFactory.createResource(ns + local ); }

    protected static final Property property( String local )
        { return ResourceFactory.createProperty(ns + local ); }
    
    public static final Resource tchar = resource("char");
    public static final Resource tchip = resource("chip");
    public static final Resource chiphigh = resource("chiphigh");
    public static final Resource wall = resource("wall");
    public static final Resource door = resource("door");
    public static final Resource room = resource("room");
    public static final Resource districtFloor = resource("districtFloor");
    public static final Resource objectVertical = resource("objectVertical");
    public static final Resource objectHorizontal = resource("objectHorizontal");
    public static final Resource objectSynset = resource("objectSynset");
    
    //layers
    public static final Resource hovering = resource("hovering");
    public static final Resource obstacle = resource("obstacle");
    public static final Resource lying = resource("lying");
    public static final Resource ground = resource("ground");
    
    //position constraint
    public static final Resource mustWallUp = resource("mustWallUp");
    public static final Resource mustWallLeft = resource("mustWallLeft");
    public static final Resource mustWallRight = resource("mustWallRight");
    public static final Resource mustWallDown = resource("mustWallDown");
    
    public static final Resource person = resource("person");
    public static final Resource male = resource("male");
    public static final Resource female = resource("female");
    public static final Resource personLiving = resource("personLiving");
    public static final Resource personDead = resource("personDead");
    public static final Resource personYoung = resource("personYoung");
    public static final Resource personTeen = resource("personTeen");
    public static final Resource personMiddle = resource("personMiddle");
    public static final Resource personOld = resource("personOld");
    
    //routine
    public static final Resource routineSleep = resource("routineSleep");
    public static final Resource routineEat = resource("routineEat");
    public static final Resource routineCutTree = resource("routineCutTree");
    public static final Resource routineInn = resource("routineInn");
    
    
    public static final Property resource = property("resource"); 
    public static final Property resourceLocation = property("resourceLocation");
    //used for horizontal and vertical objects with multi chips
    public static final Property chipList = property("chipList");
    //for making mandatory and optional objects in rooms
    public static final Property mandatory = property("mandatory");
    public static final Property optional = property("optional");
    //for building objectSynset
    public static final Property synonym = property("synonym");
    
    //person
    public static final Property firstname = property("firstname");
    public static final Property lastname = property("lastname");
    public static final Property age = property("age");
    public static final Property agePhase = property("agePhase");
    public static final Property look = property("look");
    public static final Property gender = property("female");
    public static final Property partner = property("partner");
    public static final Property parent = property("parent");
    public static final Property child = property("child");
    public static final Property friend = property("friend");
    public static final Property sibling = property("sibling");
    
    
    public static final Property dailyRoutine = property("dailyRoutine");
    public static final Property begin = property("begin");
    public static final Property routine = property("routine");
    
}
