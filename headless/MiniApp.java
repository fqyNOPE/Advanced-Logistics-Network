package headless;

import arc.*;
import arc.files.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.struct.*;
import arc.util.*;

/**
 * Minimal headless "backend" replicating arc.backend.headless.HeadlessApplication
 * (which is not bundled in the desktop release jar).
 */
public class MiniApp implements Application{
    public final Seq<ApplicationListener> listeners = new Seq<>();
    public final java.util.Queue<Runnable> posts = new java.util.concurrent.ConcurrentLinkedQueue<>();
    public final Thread mainThread = Thread.currentThread();
    public boolean running = true;

    public MiniApp(ApplicationListener main){
        listeners.add(main);
    }

    public void start(){
        Core.app = this;
        Core.files = new HeadlessFiles();
        Core.settings = new Settings();
        Core.graphics = new MiniGraphics();
        Core.audio = new arc.audio.Audio();
        // Core.atlas is intentionally left null, same as the real headless server:
        // Content.load() (region loading) is never invoked headlessly.

        Time.setDeltaProvider(() -> 1f);

        try{
            for(ApplicationListener l : listeners){
                l.init();
            }
        }catch(Throwable t){
            t.printStackTrace();
            System.exit(1);
        }

        while(running){
            Time.update();

            Runnable r;
            while((r = posts.poll()) != null){
                try{
                    r.run();
                }catch(Throwable t){
                    t.printStackTrace();
                }
            }

            for(ApplicationListener l : listeners){
                try{
                    l.update();
                }catch(Throwable t){
                    t.printStackTrace();
                    System.exit(1);
                }
            }

            try{
                Thread.sleep(5);
            }catch(InterruptedException ignored){
            }
        }
    }

    @Override public Seq<ApplicationListener> getListeners(){ return listeners; }
    @Override public ApplicationType getType(){ return ApplicationType.headless; }
    @Override public String getClipboardText(){ return ""; }
    @Override public void setClipboardText(String text){}
    @Override public void post(Runnable r){ posts.add(r); }
    @Override public void exit(){ running = false; }
    @Override public Thread getMainThread(){ return mainThread; }
    @Override public boolean isOnMainThread(){ return true; }
    @Override public boolean isDesktop(){ return false; }
    @Override public boolean isHeadless(){ return true; }
    @Override public void dispose(){}
}
