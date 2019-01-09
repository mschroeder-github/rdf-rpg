package net.markus.projects.rdfrpg;

import java.io.File;
import java.io.StringWriter;
import java.util.List;
import java.util.Set;
import net.markus.projects.rdfrpg.RandomRectsGenerator.RandomRects;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;

/**
 *
 * @author Markus Schr&ouml;der
 */
public class Family {
    
    Model model;
    RandomRects randomRects;
    GameMap house;
    List<Set<Resource>> livingCouples;
    List<String> familyNames;
    Direction entranceOrientation;
    
    public Family() {
        
    }

    @Override
    public String toString() {
        StringWriter sw = new StringWriter();
        model.write(sw, "TTL");
        return sw.toString();
    }
    
    public void save() {
        File img = new File("family/" + familyNames.toString() + ".png");
        img.getParentFile().mkdir();
        house.saveImage(img);
    }
    
}
