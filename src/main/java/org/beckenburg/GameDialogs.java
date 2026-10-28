package org.beckenburg;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/** Synchronous game prompts hosted in the existing window's glass pane. */
public final class GameDialogs extends JOptionPane {
    private GameDialogs(){}
    public static int showOptionDialog(Component parent,Object message,String title,int optionType,
            int messageType,Icon icon,Object[] choices,Object initial){
        if(!SwingUtilities.isEventDispatchThread())throw new IllegalStateException("Game prompts require the UI thread");
        JRootPane root=SwingUtilities.getRootPane(parent);
        if(root==null)throw new IllegalStateException("Game prompt requires an attached game surface");
        Object[] values=choices==null?new Object[]{"OK"}:choices;
        int[] result={CLOSED_OPTION};
        SecondaryLoop loop=Toolkit.getDefaultToolkit().getSystemEventQueue().createSecondaryLoop();
        Component oldGlass=root.getGlassPane();boolean wasVisible=oldGlass.isVisible();
        Component oldFocus=KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        JButton oldDefault=root.getDefaultButton();
        JPanel overlay=new JPanel(new BorderLayout()){
            protected void paintComponent(Graphics g){g.setColor(new Color(4,7,12,220));g.fillRect(0,0,getWidth(),getHeight());}
        };
        overlay.setOpaque(false);overlay.setName("game.dialog");
        overlay.addMouseListener(new MouseAdapter(){});overlay.addMouseMotionListener(new MouseMotionAdapter(){});
        JButton[] buttons=new JButton[values.length];
        for(int i=0;i<values.length;i++){
            final int index=i;buttons[i]=PsxHorrorUi.button(String.valueOf(values[i]));
            buttons[i].addActionListener(e->{result[0]=index;loop.exit();});
        }
        JComponent content;
        if(message instanceof JComponent component)content=component;
        else{JTextArea text=new JTextArea(String.valueOf(message));text.setEditable(false);text.setLineWrap(true);
            text.setWrapStyleWord(true);text.setForeground(new Color(235,224,201));text.setBackground(new Color(27,34,42));
            text.setMargin(new Insets(15,15,15,15));content=PsxMenuScreen.scroll(text);}
        JPanel screen=PsxMenuScreen.screen("game.dialog.screen",title,"",content,buttons);
        overlay.add(screen,BorderLayout.CENTER);overlay.setBorder(BorderFactory.createEmptyBorder(24,24,24,24));
        overlay.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE,0),"cancel");
        overlay.getActionMap().put("cancel",new AbstractAction(){public void actionPerformed(ActionEvent e){loop.exit();}});
        Window window=SwingUtilities.getWindowAncestor(parent);
        WindowAdapter closing=new WindowAdapter(){public void windowClosed(WindowEvent e){loop.exit();}};
        if(window!=null)window.addWindowListener(closing);
        root.setGlassPane(overlay);overlay.setVisible(true);
        JButton focus=buttons[0];for(int i=0;i<values.length;i++)if(java.util.Objects.equals(values[i],initial))focus=buttons[i];
        root.setDefaultButton(focus);focus.requestFocusInWindow();
        try{loop.enter();}finally{
            overlay.setVisible(false);root.setGlassPane(oldGlass);oldGlass.setVisible(wasVisible);root.setDefaultButton(oldDefault);
            if(window!=null)window.removeWindowListener(closing);
            if(oldFocus!=null)oldFocus.requestFocusInWindow();
        }
        return result[0];
    }
    public static int showConfirmDialog(Component parent,Object message,String title,int options,int type){
        Object[] choices=options==YES_NO_OPTION?new String[]{"YES","NO"}:
            options==YES_NO_CANCEL_OPTION?new String[]{"YES","NO","CANCEL"}:new String[]{"OK","CANCEL"};
        return showOptionDialog(parent,message,title,options,type,null,choices,choices[choices.length-1]);
    }
    public static void showMessageDialog(Component parent,Object message,String title,int type){
        showOptionDialog(parent,message,title,DEFAULT_OPTION,type,null,new String[]{"OK"},"OK");
    }
    public static Object showInputDialog(Component parent,Object message,String title,int type,Icon icon,Object[] values,Object initial){
        JComboBox<Object> choice=new JComboBox<>(values);choice.setSelectedItem(initial);
        JPanel content=new JPanel(new BorderLayout(0,10));content.setOpaque(false);
        content.add(PsxMenuScreen.label(String.valueOf(message)),BorderLayout.NORTH);content.add(choice,BorderLayout.CENTER);
        return showConfirmDialog(parent,content,title,OK_CANCEL_OPTION,type)==OK_OPTION?choice.getSelectedItem():null;
    }
}
