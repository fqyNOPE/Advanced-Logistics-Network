package headless;

import arc.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.core.GameState.*;
import mindustry.ctype.*;
import mindustry.entities.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.environment.*;

/**
 * Verifies the UnitFactory-style production:
 * - first drone takes 60s (3600 ticks) of progress,
 * - base power scales drone speedMultiplier,
 * - removing power stops production and parks drones (speedMultiplier 0).
 */
public class TestDriver implements ApplicationListener{
    static final int BASE_X = 20, BASE_Y = 24;
    static final int POWER_X = 19, POWER_Y = 24;
    static final int SUPPLY_X = 15, SUPPLY_Y = 24;
    static final int REQ_X = 25, REQ_Y = 24;

    static final int CONFIGURE = 300;
    static final int CUT_POWER = 4500;
    static final int RESTORE_POWER = 5500;
    static final int END = 6500;

    int ticks = 0;
    boolean setup = false;
    Team team;
    UnitType botType;

    @Override
    public void update(){
        ticks++;

        if(!setup){
            setup = true;
            setupWorld();
            return;
        }

        if(ticks == CONFIGURE){
            System.out.println("[test] request wants copper");
            buildAt(REQ_X, REQ_Y).configure(Items.copper);
        }

        if(ticks == CUT_POWER){
            System.out.println("[test] CUT POWER (remove power source)");
            Vars.world.tile(POWER_X, POWER_Y).setBlock(Blocks.air, team);
        }

        if(ticks == RESTORE_POWER){
            System.out.println("[test] RESTORE POWER");
            Vars.world.tile(POWER_X, POWER_Y).setBlock(Blocks.powerSource, team);
        }

        if(ticks % 300 == 0){
            status();
        }

        if(ticks >= END){
            status();
            System.out.println("[test] DONE");
            System.exit(0);
        }
    }

    void setupWorld(){
        team = Vars.state.rules.defaultTeam;
        botType = (UnitType)Vars.content.getByName(ContentType.unit, "logistics-network-logistics-bot");

        Vars.world.resize(48, 48);
        for(int x = 0; x < 48; x++){
            for(int y = 0; y < 48; y++){
                Tile t = new Tile(x, y);
                t.setFloor(((Floor)Blocks.stone));
                Vars.world.tiles.set(x, y, t);
            }
        }
        Vars.world.beginMapLoad();
        Vars.world.endMapLoad();
        Vars.state.set(State.playing);
        Vars.state.rules.waves = false;
        Vars.state.rules.unitCap = 10;

        place(POWER_X, POWER_Y, Blocks.powerSource);
        place(BASE_X, BASE_Y, block("logistics-base"));
        place(SUPPLY_X, SUPPLY_Y, block("logistics-supply-point"));
        place(REQ_X, REQ_Y, block("logistics-request-point"));

        buildAt(SUPPLY_X, SUPPLY_Y).items.add(Items.copper, 500);

        System.out.println("[test] world set up: base + power + supply(500 cu) + request");
    }

    void place(int x, int y, Block block){
        Vars.world.tile(x, y).setBlock(block, team);
    }

    Block block(String name){
        return Vars.content.getByName(ContentType.block, "logistics-network-" + name);
    }

    Building buildAt(int x, int y){
        Tile t = Vars.world.tile(x, y);
        return t == null ? null : t.build;
    }

    void status(){
        Building base = buildAt(BASE_X, BASE_Y);
        int bots = 0;
        StringBuilder sb = new StringBuilder();
        for(Unit u : Groups.unit){
            if(u.type == botType && u.team == team){
                bots++;
                sb.append(" [d" + u.id + " speedMul=" + fmt(u.speedMultiplier) + " (" + (int)u.x + "," + (int)u.y + ")]");
            }
        }
        float progress = -1f;
        try{
            ClassLoader cl = (ClassLoader)Vars.mods.list().first().loader;
            Class<?> bb = Class.forName("logistics.blocks.LogisticsBase$LogisticsBaseBuild", true, cl);
            progress = bb.getField("progress").getFloat(base);
        }catch(Throwable t){
            sb.append(" [progReflectErr]");
        }
        System.out.println("[status] t=" + ticks
            + " power=" + fmt(base == null ? -1 : base.power.status)
            + " progress=" + fmt(progress)
            + " getCap=" + Units.getCap(team)
            + " countType=" + team.data().countType(botType)
            + " bots=" + bots
            + " reqCu=" + buildAt(REQ_X, REQ_Y).items.get(Items.copper)
            + sb);
    }

    static String fmt(float f){
        return String.format(java.util.Locale.ROOT, "%.2f", f);
    }
}
