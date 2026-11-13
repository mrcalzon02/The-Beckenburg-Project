package org.beckenburg;

import java.util.concurrent.*;

/** Four sockets: public architecture is shared; hunter tools, evidence, bodies and sound telemetry remain private. */
public final class LanGhostHearingIntegrationTest {
    static void ok(boolean b,String label){if(!b)throw new AssertionError(label);}
    static final class Peer implements AutoCloseable {
        final BlockingQueue<String> inbox=new LinkedBlockingQueue<>();
        final LanClient client;
        Peer(int port,boolean ghost,String name)throws Exception{
            client=new LanClient("127.0.0.1",port,ghost,name,inbox::offer);
        }
        String await(String expected)throws Exception{
            long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(10);
            while(System.nanoTime()<deadline){
                String s=inbox.poll(80,TimeUnit.MILLISECONDS);
                if(s==null)continue;
                if("GHOST".equals(client.seat())&&(s.startsWith("BODY|")
                    ||s.startsWith("TEAMMAP|")||s.startsWith("PROGRESS|")
                    ||s.startsWith("EVIDENCE|")
                    ||s.startsWith("APPARITION|")||s.startsWith("MOTION|")
                    ||s.startsWith("SOUND|")))
                    throw new AssertionError("hunter private state leaked to ghost: "+s);
                if(s.startsWith(expected))return s;
            }
            throw new AssertionError("missing "+expected+" for "+client.seat());
        }
        /** Inspect every real host frame until the next stillness gate opens.
         * Do not use a fixed quiet-time sleep: it could miss a late cue or
         * misclassify a legitimate sound emitted after the host reopens.
         */
        void assertNoSpectralCuesThroughReopen()throws Exception{
            long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(6);
            while(System.nanoTime()<deadline){
                String packet=inbox.poll(80,TimeUnit.MILLISECONDS);
                if(packet==null)continue;
                if(packet.startsWith("GHEAR|")||packet.startsWith("GRIPPLE|")
                        ||packet.startsWith("BODY|")||packet.startsWith("TEAMMAP|"))
                    throw new AssertionError("moving ghost received private hunter telemetry: "+packet);
                if(packet.startsWith("GLISTEN|1|"))return;
            }
            throw new AssertionError("host never reopened hearing after real movement");
        }
        public void close(){client.close();}
    }
    public static void main(String[] args)throws Exception{
        try(LanHost server=new LanHost("127.0.0.1",0);
            Peer a=new Peer(server.port(),false,"Cleric");
            Peer b=new Peer(server.port(),false,"Observer");
            Peer c=new Peer(server.port(),false,"Demonologist");
            Peer g=new Peer(server.port(),true,"Spirit")){
            a.client.ready();b.client.ready();c.client.ready();g.client.ready();
            a.await("LOBBY|3|HUMAN|4|WAITING");a.client.start();a.await("INFO|mission started");
            g.client.viewGhost3D();g.await("ACCEPT|GVIEW3D");g.await("GPOSE|");
            g.await("GLISTEN|1|");
            for(int r:AuthoredLevelNetworkTest.path(new HouseMap(),14,4)){a.client.move(r);a.await("ACCEPT|MOVE|"+r);}
            LanGhostHearing.Event step=LanGhostHearing.decode(g.await("GHEAR|FOOTSTEP|"));
            ok(step.source()==LanGhostPursuit.Sound.FOOTSTEP,"validated movement made sound");
            ok(step.frame().split("\\|").length==5,"GHEAR remains a coarse five-field cue");
            var sourceRipple=GhostSoundRipples.Ripple.decode(g.await("GRIPPLE|FOOTSTEP|"),new HouseMap());
            ok(sourceRipple.kind()==LanGhostPursuit.Sound.FOOTSTEP,
                "Stationary ghost receives a transient positional footstep ripple");
            ok(sourceRipple.frame().split("\\|",-1).length==6,
                "Ripple carries event location, not a hunter seat, health or team telemetry");
            a.client.scan(LanCase.Tool.EMF);a.await("ACCEPT|SCAN");
            ok(LanGhostHearing.decode(g.await("GHEAR|EQUIPMENT|")).source()
                ==LanGhostPursuit.Sound.EQUIPMENT,"legal scanner sent equipment cue");
            ok(GhostSoundRipples.Ripple.decode(g.await("GRIPPLE|EQUIPMENT|"),new HouseMap())
                .kind()==LanGhostPursuit.Sound.EQUIPMENT,
                "The real hunter tool emits a source-anchored sound ring");
            // Emotes and typed communication are physical hunter-world
            // commands, not abstract room-map actions.
            a.client.view3D();a.await("ACCEPT|VIEW3D");
            a.client.emote(HunterEmote.WAVE);
            a.await("ACCEPT|EMOTE|WAVE");
            var gesture=LanGhostHearing.decode(g.await("GHEAR|EMOTE|"));
            ok(gesture.profile().equals(GhostSoundProfile.forSound(
                LanGhostPursuit.Sound.FOOTSTEP)),
                "Host-accepted hunter emote has walking-level strength and duration");
            var gestureRing=GhostSoundRipples.Ripple.decode(g.await("GRIPPLE|EMOTE|"),
                new HouseMap());
            ok(gestureRing.profile().equals(gesture.profile()),
                "Genuine emote yields a transient sound-source ripple");
            a.client.say(LanChat.Channel.LOCAL,"hi");
            a.await("ACCEPT|SAY|LOCAL");
            var shortChat=LanGhostHearing.decode(g.await("GHEAR|CHAT|"));
            var shortRing=GhostSoundRipples.Ripple.decode(g.await("GRIPPLE|CHAT|"),
                new HouseMap());
            ok(shortChat.profile().equals(GhostSoundProfile.chat(2))
                &&shortRing.profile().equals(shortChat.profile()),
                "Short local chat is low-strength and time-bounded");
            Thread.sleep(750); // Six host ticks between accepted text messages.
            String speech="This is a substantially longer investigator message for the ghost";
            a.client.say(LanChat.Channel.RADIO,speech);
            a.await("ACCEPT|SAY|RADIO");
            var longChat=LanGhostHearing.decode(g.await("GHEAR|CHAT|"));
            var longRing=GhostSoundRipples.Ripple.decode(g.await("GRIPPLE|CHAT|"),
                new HouseMap());
            ok(longChat.profile().intensity()>shortChat.profile().intensity()
                &&longChat.profile().durationTicks()>shortChat.profile().durationTicks()
                &&longRing.profile().equals(longChat.profile()),
                "Long radio text affects local source strength without leaking its text");
            // Real 4-socket host/client boundary: while an actual ghost
            // displacement keeps GLISTEN closed, not even a perfectly valid
            // walker, emoter or chatting hunter may disclose a source point.
            // Re-opened stationary hearing must deliver BOTH coarse GHEAR and
            // source-only GRIPPLE, without BODY or chat transcripts.
            g.client.ghostStep(100,0);
            g.await("ACCEPT|GSTEP");g.await("GLISTEN|0|");
            a.client.step(15,0);a.await("ACCEPT|STEP");
            g.assertNoSpectralCuesThroughReopen();
            Thread.sleep(550); // Host footstep cadence is four 100ms ticks.
            a.client.step(-15,0);a.await("ACCEPT|STEP");
            ok(LanGhostHearing.decode(g.await("GHEAR|FOOTSTEP|")).source()
                ==LanGhostPursuit.Sound.FOOTSTEP,"settled ghost resumes genuine footsteps");
            ok(GhostSoundRipples.Ripple.decode(g.await("GRIPPLE|FOOTSTEP|"),
                new HouseMap()).kind()==LanGhostPursuit.Sound.FOOTSTEP,
                "settled ghost resumes the actual walking source ripple");

            g.client.ghostStep(-100,0);
            g.await("ACCEPT|GSTEP");g.await("GLISTEN|0|");
            a.client.emote(HunterEmote.WAVE);a.await("ACCEPT|EMOTE|WAVE");
            g.assertNoSpectralCuesThroughReopen();
            Thread.sleep(950); // Accepted emotes have an eight-tick cooldown.
            a.client.emote(HunterEmote.LAUGH);a.await("ACCEPT|EMOTE|LAUGH");
            ok(LanGhostHearing.decode(g.await("GHEAR|EMOTE|")).source()
                ==LanGhostPursuit.Sound.EMOTE,"settled ghost hears genuine gestures");
            ok(GhostSoundRipples.Ripple.decode(g.await("GRIPPLE|EMOTE|"),
                new HouseMap()).kind()==LanGhostPursuit.Sound.EMOTE,
                "settled ghost sees genuine gesture source");

            g.client.ghostStep(100,0);
            g.await("ACCEPT|GSTEP");g.await("GLISTEN|0|");
            a.client.say(LanChat.Channel.LOCAL,"quiet");
            a.await("ACCEPT|SAY|LOCAL");
            g.assertNoSpectralCuesThroughReopen();
            Thread.sleep(750); // Two accepted chat lines require six ticks.
            a.client.say(LanChat.Channel.LOCAL,"audible again");
            a.await("ACCEPT|SAY|LOCAL");
            var resumedChat=LanGhostHearing.decode(g.await("GHEAR|CHAT|"));
            var resumedRing=GhostSoundRipples.Ripple.decode(g.await("GRIPPLE|CHAT|"),
                new HouseMap());
            ok(resumedChat.source()==LanGhostPursuit.Sound.CHAT
                &&resumedRing.kind()==LanGhostPursuit.Sound.CHAT
                &&resumedChat.profile().equals(resumedRing.profile()),
                "only after stillness can real text generate matched anonymous cues");
            // A ghost cannot forge its own audible source or request
            // another team's live BODY/telemetry frames.
            g.client.send("GRIPPLE|FOOTSTEP|1000|0|1000|20");
            g.await("REJECT|unknown command");
            g.client.send("GHEAR|RITUAL|LEFT|LOUD|20");
            g.await("REJECT|unknown command");
            g.client.send("EMOTE|WAVE|99999");
            g.await("REJECT|hunter world emote required");
            ok(server.running(),"forged cue cannot change mission");
            System.out.println("PASS: four-socket ghost hearing and real source ripples, move-to-mute host control, bounded sound packets, no hunter BODY leakage or forged ripple authority");
        }
    }
}
