package logistics;

import arc.util.*;
import logistics.blocks.*;
import mindustry.*;
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

        applySFireCompat();
    }

    /**
     * sfire-mod ("Saturation Firepower") compatibility: when that mod is loaded, the logistics
     * base costs <b>120 silisteel</b> instead of silicon + surge alloy. When it is not loaded,
     * costs stay unchanged.
     * <p>
     * This runs at the end of {@link #load()}, i.e. before the tech tree nodes are created in
     * {@link LogisticsMod#loadContent()}, so both the build cost and the research cost switch to
     * silisteel. A soft dependency declared in mod.json guarantees sfire-mod's content is already
     * registered when this runs; it does not prevent this mod from loading when sfire-mod is missing.
     */
    static void applySFireCompat(){
        //only when sfire-mod is actually loaded
        if(Vars.mods.locateMod("sfire-mod") != null){
            Item silisteel = Vars.content.item("silisteel");
            //guard: item must exist (e.g. sfire-mod failed to load its content)
            if(silisteel != null){
                ItemStack[] cost = ItemStack.with(silisteel, 120);
                logisticsBase.requirements(Category.distribution, cost);
                logisticsBase.researchCost = cost;
            }
        }
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
