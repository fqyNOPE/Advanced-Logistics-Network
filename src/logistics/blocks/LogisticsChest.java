package logistics.blocks;

import arc.struct.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.meta.*;

import logistics.*;

/**
 * Base class of all logistics chests. Chests behave like vaults: they accept
 * items from belts (and drones) and can be unloaded by the player.
 * <p>
 * Roles in the network:
 * <ul>
 * <li>{@link SupplyPoint} - passive provider: drones only take items from it.</li>
 * <li>{@link StoragePoint} - storage chest: drones take items from it and dump excess into it.</li>
 * <li>{@link RequestPoint} - requester chest: drones deliver its configured item to it.</li>
 * </ul>
 */
public abstract class LogisticsChest extends Block{
    public LogisticsChest(String name){
        super(name);
        size = 2;
        hasItems = true;
        solid = true;
        update = false;
        sync = true;
        destructible = true;
        separateItemCapacity = true;
        group = BlockGroup.transportation;
        flags = EnumSet.of(BlockFlag.storage);
        allowResupply = true;
        envEnabled = Env.any;
        drawCached = true;
        drawDynamic = false;
        alwaysUnlocked = true;
    }

    @Override
    public boolean outputsItems(){
        return false;
    }

    /** Whether this chest is a storage point (can receive excess items). */
    public boolean isStorage(){
        return false;
    }

    /** Whether this chest is a request point (can only receive its requested item). */
    public boolean isRequester(){
        return false;
    }

    public class LogisticsChestBuild extends Building{
        @Override
        public void onProximityAdded(){
            super.onProximityAdded();
            if(isRequester()){
                LogisticsNetwork.registerRequester(self());
            }else{
                LogisticsNetwork.registerProvider(self());
                if(isStorage()){
                    LogisticsNetwork.registerStorage(self());
                }
            }
        }

        @Override
        public void onProximityRemoved(){
            super.onProximityRemoved();
            if(isRequester()){
                LogisticsNetwork.unregisterRequester(self());
            }else{
                LogisticsNetwork.unregisterProvider(self());
                if(isStorage()){
                    LogisticsNetwork.unregisterStorage(self());
                }
            }
        }

        @Override
        public boolean acceptItem(Building source, Item item){
            return items.get(item) < getMaximumAccepted(item);
        }

        @Override
        public int getMaximumAccepted(Item item){
            return itemCapacity;
        }
    }
}
