package thaumcraft.api.golems;

import java.util.*;
import java.util.function.Function;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;
import tc4revival.content.GolemInventoryViews;
import thaumcraft.api.aspects.*;

/** Persistent references survive assembly while their golem or bell is unloaded.
 * Providers return ONE physical container; exposing a contraption-wide handler here is incorrect.
 */
public class GolemLocations extends SavedData {
    public record Target(Vec3 center,Direction face,IItemHandler items,IFluidHandler fluids,IEssentiaStorage essentia) {}
    @FunctionalInterface public interface Provider {
        Target resolve(ServerLevel level,UUID structure,BlockPos local,Direction face);
    }
    private static final Map<ResourceLocation,Provider> PROVIDERS=new LinkedHashMap<>();
    private final Map<UUID,Frame> frames=new LinkedHashMap<>();
    private static class Frame {
        BlockPos pos; UUID structure; ResourceLocation provider;
        Direction[] basis=Direction.values().clone();
        Frame(BlockPos pos){this.pos=pos.immutable();}
    }
    public static void register(ResourceLocation id,Provider provider){
        if(PROVIDERS.putIfAbsent(id,provider)!=null)throw new IllegalArgumentException("Duplicate golem location provider "+id);
    }
    public static boolean enabled(){return !PROVIDERS.isEmpty();}
    private static GolemLocations data(ServerLevel level){
        return level.getDataStorage().computeIfAbsent(GolemLocations::load,GolemLocations::new,"thaumcraft_golem_locations");
    }
    public static GolemAnchor bind(ServerLevel level,BlockPos pos,Direction face){
        var data=data(level);
        for(var entry:data.frames.entrySet())if(entry.getValue().structure==null&&entry.getValue().pos.equals(pos))
            return new GolemAnchor(entry.getKey(),inverse(entry.getValue(),face));
        var id=UUID.randomUUID();data.frames.put(id,new Frame(pos));data.setDirty();return new GolemAnchor(id,face);
    }
    public static GolemAnchor bindMoving(ServerLevel level,ResourceLocation provider,UUID structure,BlockPos local,Direction face){
        var data=data(level);
        for(var entry:data.frames.entrySet())if(structure.equals(entry.getValue().structure)&&provider.equals(entry.getValue().provider)&&local.equals(entry.getValue().pos))
            return new GolemAnchor(entry.getKey(),inverse(entry.getValue(),face));
        var frame=new Frame(local);frame.provider=provider;frame.structure=structure;
        var id=UUID.randomUUID();data.frames.put(id,frame);data.setDirty();return new GolemAnchor(id,face);
    }
    private static Direction inverse(Frame frame,Direction face){
        for(var value:Direction.values())if(frame.basis[value.get3DDataValue()]==face)return value;
        return face;
    }
    public static boolean moving(ServerLevel level,GolemAnchor anchor){
        var frame=anchor==null?null:data(level).frames.get(anchor.id());return frame!=null&&frame.structure!=null;
    }
    /** Public, non-inventory frame metadata used to render a held bell's markers. */
    public static CompoundTag view(ServerLevel level,GolemAnchor anchor){
        var frame=anchor==null?null:data(level).frames.get(anchor.id());var tag=new CompoundTag();
        if(frame!=null&&frame.structure!=null){tag.putString("Provider",frame.provider.toString());tag.putUUID("Structure",frame.structure);tag.putLong("Local",frame.pos.asLong());tag.putByte("Face",(byte)frame.basis[anchor.face().get3DDataValue()].get3DDataValue());}
        return tag;
    }
    public static Target resolve(ServerLevel level,GolemAnchor anchor){
        if(anchor==null)return null;var frame=data(level).frames.get(anchor.id());if(frame==null)return null;
        var side=frame.basis[anchor.face().get3DDataValue()];
        if(frame.structure!=null){var provider=PROVIDERS.get(frame.provider);return provider==null?null:provider.resolve(level,frame.structure,frame.pos,side);}
        if(!level.hasChunkAt(frame.pos))return null;
        var be=level.getBlockEntity(frame.pos);
        return new Target(frame.pos.getCenter(),side,GolemInventoryViews.single(level,frame.pos,side),
            be==null?null:be.getCapability(ForgeCapabilities.FLUID_HANDLER,side).resolve().orElse(null),
            be!=null&&EssentiaStorageApi.supports(be,null)?EssentiaStorageApi.atInventory(level,frame.pos):null);
    }
    /** Called before removing the captured blocks, including for references owned by unloaded entities. */
    public static void mount(ServerLevel level,ResourceLocation provider,UUID structure,Map<BlockPos,BlockPos> worldToLocal){
        var data=data(level);boolean changed=false;
        for(var frame:data.frames.values())if(frame.structure==null){
            var local=worldToLocal.get(frame.pos);if(local==null)continue;
            frame.pos=local.immutable();frame.provider=provider;frame.structure=structure;changed=true;
        }
        if(changed)data.setDirty();
    }
    /** Rebase each reference only after the actual placement transform has been chosen. */
    public static void unmount(ServerLevel level,ResourceLocation provider,UUID structure,Function<BlockPos,BlockPos> position,Function<Direction,Direction> direction){
        var data=data(level);boolean changed=false;
        for(var frame:data.frames.values())if(structure.equals(frame.structure)&&provider.equals(frame.provider)){
            frame.pos=position.apply(frame.pos).immutable();
            for(int i=0;i<6;i++)frame.basis[i]=direction.apply(frame.basis[i]);
            frame.provider=null;frame.structure=null;changed=true;
        }
        if(changed)data.setDirty();
    }
    @Override public CompoundTag save(CompoundTag tag){
        var list=new ListTag();frames.forEach((id,frame)->{
            var value=new CompoundTag();value.putUUID("Id",id);value.putLong("Pos",frame.pos.asLong());
            int[] basis=Arrays.stream(frame.basis).mapToInt(Direction::get3DDataValue).toArray();value.putIntArray("Basis",basis);
            if(frame.structure!=null){value.putUUID("Structure",frame.structure);value.putString("Provider",frame.provider.toString());}list.add(value);
        });tag.put("Frames",list);return tag;
    }
    private static GolemLocations load(CompoundTag tag){
        var data=new GolemLocations();
        for(var entry:tag.getList("Frames",Tag.TAG_COMPOUND)){
            var value=(CompoundTag)entry;if(!value.hasUUID("Id"))continue;
            var frame=new Frame(BlockPos.of(value.getLong("Pos")));int[] basis=value.getIntArray("Basis");
            if(basis.length==6)for(int i=0;i<6;i++)frame.basis[i]=Direction.from3DDataValue(basis[i]);
            if(value.hasUUID("Structure")){frame.structure=value.getUUID("Structure");frame.provider=ResourceLocation.tryParse(value.getString("Provider"));}
            data.frames.put(value.getUUID("Id"),frame);
        }return data;
    }
}
