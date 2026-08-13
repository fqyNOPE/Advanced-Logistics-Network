package logistics.blocks;

import arc.*;
import arc.audio.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.blocks.power.*;
import mindustry.world.meta.*;

import logistics.*;

/**
 * The heart of the logistics network. Drone production works like a vanilla
 * {@code UnitFactory} (progress bar, power efficiency scales the build speed,
 * producing sound + build animation on completion) but requires no items.
 * <p>
 * - draws 800 power/s straight from the grid (no internal battery),
 * - takes {@link #produceTime} seconds to produce one drone (also for the first),
 * - each drone is bound to the base that produced it: destroying the base kills
 *   its drones, and the base's power level scales its drones' movement speed,
 * - producing stops while at the drone cap / unit cap.
 */
public class LogisticsBase extends PowerBlock{
    /** Unit type spawned as the logistics robot. */
    public UnitType botType;
    /** Maximum number of drones this base keeps alive. */
    public int maxBots = 1;
    /** Ticks to produce one drone (including the first one). 60 seconds = 60 * 60. */
    public float produceTime = 3600f;
    /** Sound played when a drone is produced, like vanilla unit factories. */
    public Sound createSound = Sounds.unitCreate;

    public LogisticsBase(String name){
        super(name);
        size = 3;
        hasItems = false;
        destructible = true;
        envEnabled = Env.any;
        //1200 power per second, like a heavy factory
        consumePower(1200f / 60f);
        addBar("progress", (LogisticsBaseBuild e) -> new Bar("bar.progress", Pal.ammo, e::progress));
        addBar("logistics-bots", e -> new Bar(
            () -> Core.bundle.format("bar.logistics-bots", ((LogisticsBaseBuild)e).bots.size, maxBots),
            () -> Pal.accent,
            () -> ((LogisticsBaseBuild)e).bots.size / (float)maxBots
        ));
    }

    public class LogisticsBaseBuild extends Building{
        /** IDs of the drones owned by this base. */
        public Seq<Integer> bots = new Seq<>();
        /** Production progress, 0..1 like vanilla unit factories. */
        public float progress;

        @Override
        public float progress(){
            return progress;
        }

        @Override
        public void onProximityAdded(){
            super.onProximityAdded();
            LogisticsNetwork.registerBase(self());
        }

        @Override
        public void onProximityRemoved(){
            super.onProximityRemoved();
            LogisticsNetwork.unregisterBase(self());
        }

        @Override
        public void updateTile(){
            if(botType == null) return;

            pruneBots();
            boolean atCap = bots.size >= maxBots
                || (botType.useUnitCap && team.data().countType(botType) >= Units.getCap(team));

            //like UnitFactory: progress only advances while powered, scaled by
            //power efficiency (edelta); it freezes (does not revert) when unpowered
            if(!atCap() && powered()){
                progress += edelta() / produceTime;
            }

            if(progress >= 1f){
                progress %= 1f;
                spawnBot();
            }
        }

        /** Whether the base currently has power (any amount above zero). */
        public boolean powered(){
            return power != null && power.status > 0f;
        }

        /** Whether the base is at its drone cap (own cap or global unit cap). */
        boolean atCap(){
            return bots.size >= maxBots
                || (botType != null && botType.useUnitCap && team.data().countType(botType) >= Units.getCap(team));
        }

        void spawnBot(){
            float ang = Mathf.random(360f);
            float dist = Mathf.random(size * 4f);
            Unit u = botType.spawn(team, x + Angles.trnsx(ang, dist), y + Angles.trnsy(ang, dist));
            bots.add(u.id);

            //bind the drone to this base: it remembers its home, and its speed
            //will follow this base's power level
            if(u.controller() instanceof LogisticsAI ai){
                ai.homePos = tile.pos();
            }

            //same completion effects as vanilla unit factories
            createSound.at(this, 1f + Mathf.range(0.06f), 1f);
            Events.fire(new UnitCreateEvent(u, self()));
        }

        @Override
        public void draw(){
            super.draw();
            //build animation like vanilla unit factories; hidden when
            //unpowered or at cap
            if(botType != null && powered() && !atCap() && progress > 0f && progress < 1f){
                Draw.draw(Layer.blockOver, () -> Drawf.construct(self(), botType, rotdeg() - 90f, progress, 1f, Time.time));
            }
        }

        void pruneBots(){
            for(int i = bots.size - 1; i >= 0; i--){
                Unit u = Groups.unit.getByID(bots.get(i));
                if(u == null || !u.isValid() || u.dead()){
                    bots.remove(i);
                }
            }
        }

        /** Kills every drone owned by this base. */
        void killBots(){
            for(int id : bots){
                Unit u = Groups.unit.getByID(id);
                if(u != null && u.isValid()){
                    u.kill();
                }
            }
            bots.clear();
        }

        @Override
        public void onDestroyed(){
            super.onDestroyed();
            killBots();
        }

        @Override
        public void onRemoved(){
            super.onRemoved();
            killBots();
        }

        @Override
        public byte version(){
            return 1;
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.f(progress);
            write.s(bots.size);
            for(int id : bots){
                write.i(id);
            }
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            if(revision < 1){
                //old format: only bot ids
                bots.clear();
                short n = read.s();
                for(int i = 0; i < n; i++){
                    bots.add(read.i());
                }
                progress = 0f;
            }else{
                progress = read.f();
                bots.clear();
                short n = read.s();
                for(int i = 0; i < n; i++){
                    bots.add(read.i());
                }
            }
        }
    }
}
