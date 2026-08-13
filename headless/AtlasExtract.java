import arc.files.*;
import arc.graphics.g2d.TextureAtlas.TextureAtlasData;

/** Dumps atlas region coordinates for the vanilla cargo load/unload point textures. */
public class AtlasExtract{
    public static void main(String[] args){
        Fi dir = new Fi("sprites");
        TextureAtlasData data = new TextureAtlasData(dir.child("sprites.aatls"), dir, false);
        String[] want = {"unit-cargo-loader", "unit-cargo-unload-point", "unit-cargo-unload-point-top"};
        for(TextureAtlasData.Region r : data.getRegions()){
            for(String w : want){
                if(r.name.equals(w)){
                    System.out.println(w + "|page=" + r.page.textureFile.name()
                        + "|x=" + r.left + "|y=" + r.top + "|w=" + r.width + "|h=" + r.height
                        + "|origW=" + r.originalWidth + "|origH=" + r.originalHeight
                        + "|rot=" + r.rotate);
                }
            }
        }
    }
}
