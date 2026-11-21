package thaumicomniverse.compat.ponder;

import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import java.util.function.Consumer;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.*;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

/** Small authoring helpers. Every mutation is queued; scene compilation never dereferences a world. */
public final class PonderStage {
    public final CreateSceneBuilder s;
    public final SceneBuildingUtil u;
    public final String id;
    PonderStage(SceneBuilder base, SceneBuildingUtil util, OmniPonder.Lesson lesson) {
        s = new CreateSceneBuilder(base); u = util; id = lesson.id();
        s.title(id, lesson.title()); s.configureBasePlate(0, 0, lesson.size());
        if (lesson.size() >= 9) s.scaleSceneView(.78f);
        s.showBasePlate(); s.idle(10);
    }
    public static BlockPos at(int x, int y, int z) { return new BlockPos(x,y,z); }
    public static Vec3 center(int x, int y, int z) { return Vec3.atCenterOf(at(x,y,z)); }
    public static tc4revival.aspect.AspectAmounts nodeAspects(){
        var aspects=new tc4revival.aspect.AspectAmounts();
        for(var aspect:tc4revival.aspect.TCAspects.catalog().primals())aspects.add(aspect,64);
        return aspects;
    }
    public static ItemStack item(String id) {
        var key = new ResourceLocation(id);
        if (!ForgeRegistries.ITEMS.containsKey(key)) throw new IllegalArgumentException("Missing tutorial item " + id);
        return new ItemStack(ForgeRegistries.ITEMS.getValue(key));
    }
    public static BlockState state(String id, String... properties) {
        var key = new ResourceLocation(id);
        if (!ForgeRegistries.BLOCKS.containsKey(key)) throw new IllegalArgumentException("Missing tutorial block " + id);
        var state = ForgeRegistries.BLOCKS.getValue(key).defaultBlockState();
        for (int i=0; i<properties.length; i+=2) state = property(state, properties[i], properties[i+1]);
        return state;
    }
    private static <T extends Comparable<T>> BlockState property(BlockState state, String name, String value) {
        @SuppressWarnings("unchecked") var property = (Property<T>)state.getBlock().getStateDefinition().getProperty(name);
        if (property == null) throw new IllegalArgumentException("No property " + name + " on " + state);
        return state.setValue(property, property.getValue(value).orElseThrow());
    }
    public Selection box(int x1,int y1,int z1,int x2,int y2,int z2) { return u.select().fromTo(x1,y1,z1,x2,y2,z2); }
    public Selection pos(int x,int y,int z) { return u.select().position(x,y,z); }
    public void show(int x,int y,int z) { s.world().showSection(pos(x,y,z),Direction.DOWN); }
    public void poweredNode(int x,int y,int z) { show(x,y,z,x,y+2,z);show(x-1,y+2,z); }
    public void show(int x1,int y1,int z1,int x2,int y2,int z2) { s.world().showSection(box(x1,y1,z1,x2,y2,z2),Direction.DOWN); }
    public void hide(int x1,int y1,int z1,int x2,int y2,int z2) { s.world().hideSection(box(x1,y1,z1,x2,y2,z2),Direction.UP); }
    public void block(int x,int y,int z,String id,String... properties) { s.world().setBlock(at(x,y,z),state(id,properties),false); }
    public void nbt(int x,int y,int z,Consumer<CompoundTag> edit) { s.world().modifyBlockEntityNBT(pos(x,y,z),BlockEntity.class,edit); }
    public void key() { s.addKeyframe(); }
    public void waitFor(int ticks) { s.idle(ticks); }
    public void text(String key,int ticks,int x,int y,int z) { text(key,ticks,x,y,z,PonderPalette.WHITE); }
    public void text(String key,int ticks,int x,int y,int z,PonderPalette palette) {
        s.overlay().showText(ticks).sharedText(id+"."+key).colored(palette).pointAt(center(x,y,z)).placeNearTarget();
        s.idle(ticks+10);
    }
    public void right(int x,int y,int z,String held,boolean sneak) {
        var controls=s.overlay().showControls(center(x,y,z),Pointing.DOWN,40).rightClick();
        if (held!=null) controls.withItem(item(held));
        if (sneak) controls.whileSneaking();
    }
    public void wand(int x,int y,int z) { right(x,y,z,"thaumcraft:casting_wand",false); }
    public void outline(String key,Selection selection,PonderPalette color,int ticks) { s.overlay().showOutline(color,id+key,selection,ticks); }
    public void line(int x1,int y1,int z1,int x2,int y2,int z2,PonderPalette color,int ticks) {
        s.overlay().showLine(color,center(x1,y1,z1),center(x2,y2,z2),ticks);
    }
    public void jar(int x,int y,int z,String aspect,int amount,boolean label) {
        nbt(x,y,z,n->{n.putString("Aspect",aspect); n.putInt("Amount",amount); if(label)n.putString("AspectFilter",aspect);});
    }
    public void speed(Selection selection,float rpm) { s.world().setKineticSpeed(selection,rpm); }
    public void finish() { s.idle(20); s.markAsFinished(); }
}
