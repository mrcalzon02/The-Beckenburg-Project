package org.beckenburg;
import javax.swing.*;
/** Test-only outer game window. Production lobbies cannot allocate a Window. */
final class LanPanelTestHarness extends JFrame {
    final LanLobbyPanel lobby;
    LanPanelTestHarness(){
        super("Embedded lobby test harness");
        lobby=new LanLobbyPanel(active->{setContentPane(active);revalidate();},null,null);
        setContentPane(lobby);setSize(1020,680);setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }
    @Override public void dispose(){lobby.close();super.dispose();}
}
