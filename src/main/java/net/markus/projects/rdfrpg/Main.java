package net.markus.projects.rdfrpg;

import java.time.LocalTime;

/**
 *
 * @author Markus Schr&ouml;der
 */
public class Main {
    public static void main(String[] args) {
        normal();
    }
    
    private static void normal() {
        //this is the game
        //there is only one game per player
        //the game contains the map state and the mental world (rdf)
        Game game = new Game();
        
        //generates everything
        GenerateGameContent ggc = new GenerateGameContent(game, 1234);
        ggc.generate(5);
        
        game.setLocalTime(LocalTime.of(12, 0, 0));
        
        //game.map.saveImage(new File("img/full-5.png"));
        
        game.start();
        
        //now the game has a map and the mental world is filled
        //game.printGraphs();
        GameFrame.showGUI(game);
    }
    
}
