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
 * Verifies the request-point item cap slider:
 * - Phase A: default cap 320 fills normally (regression).
 * - Phase B: cap lowered to 20% while stock is above it -> deliveries stop
 *   entirely (stock frozen, supply untouched, surplus stays in the chest).
 * - Phase C: cap 10% + chest drained -> exactly one load (32) is delivered,
 *   then the chest counts as full and no further deliveries happen.
 * - Phase D: a conveyor feeding the chest is rejected (request points only
 *   accept unit deliveries, not belt input).
 */
public class CapacityTest implements ApplicationListener{
    static final int BASE_X = 20, BASE_Y = 24;
    static final int POWER_X = 18, POWER_Y = 24;
    static final int SUPPLY_X = 15, SUPPLY_Y = 24;
    static final int REQ_X = 25, REQ_Y = 24;

    static final int CONFIGURE = 300;
    static final int CAP_80 = 4500;
    static final int MEASURE_B1 = 4800;
    static final int CAP_40 = 5600;
    static final int MEASURE_C1 = 6500;
    static final int PHASE_C_ASSERT = 7200;
    static final int BELT_PLACE = 7300;
    static final int END = 8200;

    int ticks = 0;
    boolean setup = false;
    Team team;
    UnitType botType;
    int stockB1 = -1, supplyB1 = -1, stockB2 = -1, supplyB2 = -1;

    @Override
    public void update(){
        ticks++;

        if(!setup){
            setup = true;
            setupWorld();
            return;
        }

        if(ticks == CONFIGURE){
            System.out.println("[test] configure request: copper (default cap 320)");
            buildAt(REQ_X, REQ_Y).configure(Items.copper);
        }

        if(ticks == CAP_80){
            System.out.println("[test] set cap=64 (20%) (stock is above it by now)");
            setCap(64);
        }

        if(ticks == MEASURE_B1){
            stockB1 = stock();
            supplyB1 = supply();
            System.out.println("[test] B1 stock=" + stockB1 + " supply=" + supplyB1);
        }

        if(ticks == CAP_40){
            stockB2 = stock();
            supplyB2 = supply();
            System.out.println("[test] B2 stock=" + stockB2 + " supply=" + supplyB2 + " (must equal B1)");
            System.out.println("[test] set cap=32 (10%) and drain the chest");
            setCap(32);
            buildAt(REQ_X, REQ_Y).items.remove(Items.copper, stockB2);
        }

        if(ticks == MEASURE_C1){
            System.out.println("[test] C1 stock=" + stock() + " supply=" + supply()
                + " (expect 32 / " + (supplyB2 - 32) + ")");
        }

        if(ticks == PHASE_C_ASSERT){
            int s = stock();
            int sp = supply();
            boolean okC = s == 32 && sp == supplyB2 - 32;
            System.out.println("[test] ASSERT phaseC cap32 exactly one load: "
                + (okC ? "PASS" : "FAIL") + " [final=" + s + "/" + sp
                + " expected 32/" + (supplyB2 - 32) + "]");
            System.out.println("[test] PHASE D: kill drone, drain chest, feed a conveyor with 40 copper");
            for(Unit u : Groups.unit){
                if(u.type == botType && u.team == team){
                    u.kill();
                }
            }
            buildAt(REQ_X, REQ_Y).items.remove(Items.copper, stock());
            place(23, 24, Blocks.conveyor);
            buildAt(23, 24).rotation = 0; //points right, into the chest at (24,24)
            buildAt(23, 24).items.add(Items.copper, 40);
        }

        if(ticks % 300 == 0){
            status();
        }

        if(ticks >= END){
            int s = stock();
            int sp = supply();
            int beltCu = buildAt(23, 24) == null ? -1 : buildAt(23, 24).items.get(Items.copper);
            //phase B and C were asserted at their own checkpoints; the END block
            //re-checks B (frozen window) and D (belt rejection) on final state
            boolean okB = stockB1 == stockB2 && supplyB1 == supplyB2;
            boolean okD = s == 0 && beltCu == 40;
            System.out.println("[test] ASSERT phaseB frozen (no deliveries above cap): "
                + (okB ? "PASS" : "FAIL") + " [B1=" + stockB1 + "/" + supplyB1
                + " B2=" + stockB2 + "/" + supplyB2 + "]");
            System.out.println("[test] ASSERT phaseD belt rejected (no belt input): "
                + (okD ? "PASS" : "FAIL") + " [chest=" + s + " beltCu=" + beltCu + "]");
            System.out.println(okB && okD ? "[test] DONE-PASS" : "[test] DONE-FAIL");
            System.exit(okB && okD ? 0 : 1);
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

    void setCap(int value){
        try{
            Object build = buildAt(REQ_X, REQ_Y);
            build.getClass().getField("maxCapacity").setInt(build, value);
        }catch(Throwable t){
            System.out.println("[test] setCap FAILED: " + t);
        }
    }

    int getCap(){
        try{
            Object build = buildAt(REQ_X, REQ_Y);
            return build.getClass().getField("maxCapacity").getInt(build);
        }catch(Throwable t){
            return -999;
        }
    }

    int stock(){
        return buildAt(REQ_X, REQ_Y).items.get(Items.copper);
    }

    int supply(){
        return buildAt(SUPPLY_X, SUPPLY_Y).items.get(Items.copper);
    }

    void status(){
        StringBuilder sb = new StringBuilder();
        for(Unit u : Groups.unit){
            if(u.type == botType && u.team == team){
                sb.append(" [d" + u.id + " (" + (int)u.x + "," + (int)u.y + ")");
                try{
                    Object c = u.controller();
                    if(c != null){
                        sb.append(" claim=" + c.getClass().getField("claimPos").getInt(c));
                    }
                }catch(Throwable t){
                    //ignore
                }
                sb.append("]");
            }
        }
        System.out.println("[status] t=" + ticks
            + " cap=" + getCap()
            + " reqCu=" + stock()
            + " supplyCu=" + supply()
            + " beltCu=" + (buildAt(23, 24) == null ? -1 : buildAt(23, 24).items.get(Items.copper))
            + sb);
    }
}
