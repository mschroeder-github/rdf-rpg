package net.markus.projects.rdfrpg;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import static java.util.stream.Collectors.toList;
import org.apache.commons.io.IOUtils;

/**
 *
 * @author Markus Schr&ouml;der
 */
public class NameGenerator {
    
    private Random rnd;
    
    private List<Entry<String, Integer>> surnamesSorted;
    private int surnamesIndex = 0;
    
    private List<String> males;
    private List<String> females;
    
    public NameGenerator(Random rnd) {
        this.rnd = rnd;
        
        Map<String, Integer> surnames = new HashMap<>();
        try {
            for(String line : IOUtils.toString(NameGenerator.class.getResourceAsStream("/names/en_surname.csv"), "UTF-8").split("\r\n")) {
                String[] split = line.split(";");
                surnames.put(properCase(split[0]), Integer.parseInt(split[1]));
            }
            
            males = new ArrayList<>();
            for(String line : IOUtils.toString(NameGenerator.class.getResourceAsStream("/names/firstname_male.txt"), "UTF-8").split("\n")) {
                males.add(properCase(line.split(" ")[0].toLowerCase()));
            }
            
            females = new ArrayList<>();
            for(String line : IOUtils.toString(NameGenerator.class.getResourceAsStream("/names/firstname_female.txt"), "UTF-8").split("\n")) {
                females.add(properCase(line.split(" ")[0].toLowerCase()));
            }
            
        } catch (Exception ex) {            
            throw new RuntimeException(ex);
        }
        
        surnamesSorted = 
        surnames.entrySet().stream().sorted((o1, o2) -> {
            return Integer.compare(o2.getValue(), o1.getValue());
        }).collect(toList());
    }
    
    public String familyName() {
        return surnamesSorted.get(surnamesIndex++).getKey();
    }
    
    public String maleName() {
        return Rnd.randomlyRemove(males, rnd);
    }
    
    public String femaleName() {
        return Rnd.randomlyRemove(females, rnd);
    }
    
    private String properCase(String s) {
        return String.valueOf(s.charAt(0)).toUpperCase() + s.substring(1);
    }

}
