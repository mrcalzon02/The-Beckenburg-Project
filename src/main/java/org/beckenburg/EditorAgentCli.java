package org.beckenburg;

import org.beckenburg.editor.EditorAgentService;
import org.beckenburg.editor.EditorHistory;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import static org.beckenburg.editor.EditorAgentProtocol.json;
import static org.beckenburg.editor.EditorAgentProtocol.run;
import java.util.Map;

/** Opt-in, process-local headless adapter. No listener, shell or script execution.
 * Tab-separated requests on stdin; one JSON response per line on stdout.
 */
public final class EditorAgentCli {
    private EditorAgentCli(){}
    public static void main(String[] args)throws Exception{
        PrintStream responses=new PrintStream(System.out,true,StandardCharsets.UTF_8);
        // Both channels use UTF-8 independently of Java17 platform defaults.
        System.setErr(new PrintStream(System.err,true,StandardCharsets.UTF_8));
        System.setOut(System.err);
        int code;
        try{code=open(args,responses);}
        catch(Exception|LinkageError failure){
            Throwable cause=failure;while(cause.getCause()!=null&&cause.getCause()!=cause)cause=cause.getCause();
            responses.println(json(Map.of("ok",false,"phase","open","error",cause.toString())));responses.flush();
            failure.printStackTrace(System.err);code=1;
        }
        if(code!=0)System.exit(code);
    }
    private static int open(String[] args,PrintStream responses)throws Exception{
        if(args.length>0&&args[0].equalsIgnoreCase("MESH")){
            if(args.length!=2&&(args.length!=4||!args[2].equals("--face")))
                throw new IllegalArgumentException("Usage: EditorAgentCli MESH document.btop [--face native-id]");
            System.setProperty("java.awt.headless","true");
            var host=new HeadlessMaterialEditor(Path.of(args[1]),args.length==4?Long.parseLong(args[3]):null);
            return run(new EditorAgentService<>(host,host.project().createHistory(60)),
                new BufferedReader(new InputStreamReader(System.in,StandardCharsets.UTF_8)),responses);
        }
        if(args.length<2||args.length>3||(args.length==3&&!args[2].equals("--recover-previous")))
            throw new IllegalArgumentException("Usage: EditorAgentCli CASTLE|HOUSE|SCHOOL|... project-home [--recover-previous]");
        System.setProperty("user.home",Path.of(args[1]).toAbsolutePath().normalize().toString());
        System.setProperty("java.awt.headless","true");
        if(args.length==3){
            if(args[0].equalsIgnoreCase("CASTLE"))CastleLevelEdits.recoverSavedDocument();
            else LevelAuthoringSession.recoverSavedDocument(HouseMap.Site.valueOf(args[0].toUpperCase(java.util.Locale.ROOT)));
        }
        Thread owner=Thread.currentThread();
        Runnable access=()->{if(Thread.currentThread()!=owner)throw new IllegalStateException("Wrong runtime owner thread");};
        SceneRenderer renderer=new SceneRenderer();renderer.setRenderSize(560,350);renderer.lamp=false;
        EditorAgentService<?> service;
        if(args[0].equalsIgnoreCase("CASTLE")){
            var scene=CastleLevelEdits.current();var project=new CastleEditorProject(scene);
            double[] spawn=scene.spawn();renderer.x=spawn[0];renderer.z=spawn[1];renderer.eyeHeight=1.62;
            renderer.yaw=spawn[2];renderer.pitch=-.12;
            service=new EditorAgentService<>(new CastleEditorAgent(project,new CastleHubMap(),renderer,access),
                new EditorHistory<>(project,60));
        }else{
            var site=HouseMap.Site.valueOf(args[0].toUpperCase(java.util.Locale.ROOT));
            var session=new LevelAuthoringSession(site);
            var spawn=session.level().room(0).spawn();
            renderer.x=spawn.x();renderer.z=spawn.z();renderer.eyeHeight=spawn.y()+1.62;renderer.pitch=-.12;
            service=new EditorAgentService<>(new LevelEditorAgent(session,()->new HouseMap(site,session.level()),renderer,access),
                new EditorHistory<>(session,60));
        }
        return run(service,new BufferedReader(new InputStreamReader(System.in,StandardCharsets.UTF_8)),responses);
    }
}
