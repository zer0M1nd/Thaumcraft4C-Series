package thaumicomniverse;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import tc4revival.aspect.*;
import thaumicomniverse.client.NodeMapAppearance;
class NodeMapAppearanceTest {
    private static final AspectCatalog CATALOG = new AspectCatalog(java.util.List.of(
        new Aspect("aer",0xffff7e,java.util.List.of(),"e",1),
        new Aspect("victus",0xde0005,java.util.List.of(),"c",1)));
    private java.util.List<NodeMapAppearance.Layer> layers(CompoundTag tag) { return NodeMapAppearance.layers(tag,CATALOG); }

    private CompoundTag node(int amount, int type, int modifier) {
        var tag=new CompoundTag();var values=new AspectAmounts();
        values.add(CATALOG.require("aer"),amount);
        values.add(CATALOG.require("victus"),amount*2);
        AspectNbt.write(tag,values);tag.putByte("type",(byte)type);tag.putByte("modifier",(byte)modifier);return tag;
    }
    @Test void capacityDoesNotChangeMapFootprintOrColors() {
        assertEquals(layers(node(1,0,-1)),layers(node(10000,0,-1)));
        var layers=layers(node(26,0,-1));
        assertEquals(3,layers.size());
        assertEquals(java.util.Set.of(CATALOG.require("aer").color(),CATALOG.require("victus").color()),
            layers.subList(0,2).stream().map(NodeMapAppearance.Layer::color).collect(java.util.stream.Collectors.toSet()));
    }
    @Test void eachTypeUsesTheItemRendererSpriteRow() {
        int[] rows={1,6,2,5,3,4};
        for(int i=0;i<rows.length;i++) {var layers=layers(node(10,i,-1));assertEquals(rows[i],layers.get(2).row());}
    }
    @Test void brightnessAndFadingSurviveNormalization() {
        var normal=layers(node(10,0,-1)).get(2);
        var bright=layers(node(10,0,0)).get(2);
        var pale=layers(node(10,0,1)).get(2);
        var fading=layers(node(10,0,2)).get(2);
        assertEquals(normal.opacity()*1.5,bright.opacity(),.0001);
        assertEquals(normal.opacity()*.66,pale.opacity(),.0001);
        assertNotEquals(fading.alpha(0),fading.alpha(200));
        for(int i=0;i<300;i++)assertTrue(fading.alpha(i)>=0 && fading.alpha(i)<=1);
        assertEquals(NodeMapAppearance.frame(0),NodeMapAppearance.frame(1280));
    }
}
