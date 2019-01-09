package net.markus.projects.rdfrpg.tools;

import java.awt.Color;
import java.awt.Dimension;
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
public class CharsetFrame extends javax.swing.JFrame {

    private Image image;
    private String charset;
    private Rectangle rect;
    
    public static Dimension charsetSize = new Dimension(72, 128);
    public static Dimension charSize = new Dimension(72 / 3, 128 / 4);
    
    public CharsetFrame() {
        initComponents();
        jComboBoxCharset.removeAllItems();
        for(String cs : Resources.charsets) {
            jComboBoxCharset.addItem(cs);
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

        jPanelCharset = new JPanel() {
            public void paintComponent(Graphics g) {
                draw(g);
            }
        };
        jComboBoxCharset = new javax.swing.JComboBox<>();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Charset Frame");

        jPanelCharset.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseMoved(java.awt.event.MouseEvent evt) {
                jPanelCharsetMouseMoved(evt);
            }
        });
        jPanelCharset.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jPanelCharsetMouseClicked(evt);
            }
        });

        javax.swing.GroupLayout jPanelCharsetLayout = new javax.swing.GroupLayout(jPanelCharset);
        jPanelCharset.setLayout(jPanelCharsetLayout);
        jPanelCharsetLayout.setHorizontalGroup(
            jPanelCharsetLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );
        jPanelCharsetLayout.setVerticalGroup(
            jPanelCharsetLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 315, Short.MAX_VALUE)
        );

        jComboBoxCharset.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        jComboBoxCharset.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBoxCharsetActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanelCharset, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jComboBoxCharset, 0, 383, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addComponent(jComboBoxCharset, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanelCharset, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jComboBoxCharsetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBoxCharsetActionPerformed
        charset = (String) jComboBoxCharset.getSelectedItem();
        if(charset == null)
            return;
        
        String res = "/charset/"+ charset +".png";
        try {
            image = ImageIO.read(GamePanel.class.getResourceAsStream(res));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        
        jPanelCharset.repaint();
    }//GEN-LAST:event_jComboBoxCharsetActionPerformed

    private void jPanelCharsetMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jPanelCharsetMouseClicked
        int x = evt.getX() / charsetSize.width;
        int y = evt.getY() / charsetSize.height;
        
        StringBuilder sb = new StringBuilder();
        sb.append("t: rdfs:subClassOf t:char ;\n");
        sb.append("    rdfs:comment \"\" ;\n");
        sb.append("    t:resource <res:/charset/"+charset+".png> ;\n");
        sb.append("    t:resourceLocation \""+x+","+y+"\" .\n");
        
        StringSelection selection = new StringSelection(sb.toString());
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(selection, selection);
        System.out.println(sb.toString());
    }//GEN-LAST:event_jPanelCharsetMouseClicked

    private void jPanelCharsetMouseMoved(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jPanelCharsetMouseMoved
        int x = (evt.getX() / charsetSize.width) * charsetSize.width;
        int y = (evt.getY() / charsetSize.height) * charsetSize.height;
        
        rect = new Rectangle(x, y, charsetSize.width, charsetSize.height);
        
        jPanelCharset.repaint();
    }//GEN-LAST:event_jPanelCharsetMouseMoved

    public static void showGUI() {
        java.awt.EventQueue.invokeLater(() -> {
            new CharsetFrame().setVisible(true);
        });
    }

    public static void main(String[] args) {
        CharsetFrame.showGUI();
    }
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JComboBox<String> jComboBoxCharset;
    private javax.swing.JPanel jPanelCharset;
    // End of variables declaration//GEN-END:variables
}
