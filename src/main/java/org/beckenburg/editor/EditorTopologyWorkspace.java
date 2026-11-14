package org.beckenburg.editor;

import java.awt.*;
import javax.swing.*;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

/** Model-editor workspace chrome. Other authoring modules own their layouts; controls retain native host actions and state. */
public final class EditorTopologyWorkspace extends JPanel {
    private final JComponent tools,inspector;
    private final JTabbedPane browsers;
    private final JSplitPane canvas,bounds;
    private boolean focused;
    private boolean restoreInspector=true,restoreBrowsers;
    public EditorTopologyWorkspace(JComponent viewport,JComponent tools,JComponent inspector,JTabbedPane browsers,int inspectorWidth){
        super(new BorderLayout(4,4));this.tools=tools;this.inspector=inspector;this.browsers=browsers;
        viewport.setMinimumSize(new Dimension(240,180));inspector.setMinimumSize(new Dimension(220,100));
        inspector.setPreferredSize(new Dimension(inspectorWidth,500));browsers.setMinimumSize(new Dimension(100,90));
        canvas=new JSplitPane(JSplitPane.VERTICAL_SPLIT,viewport,browsers);canvas.setResizeWeight(1);canvas.setContinuousLayout(true);canvas.setBorder(null);
        bounds=new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,canvas,inspector);bounds.setResizeWeight(1);bounds.setContinuousLayout(true);bounds.setBorder(null);
        add(tools,BorderLayout.WEST);add(bounds,BorderLayout.CENTER);setBrowsersVisible(false);
    }
    public void setInspectorVisible(boolean visible){inspector.setVisible(visible);bounds.setRightComponent(visible?inspector:null);bounds.setDividerSize(visible?5:0);if(!visible)bounds.setDividerLocation(1.0);if(visible)SwingUtilities.invokeLater(()->bounds.setDividerLocation(Math.max(240,bounds.getWidth()-inspector.getPreferredSize().width)));revalidate();}
    public void setBrowsersVisible(boolean visible){browsers.setVisible(visible);canvas.setBottomComponent(visible?browsers:null);canvas.setDividerSize(visible?5:0);if(!visible)canvas.setDividerLocation(1.0);if(visible)SwingUtilities.invokeLater(()->canvas.setDividerLocation(Math.max(180,canvas.getHeight()-180)));revalidate();}
    public boolean inspectorVisible(){return inspector.isVisible();}
    public boolean browsersVisible(){return browsers.isVisible();}
    public void toggleFocus(){
        if(!focused){restoreInspector=inspector.isVisible();restoreBrowsers=browsers.isVisible();setInspectorVisible(false);setBrowsersVisible(false);tools.setVisible(false);}
        else{setInspectorVisible(restoreInspector);setBrowsersVisible(restoreBrowsers);tools.setVisible(true);}focused=!focused;revalidate();repaint();
    }
    public boolean focused(){return focused;}
    public boolean savedInspectorVisible(){return focused?restoreInspector:inspectorVisible();}
    public boolean savedBrowsersVisible(){return focused?restoreBrowsers:browsersVisible();}
    public static JMenu menu(String title,AbstractButton... commands){
        JMenu menu=new JMenu(title);
        for(var command:commands){var item=new JMenuItem(command.getText());item.setToolTipText(command.getToolTipText());item.addActionListener(e->command.doClick());menu.add(item);}
        menu.addMenuListener(new MenuListener(){public void menuSelected(MenuEvent e){for(int i=0;i<commands.length;i++)menu.getItem(i).setEnabled(commands[i].isEnabled());}public void menuDeselected(MenuEvent e){}public void menuCanceled(MenuEvent e){}});
        return menu;
    }
    public static void shortcut(JMenu menu,int item,int key){menu.getItem(item).setAccelerator(KeyStroke.getKeyStroke(key,java.awt.event.InputEvent.CTRL_DOWN_MASK));}
    public static JPanel column(JComponent... components){JPanel panel=new JPanel();panel.setLayout(new BoxLayout(panel,BoxLayout.Y_AXIS));for(var component:components){component.setAlignmentX(LEFT_ALIGNMENT);component.setMaximumSize(new Dimension(Integer.MAX_VALUE,component.getPreferredSize().height));panel.add(component);panel.add(Box.createVerticalStrut(6));}return panel;}
    public static JPanel form(Object... pairs){
        JPanel panel=new JPanel(new GridBagLayout());GridBagConstraints c=new GridBagConstraints();c.insets=new Insets(4,4,4,4);c.anchor=GridBagConstraints.WEST;c.fill=GridBagConstraints.HORIZONTAL;
        for(int i=0;i<pairs.length;i+=2){c.gridy=i/2;c.gridx=0;c.weightx=0;panel.add(new JLabel((String)pairs[i]),c);c.gridx=1;c.weightx=1;panel.add((JComponent)pairs[i+1],c);}return panel;
    }
}
