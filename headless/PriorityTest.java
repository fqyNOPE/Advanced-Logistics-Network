package headless;

import arc.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.core.GameState.*;
import mindustry.ctype.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.environment.*;

/**
 * Verifies request-point priority (strict tiering):
 * two request points want the same item - one NEAR with priority 0 (low),
 * one FAR with priority 9 (high). The first drone load must go to the FAR
 * high-priority box, and the NEAR low-priority box must stay empty while
 * the high-priority one still needs the item (no distance shortcut, no
 * preemption of the in-flight drone).
 */
public class PriorityTest implements ApplicationListener{
    static final int BASE_X = 20, BASE_Y = 24;
    static final int POWER_X = 18, POWER_Y = 24; //18: adjacent-left of the 3x3 base (19-21,23-25); 19 was inside the footprint and got deleted
    static final int SUPPLY_X = 15, SUPPLY_Y = 24;
    static final int REQ_NEAR_X = 25, REQ_NEAR_Y = 24;
    static final int REQ_FAR_X = 33, REQ_FAR_Y = 24;

    static final int CONFIGURE = 300;
    static final int END = 5200;

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
            System.out.println("[test] configure: near req (25,24) priority=0, far req (33,24) priority=9, both want copper");
            buildAt(REQ_NEAR_X, REQ_NEAR_Y).configure(Items.copper);
            buildAt(REQ_FAR_X, REQ_FAR_Y).configure(Items.copper);
            setPriority(REQ_NEAR_X, REQ_NEAR_Y, 0);
            setPriority(REQ_FAR_X, REQ_FAR_Y, 9);
            //sanity: read back what was set
            System.out.println("[test] readback priNear=" + getPriority(REQ_NEAR_X, REQ_NEAR_Y)
                + " priFar=" + getPriority(REQ_FAR_X, REQ_FAR_Y));
        }

        if(ticks % 300 == 0){
            status();
        }

        if(ticks >= END){
            status();
            int nearCu = buildAt(REQ_NEAR_X, REQ_NEAR_Y).items.get(Items.copper);
            int farCu = buildAt(REQ_FAR_X, REQ_FAR_Y).items.get(Items.copper);
            boolean ok = farCu >= 40 && nearCu == 0;
            System.out.println("[test] ASSERT far(high-priority) served first: farCu=" + farCu
                + " nearCu=" + nearCu + " -> " + (ok ? "PASS" : "FAIL"));
            System.out.println(ok ? "[test] DONE-PASS" : "[test] DONE-FAIL");
            System.exit(ok ? 0 : 1);
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
        place(REQ_NEAR_X, REQ_NEAR_Y, block("logistics-request-point"));
        place(REQ_FAR_X, REQ_FAR_Y, block("logistics-request-point"));

        buildAt(SUPPLY_X, SUPPLY_Y).items.add(Items.copper, 500);

        System.out.println("[test] world set up: base + power + supply(500 cu) + reqNear(25,24) + reqFar(33,24)");
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

    void setPriority(int x, int y, int value){
        try{
            Object build = buildAt(x, y);
            build.getClass().getField("priority").setInt(build, value);
        }catch(Throwable t){
            System.out.println("[test] setPriority FAILED at (" + x + "," + y + "): " + t);
        }
    }

    int getPriority(int x, int y){
        try{
            Object build = buildAt(x, y);
            return build.getClass().getField("priority").getInt(build);
        }catch(Throwable t){
            return -999;
        }
    }

    void status(){
        Building base = buildAt(BASE_X, BASE_Y);
        float progress = -1f;
        try{
            ClassLoader cl = (ClassLoader)Vars.mods.list().first().loader;
            Class<?> bb = Class.forName("logistics.blocks.LogisticsBase$LogisticsBaseBuild", true, cl);
            progress = bb.getField("progress").getFloat(base);
        }catch(Throwable t){
            //ignore
        }
        StringBuilder sb = new StringBuilder();
        for(Unit u : Groups.unit){
            if(u.type == botType && u.team == team){
                sb.append(" [d" + u.id + " (" + (int)u.x + "," + (int)u.y + ")");
                try{
                    Object c = u.controller();
                    if(c != null){
                        sb.append(" claim=" + c.getClass().getField("claimPos").getInt(c)
                            + " target=" + c.getClass().getField("targetPos").getInt(c));
                    }
                }catch(Throwable t){
                    sb.append(" [reflErr]");
                }
                sb.append("]");
            }
        }
        System.out.println("[status] t=" + ticks
            + " basePower=" + fmt(base == null ? -1 : base.power == null ? -2 : base.power.status)
            + " progress=" + fmt(progress)
            + " priNear=" + getPriority(REQ_NEAR_X, REQ_NEAR_Y)
            + " priFar=" + getPriority(REQ_FAR_X, REQ_FAR_Y)
            + " reqNearCu=" + buildAt(REQ_NEAR_X, REQ_NEAR_Y).items.get(Items.copper)
            + " reqFarCu=" + buildAt(REQ_FAR_X, REQ_FAR_Y).items.get(Items.copper)
            + " supplyCu=" + buildAt(SUPPLY_X, SUPPLY_Y).items.get(Items.copper)
            + sb);
    }

    static String fmt(float f){
        return String.format(java.util.Locale.ROOT, "%.2f", f);
    }
}
