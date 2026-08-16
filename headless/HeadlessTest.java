package headless;

import arc.*;
import arc.util.*;
import mindustry.*;
import mindustry.core.*;
import mindustry.mod.*;
import mindustry.net.*;
import mindustry.ui.*;

/**
 * Headless game bootstrap (mirrors the official ServerLauncher flow) plus a
 * scripted test scenario for the Logistics Network mod.
 *
 * Run with the game jar + this harness on the classpath, and the mod jar in
 * <cwd>/config/mods/.
 */
public class HeadlessTest implements ApplicationListener{
    /** Scenario selected on the command line ("priority" or empty for the default test). */
    static String scenario = "";

    public static void main(String[] args){
        scenario = args.length > 0 ? args[0] : "";
        try{
            Log.useColors = false;
            Vars.loadLogger();

            Vars.platform = new Platform(){};
            Vars.net = new Net(Vars.platform.getNet());

            MiniApp app = new MiniApp(new HeadlessTest());
            app.start();
        }catch(Throwable t){
            t.printStackTrace();
            System.exit(1);
        }
    }

    @Override
    public void init(){
        Core.settings.setDataDirectory(Core.files.local("config"));
        Vars.loadLocales = false;
        Vars.headless = true;

        Vars.loadSettings();
        Vars.init();

        UI.loadColors();
        Fonts.loadContentIconsHeadless();

        Vars.content.createBaseContent();
        Vars.mods.loadScripts();
        Vars.content.createModContent();
        Vars.content.init();

        if(Vars.mods.hasContentErrors()){
            Log.err("Content errors detected, aborting.");
            System.exit(1);
        }

        try{
            Vars.bases.load();
        }catch(Throwable t){
            Log.warn("bases.load failed (non-fatal): @", t);
        }

        Core.app.addListener(Vars.logic = new Logic());
        Core.app.addListener(Vars.netServer = new NetServer());
        //scenario selection: "priority" runs the request-point priority test,
        //"capacity" runs the request-point item cap test,
        //anything else runs the default production/power test
        Core.app.addListener("priority".equals(scenario) ? new PriorityTest()
            : "capacity".equals(scenario) ? new CapacityTest() : new TestDriver());

        Vars.mods.eachClass(Mod::init);

        Log.info("[test] bootstrap complete");
    }

    @Override
    public void update(){
        // game logic runs through the listeners added in init()
    }
}
