package org.beckenburg;
/** Historical host-lifetime helper. All player interfaces use Castle-embedded LanLobbyPanel. */
public final class LanLobbyWindow {
    private LanLobbyWindow(){}
    static boolean keepRunningLocalHost(LanHost host,LanClient.ResumeTicket ticket){return LanLobbyPanel.keepRunningLocalHost(host,ticket);}
}
