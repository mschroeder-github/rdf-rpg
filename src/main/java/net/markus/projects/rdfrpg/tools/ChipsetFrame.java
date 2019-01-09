package net.markus.projects.rdfrpg.tools;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.JPanel;
import net.markus.projects.rdfrpg.GamePanel;
import net.markus.projects.rdfrpg.Resources;

/**
 *
 * @author Markus Schr&ouml;der
 */
public class ChipsetFrame extends javax.swing.JFrame {

    private Image image;
    private String chipset;
    private Rectangle rect;
    
    public ChipsetFrame() {
        initComponents();
        jComboBoxChipset.removeAllItems();
        for(String chipset : Resources.chipsets) {
            jComboBoxChipset.addItem(chipset);
        }
    }
    
    public void draw(Graphics g) {
        g.setColor(Color.black);
        g.fillRect(0, 0, getWidth(), getHeight());
        
        g.drawImage(image, 0, 0, null);
        
        if(rect != null) {
            g.setColor(Color.blue);
            g.drawRect(rect.x, rect.y, rect.width, rect.height);
        }
    }
    
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanelChipset = new JPanel() {
            public void paintComponent(Graphics g) {
                draw(g);
            }
        };
        jComboBoxChipset = new javax.swing.JComboBox<>();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Chipset Frame");

        jPanelChipset.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseMoved(java.awt.event.MouseEvent evt) {
                jPanelChipsetMouseMoved(evt);
            }
        });
        jPanelChipset.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jPanelChipsetMouseClicked(evt);
            }
        });

        javax.swing.GroupLayout jPanelChipsetLayout = new javax.swing.GroupLayout(jPanelChipset);
        jPanelChipset.setLayout(jPanelChipsetLayout);
        jPanelChipsetLayout.setHorizontalGroup(
            jPanelChipsetLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );
        jPanelChipsetLayout.setVerticalGroup(
            jPanelChipsetLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 315, Short.MAX_VALUE)
        );

        jComboBoxChipset.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        jComboBoxChipset.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBoxChipsetActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanelChipset, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jComboBoxChipset, 0, 383, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addComponent(jComboBoxChipset, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanelChipset, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jComboBoxChipsetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBoxChipsetActionPerformed
        chipset = (String) jComboBoxChipset.getSelectedItem();
        if(chipset == null)
            return;
        
        String res = "/chipset/"+ chipset +".png";
        try {
            image = ImageIO.read(GamePanel.class.getResourceAsStream(res));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        
        jPanelChipset.repaint();
    }//GEN-LAST:event_jComboBoxChipsetActionPerformed

    private void jPanelChipsetMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jPanelChipsetMouseClicked
        int x = evt.getX() / 16;
        int y = evt.getY() / 16;
        
        StringBuilder sb = new StringBuilder();
        sb.append("t: rdfs:subClassOf t:chip ;\n");
        sb.append("    rdfs:comment \"\" ;\n");
        sb.append("    t:resource <res:/chipset/"+chipset+".png> ;\n");
        sb.append("    t:resourceLocation \""+x+","+y+"\" .\n");
        
        StringSelection selection = new StringSelection(sb.toString());
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(selection, selection);
        System.out.println(sb.toString());
    }//GEN-LAST:event_jPanelChipsetMouseClicked

    private void jPanelChipsetMouseMoved(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jPanelChipsetMouseMoved
        int x = (evt.getX() / 16) * 16;
        int y = (evt.getY() / 16) * 16;
        
        rect = new Rectangle(x, y, 16, 16);
        
        jPanelChipset.repaint();
    }//GEN-LAST:event_jPanelChipsetMouseMoved

    public static void showGUI() {
        java.awt.EventQueue.invokeLater(() -> {
            new ChipsetFrame().setVisible(true);
        });
    }

    public static void main(String[] args) {
        ChipsetFrame.showGUI();
    }
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JComboBox<String> jComboBoxChipset;
    private javax.swing.JPanel jPanelChipset;
    // End of variables declaration//GEN-END:variables
}
