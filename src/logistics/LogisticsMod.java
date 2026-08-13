package logistics;

import arc.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.game.EventType.*;
import mindustry.mod.*;
import mindustry.world.*;

import static mindustry.content.Blocks.*;
import static mindustry.content.TechTree.*;

/**
 * Logistics Network - a Factorio-style logistics system.
 * <p>
 * Buildings:
 * <ul>
 * <li><b>Logistics base</b> - while powered, spawns flying logistics drones.
 * Drones vanish if the base is destroyed.</li>
 * <li><b>Supply point</b> - passive provider chest (red box): drones pick
 * items up from it.</li>
 * <li><b>Request point</b> - requester chest (blue box): configure an item,
 * drones keep it full.</li>
 * <li><b>Storage point</b> - storage chest (yellow box): excess items are
 * stored here, and drones take from it when needed.</li>
 * </ul>
 */
public class LogisticsMod extends Mod{
    @Override
    public void loadContent(){
        LogisticsUnitTypes.load();
        LogisticsBlocks.load();

        //tech tree: the base follows vault (Serpulo) and unitCargoLoader (Erekir);
        //the three chests follow the base in both trees. loadContent() runs after
        //the vanilla trees are built (SerpuloTechTree/ErekirTechTree), so the
        //vault/unitCargoLoader nodes already exist here.
        TechNode baseSerpulo = new TechNode(findNode(vault), LogisticsBlocks.logisticsBase, LogisticsBlocks.logisticsBase.researchRequirements());
        TechNode baseErekir = new TechNode(findNode(unitCargoLoader), LogisticsBlocks.logisticsBase, LogisticsBlocks.logisticsBase.researchRequirements());

        new TechNode(baseSerpulo, LogisticsBlocks.logisticsSupplyPoint, LogisticsBlocks.logisticsSupplyPoint.researchRequirements());
        new TechNode(baseSerpulo, LogisticsBlocks.logisticsRequestPoint, LogisticsBlocks.logisticsRequestPoint.researchRequirements());
        new TechNode(baseSerpulo, LogisticsBlocks.logisticsStoragePoint, LogisticsBlocks.logisticsStoragePoint.researchRequirements());

        new TechNode(baseErekir, LogisticsBlocks.logisticsSupplyPoint, LogisticsBlocks.logisticsSupplyPoint.researchRequirements());
        new TechNode(baseErekir, LogisticsBlocks.logisticsRequestPoint, LogisticsBlocks.logisticsRequestPoint.researchRequirements());
        new TechNode(baseErekir, LogisticsBlocks.logisticsStoragePoint, LogisticsBlocks.logisticsStoragePoint.researchRequirements());
    }

    /** Finds the tech tree node of a vanilla block. */
    static TechNode findNode(Block block){
        TechNode node = all.find(n -> n.content == block);
        if(node == null){
            throw new IllegalArgumentException("No tech tree node found for block: " + block.name);
        }
        return node;
    }

    @Override
    public void init(){
        //registries are rebuilt by the buildings themselves on load
        Events.on(ResetEvent.class, e -> LogisticsNetwork.clear());
        Events.on(WorldLoadBeginEvent.class, e -> LogisticsNetwork.clear());

        Log.info("Logistics Network mod loaded.");
    }
}
