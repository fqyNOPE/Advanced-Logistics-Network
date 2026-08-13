package logistics;

import arc.struct.*;
import mindustry.game.*;
import mindustry.gen.*;

/**
 * Global registry of every logistics building in the world, grouped by team.
 * Buildings register themselves on creation and unregister on removal, so the
 * drone AI can find supply/request/storage points without scanning the world.
 */
public class LogisticsNetwork{
    /** All logistics bases. */
    public static final ObjectMap<Team, Seq<Building>> bases = new ObjectMap<>();
    /** Supply points + storage points (things drones may take items from). */
    public static final ObjectMap<Team, Seq<Building>> providers = new ObjectMap<>();
    /** Storage points only (things drones may dump excess items into). */
    public static final ObjectMap<Team, Seq<Building>> storages = new ObjectMap<>();
    /** Request points only. */
    public static final ObjectMap<Team, Seq<Building>> requesters = new ObjectMap<>();
    /**
     * Building tile positions -> drone ids currently assigned to that building.
     * Used for two purposes:
     * - request point tile pos: how many drones are assigned to refill it (gap-based dispatch),
     * - supply point tile pos: how many drones are allowed to pick up from it (stock-based dispatch).
     */
    public static final ObjectMap<Team, ObjectMap<Integer, Seq<Integer>>> claims = new ObjectMap<>();

    /** Clears all registries. Called on world reset/load; buildings re-register themselves. */
    public static void clear(){
        bases.clear();
        providers.clear();
        storages.clear();
        requesters.clear();
        claims.clear();
    }

    /** Assigns a drone to a request point. */
    public static void claim(Team team, int pos, int unitId){
        ObjectMap<Integer, Seq<Integer>> map = claims.get(team);
        if(map == null){
            map = new ObjectMap<>();
            claims.put(team, map);
        }
        Seq<Integer> seq = map.get(pos);
        if(seq == null){
            seq = new Seq<>(false);
            map.put(pos, seq);
        }
        for(int i = 0; i < seq.size; i++){
            if(seq.get(i) == unitId) return;
        }
        seq.add(unitId);
    }

    /** Releases a drone's assignment to a request point. */
    public static void release(Team team, int pos, int unitId){
        ObjectMap<Integer, Seq<Integer>> map = claims.get(team);
        if(map == null) return;
        Seq<Integer> seq = map.get(pos);
        if(seq == null) return;
        for(int i = seq.size - 1; i >= 0; i--){
            if(seq.get(i) == unitId){
                seq.remove(i);
            }
        }
        if(seq.isEmpty()){
            map.remove(pos);
        }
    }

    /**
     * Number of live drones currently assigned to this request point.
     * Dead drones are pruned while counting.
     */
    public static int countClaims(Team team, int pos){
        ObjectMap<Integer, Seq<Integer>> map = claims.get(team);
        if(map == null) return 0;
        Seq<Integer> seq = map.get(pos);
        if(seq == null) return 0;
        int count = 0;
        for(int i = seq.size - 1; i >= 0; i--){
            Unit u = Groups.unit.getByID(seq.get(i));
            if(u == null || !u.isValid() || u.dead()){
                seq.remove(i);
            }else{
                count++;
            }
        }
        if(seq.isEmpty()){
            map.remove(pos);
        }
        return count;
    }

    public static Seq<Building> get(ObjectMap<Team, Seq<Building>> map, Team team){
        Seq<Building> seq = map.get(team);
        if(seq == null){
            seq = new Seq<>(false);
            map.put(team, seq);
        }
        return seq;
    }

    public static void registerBase(Building b){
        get(bases, b.team).add(b);
    }

    public static void unregisterBase(Building b){
        get(bases, b.team).remove(b);
    }

    public static void registerProvider(Building b){
        get(providers, b.team).add(b);
    }

    public static void unregisterProvider(Building b){
        get(providers, b.team).remove(b);
    }

    public static void registerStorage(Building b){
        get(storages, b.team).add(b);
    }

    public static void unregisterStorage(Building b){
        get(storages, b.team).remove(b);
    }

    public static void registerRequester(Building b){
        get(requesters, b.team).add(b);
    }

    public static void unregisterRequester(Building b){
        get(requesters, b.team).remove(b);
    }
}
