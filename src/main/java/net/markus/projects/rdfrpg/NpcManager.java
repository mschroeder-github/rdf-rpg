package net.markus.projects.rdfrpg;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDFS;

/**
 *
 * @author Markus Schr&ouml;der
 */
public class NpcManager {
    
    private Game game;

    //routine -> lineNumber -> java function to execute a step
    private Map<Resource, Map<Integer, Consumer<NpcContext>>> routines;
    
    public NpcManager(Game game) {
        this.game = game;
        routines = new HashMap<>();
    }
    
    public void step() {
        long begin = System.currentTimeMillis();
        
        //search for all fields having characters
        List<Field> fields = 
        game.map.getFields((f) -> {
            if(f.has(TBox.obstacle)) {
                return TBox.model.contains(f.obstacle.type, RDFS.subClassOf, TBox.tchar);
            }
            return false;
        });
        
        for(Field field : fields) {
          
            String npcUri = field.obstacle.resource.getURI();
            Model npcModel = game.dataset.getNamedModel(npcUri);
            Resource npc = npcModel.getResource(npcUri);
            
            
            List<RDFNode> dailyRoutines = npcModel.listObjectsOfProperty(npc, TBox.dailyRoutine).toList();
            
            Resource nowRoutine = dailyRoutines.stream()
                    .filter(r -> npcModel.getRequiredProperty(r.asResource(), TBox.begin).getFloat() <= game.dayRatio())
                    .sorted((o1, o2) -> {
                return Float.compare(
                        npcModel.getRequiredProperty(o2.asResource(), TBox.begin).getFloat(), 
                        npcModel.getRequiredProperty(o1.asResource(), TBox.begin).getFloat()
                );
            }).map(n -> n.asResource()).findFirst().get();
            
            //TODO execute function
            //TODO check NPC graph and decide the action based on day routine
            
            
            //System.out.println(npcUri + " with " + npcModel.size() + " triples " + nowRoutine + "(" + dailyRoutines.size() + ")");
            
            //System.out.println(field.toStringLong());
            
            
            //TODO remove
            /*
            Path p = game.map.astar(field.toPoint(), new Point(0,0));
            //System.out.println(p);
            
            if(p.isMoveable()) {
                Field dst = p.nextField(game.map);
                
                //if realy free
                if(dst.obstacle == null) {
                    //System.out.println(dst);
                    GameMap.move(field, dst, TBox.obstacle);
                }
            }
            */
            
            //break;
            
            /*
            List<Field> sides = game.map.getAllSides(field.x, field.y);
            sides.removeIf(f -> f.has(TBox.obstacle));
            if(!sides.isEmpty()) {
                Field target = Rnd.randomlySelect(sides, rnd);
                
                //move it
                GameMap.move(field, target, TBox.obstacle);
            }
            */
        }
        
        long end = System.currentTimeMillis();
        long duration = end - begin;
        System.out.println(fields.size() + " NPCs in " + duration + " ms, time=" + game.time);
    }
    
    private class NpcContext {
        //TODO
    }
    
}
