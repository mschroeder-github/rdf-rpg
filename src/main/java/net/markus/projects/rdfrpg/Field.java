package net.markus.projects.rdfrpg;

import java.awt.Point;
import java.util.HashSet;
import java.util.Set;
import org.apache.jena.rdf.model.Resource;

/**
 *
 * @author Markus Schr&ouml;der
 */
public class Field {
    
    public int x;
    public int y;
    
    public Thing hovering;
    public Thing obstacle;
    public Thing lying;
    public Thing ground;
    
    //if the field is part of some rooms
    public Set<Resource> rooms;

    /*
    public enum Layer {
        Hovering, //t:hovering
        Obstacle, //t:obstacle
        Lying,    //t:lying
        Ground
    }
    */
    
    public Field(int x, int y) {
        this.x = x;
        this.y = y;
    }
    
    public Point toPoint() {
        return new Point(x, y);
    }
    
    public void clear() {
        hovering = null;
        obstacle = null;
        lying = null;
        ground = null;
    }
    
    public void pasteOverwrite(Field other) {
        this.hovering = other.hovering;
        this.obstacle = other.obstacle;
        this.lying = other.lying;
        this.ground = other.ground;
        this.rooms = other.rooms;
    }
    
    public void paste(Field other) {
        if(other.hovering != null)
            this.hovering = other.hovering;
        
        if(other.obstacle != null)
            this.obstacle = other.obstacle;
        
        if(other.lying != null)
            this.lying = other.lying;
        
        if(other.ground != null)
            this.ground = other.ground;
        
        if(other.rooms != null)
            this.rooms = other.rooms;
    }
    
    public void undo(Field other) {
        if(other.hovering != null)
            this.hovering = null;
        
        if(other.obstacle != null)
            this.obstacle = null;
        
        if(other.lying != null)
            this.lying = null;
        
        if(other.ground != null)
            this.ground = null;
        
        if(other.rooms != null)
            this.rooms = null;
    }
    
    public Set<Thing> getNonGroundThings() {
        Set<Thing> s = new HashSet<>();
        if(hovering != null)
            s.add(hovering);
        if(obstacle != null)
            s.add(obstacle);
        if(lying != null)
            s.add(lying);
        return s;
    }
    
    public boolean hasNonGround() {
        return !getNonGroundThings().isEmpty();
    }
    
    public boolean has(Resource layer) {
        if(layer.equals(TBox.hovering) && hovering != null)
            return true;
        
        if(layer.equals(TBox.obstacle) && obstacle != null)
            return true;
        
        if(layer.equals(TBox.lying) && lying != null)
            return true;
        
        if(layer.equals(TBox.ground) && ground != null)
            return true;
        
        return false;
    }
    
    //idea is to use has(Resource layer) 
    @Deprecated
    public boolean hasObstacle() {
        return obstacle != null;
    }

    public boolean hasHovering() {
        return hovering != null;
    }
    
    public boolean hasThingWithType(Resource type) {
        Set<Resource> types = new HashSet<>();
        
        if(hovering != null) 
            types.add(hovering.type);
            
        if(obstacle != null)
            types.add(obstacle.type);
        
        if(lying != null)
            types.add(lying.type);
        
        if(ground != null)
            types.add(ground.type);
        
        return types.contains(type);
    }
    
    public boolean hasRoom() {
        return rooms != null;
    }
    
    public boolean reveal(Set<Resource> inRoom) {
        if(hasRoom()) {
            return inRoom.stream().anyMatch(r -> rooms.contains(r));
        }
        return true;
    }
    
    @Override
    public String toString() {
        return "(" + x + "," + y + ")";
    }
    
    public String toStringLong() {
        StringBuilder sb = new StringBuilder();
        sb.append(toString()).append("\n");
        sb.append("    ").append(hovering).append("\n");
        sb.append("    ").append(obstacle).append("\n");
        sb.append("    ").append(lying).append("\n");
        sb.append("    ").append(ground).append("\n");
        sb.append("    ").append(rooms).append("\n");
        return sb.toString();
    }
    
}
