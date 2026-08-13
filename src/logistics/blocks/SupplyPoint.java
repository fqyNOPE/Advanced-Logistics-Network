package logistics.blocks;

/**
 * Supply point - the "passive provider chest" (red box) of the network.
 * Items placed inside (by belts, drones or the player) are picked up by
 * logistics drones and delivered to request points / storage points.
 */
public class SupplyPoint extends LogisticsChest{
    public SupplyPoint(String name){
        super(name);
    }
}
