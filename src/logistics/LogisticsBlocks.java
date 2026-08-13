package logistics;

import logistics.blocks.*;
import mindustry.content.*;
import mindustry.type.*;
import mindustry.world.*;

import static mindustry.content.Items.*;

/**
 * All content of the logistics network mod.
 */
public class LogisticsBlocks{
    public static LogisticsBase logisticsBase;
    public static SupplyPoint logisticsSupplyPoint;
    public static RequestPoint logisticsRequestPoint;
    public static StoragePoint logisticsStoragePoint;

    public static void load(){
        logisticsBase = new LogisticsBase("logistics-base");
        logisticsBase.requirements(Category.distribution, ItemStack.with(silicon, 60, surgeAlloy, 120));
        logisticsBase.researchCost = ItemStack.with(silicon, 60, surgeAlloy, 120);
        logisticsBase.alwaysUnlocked = true;
        logisticsBase.botType = LogisticsUnitTypes.logisticsBot;
        showOnBothPlanets(logisticsBase);

        logisticsSupplyPoint = new SupplyPoint("logistics-supply-point");
        logisticsSupplyPoint.requirements(Category.distribution, ItemStack.with(silicon, 20, surgeAlloy, 10));
        logisticsSupplyPoint.researchCost = ItemStack.with(silicon, 20, surgeAlloy, 10);
        logisticsSupplyPoint.itemCapacity = 320;
        showOnBothPlanets(logisticsSupplyPoint);

        logisticsRequestPoint = new RequestPoint("logistics-request-point");
        logisticsRequestPoint.requirements(Category.distribution, ItemStack.with(silicon, 20, surgeAlloy, 10));
        logisticsRequestPoint.researchCost = ItemStack.with(silicon, 20, surgeAlloy, 10);
        logisticsRequestPoint.itemCapacity = 320;
        showOnBothPlanets(logisticsRequestPoint);

        logisticsStoragePoint = new StoragePoint("logistics-storage-point");
        logisticsStoragePoint.requirements(Category.distribution, ItemStack.with(silicon, 20, surgeAlloy, 10));
        logisticsStoragePoint.researchCost = ItemStack.with(silicon, 20, surgeAlloy, 10);
        logisticsStoragePoint.itemCapacity = 320;
        showOnBothPlanets(logisticsStoragePoint);
    }

    /**
     * Makes the block buildable on both planets. The cost includes surge alloy
     * (a Serpulo-only item), which would otherwise restrict the block to
     * Serpulo via the automatic shownPlanets assignment in Block.postInit().
     */
    static void showOnBothPlanets(Block block){
        block.shownPlanets.add(Planets.serpulo);
        block.shownPlanets.add(Planets.erekir);
    }
}
