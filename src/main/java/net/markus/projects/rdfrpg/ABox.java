package net.markus.projects.rdfrpg;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.vocabulary.RDF;

/**
 *
 * @author Markus Schr&ouml;der
 */
public class ABox {
    
    public static final String ns="a:";
    
    //ages
    static final int oldBegin = 75;
    static final int oldEnd = 85;

    static final int midBegin = 30;
    static final int midEnd = 50;

    static final int kidBegin = 6;
    
    protected static final Resource resource( String local )
        { return ResourceFactory.createResource(ns + local ); }

    protected static final Property property( String local )
        { return ResourceFactory.createProperty(ns + local ); }
    
    public static Resource person(String firstname, String lastname, int age, Resource gender, Model model) {
        Resource person = resource(firstname + "-" + lastname + "-" + age);
        model.add(person, RDF.type, TBox.person);
        model.add(person, TBox.firstname, firstname);
        model.add(person, TBox.lastname, lastname);
        model.add(person, TBox.gender, gender);
        model.addLiteral(person, TBox.age, age);
        
        if(age >= oldBegin) {
            model.add(person, TBox.agePhase, TBox.personOld);
        } else if(age >= midBegin) {
            model.add(person, TBox.agePhase, TBox.personMiddle);
        } else if(age >= 13) {
            model.add(person, TBox.agePhase, TBox.personTeen);
        } else {
            model.add(person, TBox.agePhase, TBox.personYoung);
        }
        
        return person;
    }
}
