package net.markus.projects.rdfrpg;

import java.util.Objects;
import java.util.UUID;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;

/**
 *
 * @author Markus Schr&ouml;der
 */
public class Thing {
    
    //can be anything: npc, background, object, item
    
    //a unique identifier to refer to it
    //every thing will have it
    public Resource resource;
    
    //the type: this decides what it is, how it looks
    //every thing will have it
    public Resource type;
    
    //the brain for humans or state for objects
    //stateless things do not have it
    public Resource graph;
    
    //if the thing has a specific direction (e.g. for chars)
    public Direction direction = Direction.Down;

    public Thing(String typeUri) {
        this("a:" + UUID.randomUUID().toString(), typeUri);
    }
    
    public Thing(String resourceUri, String typeUri) {
        this.resource = ResourceFactory.createResource(resourceUri);
        this.type = ResourceFactory.createResource(typeUri);
    }
    
    public Resource setGraph(String graphUri) {
        graph = ResourceFactory.createResource(graphUri);
        return graph;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 17 * hash + Objects.hashCode(this.resource);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Thing other = (Thing) obj;
        if (!Objects.equals(this.resource, other.resource)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return resource + "(" + type + ")" + (graph != null ? "G" : "");
    }
    
    
    
}
