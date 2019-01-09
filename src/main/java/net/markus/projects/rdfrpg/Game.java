package net.markus.projects.rdfrpg;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Consumer;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Model;

/**
 *
 * @author Markus Schr&ouml;der
 */
public class Game {

    //contains all named graphs
    public Dataset dataset;

    //the whole game map
    public GameMap map;
    
    //timer for the in-game time
    public int time;
    public final int dayTime = 5 * 60; // x minutes a game day
    private Timer timer;
    private final long period = 750;
    
    //to invoke e.g. draw event
    private List<GameListener> gameListeners;
    
    //manages all NPCs
    private NpcManager npcmanager;
    
    public Game() {
        dataset = DatasetFactory.create();
        gameListeners = new LinkedList<>();
        npcmanager = new NpcManager(this);
    }
    
    public void start() {
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                timeTick();
            }
        }, 0, period);
        
        //debug
        //npcmanager.step();
    }
    
    public void stop() {
        if(timer != null) {
            timer.cancel();
        }
    }
    
    private void timeTick() {
        
        npcmanager.step();
        
        event(gl -> gl.draw(this));
        
        //counts up ingame time
        
        time++;
        if(time >= dayTime) {
            time = 0;
        }
        
        LocalTime localTime = localTime();
        String timeString = localTime.format(DateTimeFormatter.ISO_LOCAL_TIME);
        
        System.out.println("time=" + time + ", timeString=" + timeString);
    }
    
    public float dayRatio() {
        return time / (float) dayTime;
    }
    
    public LocalTime localTime() {
        int daySeconds = 24 * 60 * 60;
        int currentSeconds = (int) Math.ceil(daySeconds * dayRatio());
        LocalTime localTime = LocalTime.ofSecondOfDay(currentSeconds);
        return localTime;
    }
    
    public void setLocalTime(LocalTime localTime) {
        time = (int) Math.ceil(dayTime * convertToRatio(localTime));
    }
    
    public static float convertToRatio(LocalTime localTime) {
        int currentSeconds = localTime.toSecondOfDay();
        float daySeconds = 24 * 60 * 60;
        return currentSeconds / daySeconds;
    }
          
    
    public String localTimeString() {
        return localTime().format(DateTimeFormatter.ISO_LOCAL_TIME);
    }
    
    public void printGraphs() {
        Iterator<String> iter = dataset.listNames();
        while(iter.hasNext()) {
            String n = iter.next();
            
            System.out.println(n);
            Model m = dataset.getNamedModel(n);
            
            m.write(System.out, "TTL");
            System.out.println();
        }
    }

    public List<GameListener> getGameListeners() {
        return gameListeners;
    }
    
    public void event(Consumer<GameListener> gls) {
        getGameListeners().forEach(gls);
    }
    
}
