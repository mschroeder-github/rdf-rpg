package net.markus.projects.rdfrpg;

/**
 *
 * @author Markus Schr&ouml;der
 */
public class GameFrame extends javax.swing.JFrame implements GameListener {

    private Game game;
    
    public GameFrame(Game game) {
        this.game = game;
        initComponents();
        jScrollPaneGame.getViewport().addChangeListener((e) -> {
            jPanelGame.repaint();
        });
        game.getGameListeners().add(this);
        setSize(500, 500);
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPaneGame = new javax.swing.JScrollPane();
        jPanelGame = new GamePanel(game);

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jPanelGame.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jPanelGameMouseClicked(evt);
            }
        });

        javax.swing.GroupLayout jPanelGameLayout = new javax.swing.GroupLayout(jPanelGame);
        jPanelGame.setLayout(jPanelGameLayout);
        jPanelGameLayout.setHorizontalGroup(
            jPanelGameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 490, Short.MAX_VALUE)
        );
        jPanelGameLayout.setVerticalGroup(
            jPanelGameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 451, Short.MAX_VALUE)
        );

        jScrollPaneGame.setViewportView(jPanelGame);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPaneGame, javax.swing.GroupLayout.DEFAULT_SIZE, 370, Short.MAX_VALUE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPaneGame, javax.swing.GroupLayout.DEFAULT_SIZE, 341, Short.MAX_VALUE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jPanelGameMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jPanelGameMouseClicked
        game.map.cursor.x = evt.getPoint().x / game.map.size;
        game.map.cursor.y = evt.getPoint().y / game.map.size;
        jPanelGame.repaint();
        
        if(game.map.in(game.map.cursor.x, game.map.cursor.y)) {
            Field f = game.map.getField(game.map.cursor.x, game.map.cursor.y);
            System.out.println(f.toStringLong());
        }
    }//GEN-LAST:event_jPanelGameMouseClicked

    public static void showGUI(Game game) {
        java.awt.EventQueue.invokeLater(() -> {
            new GameFrame(game).setVisible(true);
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel jPanelGame;
    private javax.swing.JScrollPane jScrollPaneGame;
    // End of variables declaration//GEN-END:variables

    @Override
    public void draw(Game game) {
        java.awt.EventQueue.invokeLater(() -> {
            jPanelGame.repaint();
        });
        
        this.setTitle(game.localTimeString());
    }
}
