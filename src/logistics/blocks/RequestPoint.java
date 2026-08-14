package logistics.blocks;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.ui.*;
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
 * <p>
 * Each request point has a 0-9 priority (default 5). When drones compete
 * for limited transport capacity, request points wanting the same item are
 * served in strict priority order: a priority 9 box is always served before
 * a priority 8 box, regardless of distance (distance only breaks ties within
 * the same priority). Priorities never affect the request threshold, drone
 * claim limits, source picking or any other behavior.
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
        /** Delivery priority 0-9; 5 is the default ("normal"). Only the number is shown, in a single color. */
        public int priority = 5;

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
            //priority badge: a single-color number in the top-right corner,
            //always drawn (including the default 5); quarter-size (scale 0.25)
            Draw.z(Layer.blockOver);
            Fonts.outline.draw(String.valueOf(priority),
                x + size * tilesize / 2f - 2f,
                y + size * tilesize / 2f - 2f,
                Color.white, 0.25f, false, Align.center);
            Draw.reset();
        }

        @Override
        public boolean acceptItem(Building source, Item item){
            //only the requested item may enter
            return item == requestItem && items.get(item) < getMaximumAccepted(item);
        }

        @Override
        public void buildConfiguration(Table table){
            ItemSelection.buildTable(RequestPoint.this, table, content.items(), () -> requestItem, this::configure, selectionRows, selectionColumns);

            //priority row: label on its own line, slider + value on the next,
            //so the long slider cannot deform the item-selection grid above
            table.row();
            table.add(Core.bundle.get("logistics.priority")).padBottom(4f);
            table.row();
            Slider slider = new Slider(0f, 9f, 1f, false);
            slider.setValue(priority);
            slider.moved(value -> {
                priority = (int)value;
                //this block uses cached drawing (drawCached), so the badge only
                //updates live when we rebuild the cache on every slider move
                recache();
            });
            table.add(slider).width(200f).padRight(8f);
            Label valueLabel = new Label("", Styles.defaultLabel);
            valueLabel.update(() -> valueLabel.setText(String.valueOf(priority)));
            table.add(valueLabel);
        }

        @Override
        public Item config(){
            return requestItem;
        }

        @Override
        public byte version(){
            return 2;
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.s(requestItem == null ? -1 : requestItem.id);
            write.b((byte)priority);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            requestItem = content.item(read.s());
            //revision 1 saves have no priority field: default to 5 (= normal,
            //behaves exactly like before this feature existed)
            priority = revision >= 2 ? read.b() : 5;
        }
    }
}
