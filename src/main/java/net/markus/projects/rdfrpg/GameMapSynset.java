package net.markus.projects.rdfrpg;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import static java.util.stream.Collectors.toList;

/**
 *
 * @author Markus Schr&ouml;der
 */
public class GameMapSynset extends ArrayList<GameMap> {
    
    Map<GameMap, Integer> gm2count;

    public GameMapSynset() {
        gm2count = new HashMap<>();
    }

    public GameMapSynset(Collection<? extends GameMap> c) {
        super(c);
        gm2count = new HashMap<>();
    }
    
    public GameMap randomlySelect(Random rnd) {
        return get(rnd.nextInt(size()));
    }
    
    public GameMap randomlyRemove(Random rnd) {
        GameMap gm = randomlySelect(rnd);
        remove(gm);
        return gm;
    }
    
    public GameMap randomlyRemoveInfrequent(Random rnd) {
        //if no count is given use normal method
        if(gm2count.isEmpty()) {
            return randomlyRemove(rnd);
        }
        
        List<Entry<GameMap, Integer>> e = gm2count.entrySet().stream().sorted((o1, o2) -> {
            int cmp = Integer.compare(o1.getValue(), o2.getValue());
            //same infrequent
            if(cmp == 0) {
                //-1 or 1
                return -1 + (rnd.nextInt(2) * 2);
            }
            return cmp;
        }).collect(toList());
        
        GameMap gm = e.get(0).getKey();
        remove(gm);
        gm2count.remove(gm);
        return gm;
    }
    
    public double avgCount() {
        if(gm2count.isEmpty())
            return 0;
        
        return gm2count.values().stream().mapToDouble(i -> i).average().getAsDouble();
    }
    
}
