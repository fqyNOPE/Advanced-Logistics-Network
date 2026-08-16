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
 * to its capacity. Belts and other buildings cannot insert items: the chest
 * only accepts deliveries from logistics drones (or the player).
 * <p>
 * It also acts as an active supplier: its stored items are automatically
 * pushed to adjacent belts / buildings, so it can feed a production line.
 * <p>
 * Each request point has a 1-9 priority (default 5). When drones compete
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
        //full config (item + priority + capacity) travels as one packed long,
        //so the copy tool / schematics restore every setting, not just the item
        config(Long.class, (RequestBuild tile, Long cfg) -> tile.applyConfig(cfg));
        configClear((RequestBuild tile) -> {
            tile.requestItem = null;
            tile.recache();
        });
    }

    /**
     * Packs item id (16 bits, 0xFFFF = none), priority (8 bits) and capacity
     * percent index (8 bits) into one config value, so copying the building
     * or saving a schematic carries all settings.
     */
    static long packConfig(@Nullable Item item, int priority, int capacityPercent){
        int itemId = item == null ? 0xFFFF : item.id;
        return ((long)(itemId & 0xFFFF) << 16)
            | ((long)(priority & 0xFF) << 8)
            | (long)((capacityPercent / 10 - 1) & 0xFF);
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
        /** Delivery priority 1-9; 5 is the default ("normal"). Only the number is shown, in a single color. */
        public int priority = 5;
        /**
         * Per-build item cap as a percentage of the block's itemCapacity
         * (10%..100% in steps of 10; the slider works in percent, this field
         * stores the resulting item count: 10% of 400 = 40, 100% = 400).
         * All demand/threshold/dispatch logic goes through
         * {@link #getMaximumAccepted(Item)}, so lowering the cap simply stops
         * deliveries once the stock reaches it; surplus items stay in the chest
         * and keep being dumped to adjacent belts/buildings.
         */
        public int maxCapacity = 400;

        /** Percent value (10..100) this build's cap corresponds to. */
        int capacityPercent(){
            return Math.max(10, Math.min(100, maxCapacity * 100 / itemCapacity));
        }

        /** Applies a packed config value (item + priority + capacity). */
        void applyConfig(long cfg){
            int itemId = (int)((cfg >> 16) & 0xFFFF);
            requestItem = itemId == 0xFFFF ? null : content.item(itemId);
            priority = Math.max(1, Math.min(9, (int)((cfg >> 8) & 0xFF)));
            int pct = Math.max(10, Math.min(100, ((int)(cfg & 0xFF) + 1) * 10));
            maxCapacity = pct * itemCapacity / 100;
            recache();
        }

        @Override
        public void updateTile(){
            //actively push stored items to adjacent belts / buildings
            dump();
        }

        @Override
        public int getMaximumAccepted(Item item){
            return maxCapacity;
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
            //only the requested item may enter, and only via the unit delivery
            //path: acceptStack calls back with THIS building as the source, so
            //belts and other buildings (which pass themselves) are rejected -
            //the request point can only be fed by logistics drones (or players)
            return item == requestItem && source == self() && items.get(item) < getMaximumAccepted(item);
        }

        @Override
        public int acceptStack(Item item, int amount, Teamc source){
            //gate the unit delivery path directly (drones, players): the item
            //must be the requested one and the chest must have room
            if(item == requestItem && items.get(item) < getMaximumAccepted(item)){
                return Math.min(getMaximumAccepted(item) - items.get(item), amount);
            }
            return 0;
        }

        @Override
        public void buildConfiguration(Table table){
            ItemSelection.buildTable(RequestPoint.this, table, content.items(), () -> requestItem,
                item -> configure(packConfig(item, priority, capacityPercent())), selectionRows, selectionColumns);

            //priority row: label on its own line, slider + value on the next,
            //so the long slider cannot deform the item-selection grid above
            table.row();
            table.add(Core.bundle.get("logistics.priority")).padBottom(4f);
            table.row();
            Slider slider = new Slider(1f, 9f, 1f, false);
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

            //capacity row: same layout convention as the priority row; the
            //slider works in percent of the chest's item capacity (10%..100%)
            table.row();
            table.add(Core.bundle.get("logistics.capacity")).padBottom(4f);
            table.row();
            Slider capSlider = new Slider(10f, 100f, 10f, false);
            capSlider.setValue(capacityPercent());
            capSlider.moved(value -> {
                maxCapacity = (int)(value / 100f * itemCapacity);
                //cached drawing does not depend on the cap, but keep the
                //rebuild convention of the other slider for consistency
                recache();
            });
            table.add(capSlider).width(200f).padRight(8f);
            Label capLabel = new Label("", Styles.defaultLabel);
            //show the actual item count (capacity x percent), not the percent
            capLabel.update(() -> capLabel.setText(String.valueOf(maxCapacity)));
            table.add(capLabel);
        }

        @Override
        public Object config(){
            return packConfig(requestItem, priority, capacityPercent());
        }

        @Override
        public byte version(){
            return 3;
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.s(requestItem == null ? -1 : requestItem.id);
            write.b((byte)priority);
            //the cap is stored as a 0..9 percent index ((percent/10)-1): 10%->0, 20%->1, ..., 100%->9
            write.b((byte)(capacityPercent() / 10 - 1));
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            requestItem = content.item(read.s());
            //revision 1 saves have no priority field: default to 5 (= normal,
            //behaves exactly like before this feature existed); the priority
            //range is 1..9, so clamp legacy 0 to 1
            priority = Math.max(1, revision >= 2 ? read.b() : 5);
            //revision < 3 saves have no cap field: default to 100% (current behavior)
            if(revision < 3){
                maxCapacity = itemCapacity;
            }else{
                //0..9 percent index: 10%->0 ... 100%->9
                int index = Math.max(0, Math.min(9, read.b()));
                int percent = (index + 1) * 10;
                maxCapacity = percent * itemCapacity / 100;
            }
        }
    }
}
