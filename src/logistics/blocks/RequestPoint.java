package logistics.blocks;

import arc.*;
import arc.graphics.g2d.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.*;

import static mindustry.Vars.*;

/**
 * Request point - the "requester chest" (blue box) of the network.
 * Tap it to pick the item you want; logistics drones will keep it topped up
 * to its capacity. Belts may also insert the requested item directly.
 * <p>
 * It also acts as an active supplier: its stored items are automatically
 * pushed to adjacent belts / buildings, so it can feed a production line.
 */
public class RequestPoint extends LogisticsChest{
    public RequestPoint(String name){
        super(name);
        update = true;
        configurable = true;
        saveConfig = true;
        clearOnDoubleTap = true;

        config(Item.class, (RequestBuild tile, Item item) -> {
            tile.requestItem = item;
            //force the cached drawing to rebuild so the item color updates instantly
            tile.recache();
        });
        configClear((RequestBuild tile) -> {
            tile.requestItem = null;
            tile.recache();
        });
    }

    /** Overlay texture tinted with the requested item's color (like vanilla unit cargo unload points). */
    public TextureRegion topRegion;

    @Override
    public void load(){
        super.load();
        //the top overlay is our own sprite (copied from the vanilla unit cargo
        //unload point); mod sprites are packed under "modname-filename"
        topRegion = Core.atlas.find("logistics-network-logistics-request-point-top");
    }

    @Override
    public boolean outputsItems(){
        return true;
    }

    @Override
    public boolean isRequester(){
        return true;
    }

    public class RequestBuild extends LogisticsChestBuild{
        /** Item this chest requests from the network; null = nothing requested. */
        public @Nullable Item requestItem;

        @Override
        public void updateTile(){
            //actively push stored items to adjacent belts / buildings
            dump();
        }

        @Override
        public void draw(){
            super.draw();
            //show the requested item's color on the overlay, like vanilla
            //unit cargo unload points
            if(requestItem != null && topRegion != null){
                Draw.color(requestItem.color);
                Draw.rect(topRegion, x, y);
                Draw.color();
            }
        }

        @Override
        public boolean acceptItem(Building source, Item item){
            //only the requested item may enter
            return item == requestItem && items.get(item) < getMaximumAccepted(item);
        }

        @Override
        public void buildConfiguration(Table table){
            ItemSelection.buildTable(RequestPoint.this, table, content.items(), () -> requestItem, this::configure, selectionRows, selectionColumns);
        }

        @Override
        public Item config(){
            return requestItem;
        }

        @Override
        public byte version(){
            return 1;
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.s(requestItem == null ? -1 : requestItem.id);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            requestItem = content.item(read.s());
        }
    }
}
