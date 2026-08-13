package logistics;

import arc.math.*;
import arc.struct.*;
import arc.util.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.*;

import logistics.blocks.*;

import static mindustry.Vars.*;

/**
 * AI for logistics drones, inspired by Factorio logistic robots:
 * - empty drones pick up items from supply/storage points only when a request
 *   point actually needs them (gap >= {@link #requestThreshold}),
 * - loaded drones fly to the nearest request point wanting that item, or to a
 *   storage point with space (only as a fallback when no request point can
 *   take the cargo), and deposit it.
 * Drones only accept new jobs while their home base has power.
 */
public class LogisticsAI extends AIController{
    public static float moveRange = 9f, moveSmooth = 30f;
    public static float transferRange = 12f;
    public static float retargetTime = 20f;
    /**
     * A request point only counts as "needing" an item when its gap is at
     * least this large. Without this hysteresis, a request point that is
     * constantly consumed would trigger a drone round-trip for every single
     * missing item, making drones oscillate between the storage point and the
     * request point.
     */
    public static int requestThreshold = 40;

    /** Tile position of the current target building. */
    public int targetPos = -1;
    /** Item this drone is picking up / carrying. */
    public @Nullable Item targetItem;
    /** Tile position of the request point this drone is assigned to; -1 = none. */
    public int claimPos = -1;
    /** Tile position of the source this drone is picking up from; -1 = none. */
    public int claimSupplyPos = -1;
    /** Tile position of the base that produced this drone; -1 = unknown (after loading a save), re-derived from the producing base's bot list. */
    public int homePos = -1;

    /** Whether a request point actually needs more of this item (hysteresis-aware). */
    static boolean needsItem(Building b, Item item){
        return b.items.get(item) <= b.getMaximumAccepted(item) - requestThreshold;
    }

    @Override
    public void updateMovement(){
        if(!unit.isValid() || unit.dead()) return;

        Building base = findBase();
        boolean powered = base != null && base.power != null && base.power.status > 0f;

        //the producing base's power level scales this drone's movement speed:
        //full power = full speed, weak power = slow, no power = parked
        float speed = powered ? Mathf.clamp(base.power.status, 0f, 1f) : 0f;
        //overdriving the base boosts its drones at a 2:1 ratio:
        //100% overdrive (timeScale 2.0) -> +50% drone speed
        if(base != null){
            float overdrive = Math.max(base.timeScale(), 1f);
            speed *= 1f + (overdrive - 1f) * 0.5f;
        }
        unit.speedMultiplier = speed;

        if(!unit.hasItem()){
            // ---- empty: look for a pickup job ----
            if(!powered){
                targetPos = -1;
                releaseClaim();
                moveToBase(base);
                return;
            }

            //only re-pick a job when idle: never switch jobs mid-flight to a source,
            //otherwise drones oscillate between supply points when a request point's
            //gap flickers around the threshold
            if(targetPos == -1 && timer.get(timerTarget, retargetTime)){
                findPickupJob();
            }

            if(targetPos == -1){
                moveToBase(base);
                return;
            }

            Building src = world.build(targetPos);
            if(src == null || !isProvider(src) || !src.items.has(targetItem)){
                targetPos = -1;
                releaseClaim();
                moveToBase(base);
                return;
            }

            moveTo(src, moveRange, moveSmooth);

            if(unit.within(src, transferRange) && timer.get(timerTarget2, 15f)){
                //take only as much as request points can actually hold - never more
                int needed = neededByTargets(targetItem);
                int take = Math.min(unit.maxAccepted(targetItem), src.items.get(targetItem));
                take = Math.min(take, needed);
                if(take > 0){
                    Call.takeItems(src, targetItem, take, unit);
                    //cargo secured - free up the source slot for other drones
                    releaseSupplyClaim();
                    //now find a destination for the carried items
                    findDeliverJob(targetItem);
                    if(targetPos == -1){
                        unit.clearItem();
                        releaseClaim();
                    }
                }else{
                    //demand vanished while flying here; look for a new job
                    targetPos = -1;
                    releaseClaim();
                }
            }
        }else{
            // ---- carrying: deliver ----
            if(targetPos == -1 || timer.get(timerTarget, retargetTime)){
                findDeliverJob(unit.item());
                if(targetPos == -1){
                    //nowhere to put it; drop the cargo
                    unit.clearItem();
                    releaseClaim();
                    return;
                }
            }

            Building dest = world.build(targetPos);
            if(dest == null || !isDest(dest, unit.item())){
                targetPos = -1;
                releaseClaim();
                return;
            }

            moveTo(dest, moveRange, moveSmooth);

            if(unit.within(dest, transferRange) && timer.get(timerTarget2, 15f)){
                int amount = Math.min(unit.stack.amount, dest.acceptStack(unit.item(), unit.stack.amount, unit));
                amount = Math.min(amount, dest.getMaximumAccepted(unit.item()) - dest.items.get(unit.item()));
                if(amount > 0){
                    Call.transferItemTo(unit, unit.item(), amount, unit.x, unit.y, dest);
                }
                if(!unit.hasItem()){
                    targetPos = -1;
                    releaseClaim();
                }
            }
        }
    }

    @Override
    public void removed(Unit u){
        if(u != null){
            if(claimPos != -1){
                LogisticsNetwork.release(u.team, claimPos, u.id);
                claimPos = -1;
            }
            if(claimSupplyPos != -1){
                LogisticsNetwork.release(u.team, claimSupplyPos, u.id);
                claimSupplyPos = -1;
            }
        }
    }

    /**
     * How many drones this request point should get, based on its item gap:
     * one drone per full load (gap 40 -> 1 drone, gap 80 -> 2 drones, ...).
     */
    int allowedDrones(Building b, Item item){
        int gap = b.getMaximumAccepted(item) - b.items.get(item);
        return Math.max(1, (gap + unit.type.itemCapacity - 1) / unit.type.itemCapacity);
    }

    /** Releases this drone's claim on its assigned request point and source, if any. */
    void releaseClaim(){
        if(claimPos != -1){
            LogisticsNetwork.release(unit.team, claimPos, unit.id);
            claimPos = -1;
        }
        releaseSupplyClaim();
    }

    /** Releases this drone's claim on its assigned source only. */
    void releaseSupplyClaim(){
        if(claimSupplyPos != -1){
            LogisticsNetwork.release(unit.team, claimSupplyPos, unit.id);
            claimSupplyPos = -1;
        }
    }

    /** Picks a job for an empty drone: the nearest request point whose item is available somewhere. */
    void findPickupJob(){
        targetItem = null;
        targetPos = -1;

        //job stickiness: keep serving the request point we already claimed while
        //it still needs the item - avoids switching jobs after every single trip
        if(claimPos != -1){
            Building own = world.build(claimPos);
            if(own != null && own.block instanceof RequestPoint){
                RequestPoint.RequestBuild req = (RequestPoint.RequestBuild)own;
                Item want = req.requestItem;
                if(want != null && needsItem(own, want)){
                    Building src = findSourceFree(want);
                    if(src != null){
                        targetItem = want;
                        targetPos = src.tile.pos();
                        //re-claim the new source if it changed
                        if(claimSupplyPos != src.tile.pos()){
                            releaseSupplyClaim();
                            claimSupplyPos = src.tile.pos();
                            LogisticsNetwork.claim(unit.team, claimSupplyPos, unit.id);
                        }
                        return;
                    }
                }
            }
            releaseClaim();
        }

        //nearest request point that needs its requested item and still has free slots,
        //both for the request gap (gap-based) and for the source stock (stock-based)
        Building best = null;
        Building bestSrc = null;
        float bestDst = Float.MAX_VALUE;
        for(Building b : LogisticsNetwork.get(LogisticsNetwork.requesters, unit.team)){
            if(!b.isValid() || !(b.block instanceof RequestPoint) || b.items == null) continue;
            RequestPoint.RequestBuild req = (RequestPoint.RequestBuild)b;
            Item want = req.requestItem;
            if(want == null || !needsItem(b, want)) continue;
            Building src = findSourceFree(want);
            if(src == null) continue;
            int gapDrones = allowedDrones(b, want);
            int srcDrones = Math.max(1, (src.items.get(want) + unit.type.itemCapacity - 1) / unit.type.itemCapacity);
            int allowed = Math.min(gapDrones, srcDrones);
            if(LogisticsNetwork.countClaims(unit.team, b.tile.pos()) >= allowed) continue;
            float d = unit.dst(b);
            if(d < bestDst){
                bestDst = d;
                best = b;
                bestSrc = src;
            }
        }
        if(best != null && bestSrc != null){
            targetItem = ((RequestPoint.RequestBuild)best).requestItem;
            //the job destination is the requester, but the drone flies to the SOURCE first
            targetPos = bestSrc.tile.pos();
            claimPos = best.tile.pos();
            claimSupplyPos = bestSrc.tile.pos();
            LogisticsNetwork.claim(unit.team, claimPos, unit.id);
            LogisticsNetwork.claim(unit.team, claimSupplyPos, unit.id);
        }
        //no active request -> the drone stays idle. Items are never moved from a supply
        //point into a storage point on the drone's own initiative.
    }

    /**
     * Finds the destination for the carried item: the request point this drone
     * claimed first, then another request point wanting the item, then a storage
     * point with space as a fallback.
     */
    void findDeliverJob(Item item){
        targetPos = -1;

        //1. deliver to the request point this drone claimed, if it still wants the item
        if(claimPos != -1){
            Building own = world.build(claimPos);
            if(own != null && own.block instanceof RequestPoint && ((RequestPoint.RequestBuild)own).requestItem == item && needsItem(own, item)){
                targetPos = claimPos;
                return;
            }
            //claimed point no longer needs it - release the claim
            releaseClaim();
        }

        //2. nearest request point wanting this item that still has a free drone slot
        Building best = null;
        float bestDst = Float.MAX_VALUE;
        for(Building b : LogisticsNetwork.get(LogisticsNetwork.requesters, unit.team)){
            if(!b.isValid() || !(b.block instanceof RequestPoint) || b.items == null) continue;
            if(((RequestPoint.RequestBuild)b).requestItem != item) continue;
            if(!needsItem(b, item)) continue;
            if(LogisticsNetwork.countClaims(unit.team, b.tile.pos()) >= allowedDrones(b, item)) continue;
            float d = unit.dst(b);
            if(d < bestDst){
                bestDst = d;
                best = b;
            }
        }
        if(best != null){
            targetPos = best.tile.pos();
            claimPos = best.tile.pos();
            LogisticsNetwork.claim(unit.team, claimPos, unit.id);
            return;
        }

        //3. storage point fallback: only used when no request point can take the cargo
        best = null;
        bestDst = Float.MAX_VALUE;
        for(Building b : LogisticsNetwork.get(LogisticsNetwork.storages, unit.team)){
            if(!b.isValid() || b.items == null || b.items.total() >= b.block.itemCapacity) continue;
            if(b.items.get(item) >= b.getMaximumAccepted(item)) continue;
            float d = unit.dst(b);
            if(d < bestDst){
                bestDst = d;
                best = b;
            }
        }
        if(best != null){
            targetPos = best.tile.pos();
        }
    }

    /** Total demand for an item across all request points (storage points do NOT count as demand). */
    int neededByTargets(Item item){
        int needed = 0;
        for(Building b : LogisticsNetwork.get(LogisticsNetwork.requesters, unit.team)){
            if(!b.isValid() || !(b.block instanceof RequestPoint)) continue;
            if(((RequestPoint.RequestBuild)b).requestItem != item) continue;
            needed += Math.max(b.getMaximumAccepted(item) - b.items.get(item), 0);
        }
        return needed;
    }

    /**
     * Nearest source of this item with a free drone slot. Storage points (yellow
     * chests) are preferred; a source only counts as free while fewer than
     * ceil(stock / capacity) drones are already picking up from it, so a supply
     * point with 40 items only ever gets 1 drone, 80 items -> 2 drones, etc.
     */
    Building findSourceFree(Item item){
        //1. drain storage points first
        Building best = findSourceIn(LogisticsNetwork.get(LogisticsNetwork.storages, unit.team), item, true);
        if(best != null) return best;

        //2. fall back to supply points
        return findSourceIn(LogisticsNetwork.get(LogisticsNetwork.providers, unit.team), item, false);
    }

    Building findSourceIn(Seq<Building> list, Item item, boolean storage){
        Building best = null;
        float bestDst = Float.MAX_VALUE;
        for(Building b : list){
            if(!b.isValid() || b.items == null || b.items.get(item) <= 0) continue;
            if(storage != (b.block instanceof StoragePoint)) continue;
            //stock-based dispatch: at most ceil(stock / capacity) drones per source
            int allowed = Math.max(1, (b.items.get(item) + unit.type.itemCapacity - 1) / unit.type.itemCapacity);
            if(LogisticsNetwork.countClaims(unit.team, b.tile.pos()) >= allowed) continue;
            float d = unit.dst(b);
            if(d < bestDst){
                bestDst = d;
                best = b;
            }
        }
        return best;
    }

    boolean isProvider(Building b){
        return b.block instanceof SupplyPoint || b.block instanceof StoragePoint;
    }

    boolean isDest(Building b, Item item){
        if(!b.isValid() || b.items == null) return false;
        if(b.block instanceof RequestPoint){
            return ((RequestPoint.RequestBuild)b).requestItem == item && needsItem(b, item);
        }
        if(b.block instanceof StoragePoint){
            return b.items.total() < b.block.itemCapacity && b.items.get(item) < b.getMaximumAccepted(item);
        }
        return false;
    }

    /**
     * The base that produced this drone. homePos is authoritative when set;
     * otherwise (e.g. after loading a save, where AI fields are not persisted)
     * the producing base is found by scanning bases for the one whose bot list
     * contains this drone's id - the binding therefore never changes with the
     * drone's position. Falls back to the nearest base only for drones that
     * belong to no base at all.
     */
    Building findBase(){
        if(homePos != -1){
            Building b = world.build(homePos);
            if(b != null && b.isValid() && b.block instanceof LogisticsBase && b.team == unit.team){
                return b;
            }
        }

        //the producing base's bot list is persisted across saves, so it is the
        //authoritative owner even when homePos was lost; cache it back into homePos
        for(Building b : LogisticsNetwork.get(LogisticsNetwork.bases, unit.team)){
            if(!b.isValid() || !(b.block instanceof LogisticsBase)) continue;
            if(((LogisticsBase.LogisticsBaseBuild)b).bots.contains(unit.id)){
                homePos = b.tile.pos();
                return b;
            }
        }

        //no owning base found - last resort, nearest base
        Building best = null;
        float bestDst = Float.MAX_VALUE;
        for(Building b : LogisticsNetwork.get(LogisticsNetwork.bases, unit.team)){
            if(!b.isValid()) continue;
            float d = unit.dst(b);
            if(d < bestDst){
                bestDst = d;
                best = b;
            }
        }
        return best;
    }

    void moveToBase(@Nullable Building base){
        if(base != null){
            moveTo(base, 12f, moveSmooth);
        }
    }
}
