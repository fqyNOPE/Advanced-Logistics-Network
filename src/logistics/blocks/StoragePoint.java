package logistics.blocks;

/**
 * Storage point - the "storage chest" (yellow box) of the network.
 * Drones both take items from it (when a request needs them) and dump
 * excess items into it. Acts as the network's warehouse.
 */
public class StoragePoint extends LogisticsChest{
    public StoragePoint(String name){
        super(name);
    }

    @Override
    public boolean isStorage(){
        return true;
    }
}
