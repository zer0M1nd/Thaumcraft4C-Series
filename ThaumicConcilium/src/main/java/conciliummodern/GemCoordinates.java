package conciliummodern;

import java.util.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.resources.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

/** Bounded, typed samples replace numeric block metadata and dimension IDs. */
public final class GemCoordinates {
    public static final int LIMIT=GemRules.SAMPLE_LIMIT;
    public record Sample(BlockState state,ResourceKey<Level> dimension) {
        public CompoundTag save(){var out=new CompoundTag();out.put("State",NbtUtils.writeBlockState(state));out.putString("Dimension",dimension.location().toString());return out;}
        public ItemStack display(){return new ItemStack(state.getBlock());}
    }
    public static Sample read(CompoundTag tag){
        var id=ResourceLocation.tryParse(tag.getString("Dimension"));
        if(id==null||!tag.contains("State",Tag.TAG_COMPOUND))return null;
        var state=NbtUtils.readBlockState(net.minecraft.core.registries.BuiltInRegistries.BLOCK.asLookup(),tag.getCompound("State"));
        if(state.isAir())return null;
        return new Sample(state,ResourceKey.create(Registries.DIMENSION,id));
    }
    public static Sample captured(ItemStack stack){
        if(!stack.hasTag())return null;
        var tag=stack.getTag();var sample=read(tag.getCompound("Captured"));
        return sample!=null?sample:legacy(tag.getString("name"),tag.getInt("meta"),tag,"dim");
    }
    public static List<Sample> samples(ItemStack stack){
        if(!stack.hasTag())return List.of();var tag=stack.getTag();var out=new ArrayList<Sample>();
        if(tag.contains("Samples",Tag.TAG_LIST)){
            var list=tag.getList("Samples",Tag.TAG_COMPOUND);
            for(int i=0;i<Math.min(LIMIT,list.size());i++){var sample=read(list.getCompound(i));if(sample!=null)out.add(sample);}
        }else for(int i=0;i<GemRules.sampleCount(tag.getInt("MAX_GEMS"));i++){
            var sample=legacy(tag.getString("NAME"+i),tag.getInt("META"+i),tag,"DIM"+i);if(sample!=null)out.add(sample);
        }
        return List.copyOf(out);
    }
    private static Sample legacy(String name,int metadata,CompoundTag tag,String dimensionKey){
        // Legacy standard dimensions are unambiguous. Unknown modded numeric IDs are not silently redirected.
        if(!tag.contains(dimensionKey,Tag.TAG_STRING)&&!tag.contains(dimensionKey,Tag.TAG_ANY_NUMERIC))return null;
        ResourceKey<Level> dimension=tag.contains(dimensionKey,Tag.TAG_STRING)?
                Optional.ofNullable(ResourceLocation.tryParse(tag.getString(dimensionKey))).map(id->ResourceKey.create(Registries.DIMENSION,id)).orElse(null):
                switch(tag.getInt(dimensionKey)){case -1->Level.NETHER;case 0->Level.OVERWORLD;case 1->Level.END;default->null;};
        var id=ResourceLocation.tryParse(name);if(dimension==null||id==null||metadata!=0||!ForgeRegistries.BLOCKS.containsKey(id))return null;
        var block=ForgeRegistries.BLOCKS.getValue(id);return block==null||block==Blocks.AIR?null:new Sample(block.defaultBlockState(),dimension);
    }
    public static int selected(ItemStack stack,int count){return GemRules.selected(stack.hasTag()?stack.getTag().getInt("CURR"):0,count);}
    public static Sample selected(ItemStack stack){var samples=samples(stack);return samples.isEmpty()?null:samples.get(selected(stack,samples.size()));}
    public static void append(ItemStack stack,Sample sample){
        var all=new ArrayList<>(samples(stack));if(all.size()>=LIMIT)throw new IllegalArgumentException("Rift gem already holds 16 samples");all.add(sample);
        var list=new ListTag();all.forEach(value->list.add(value.save()));var tag=stack.getOrCreateTag();tag.put("Samples",list);tag.putInt("CURR",all.size()-1);
        tag.putInt("MAX_GEMS",all.size());
    }
    public static boolean reverse(ItemStack stack){return stack.hasTag()&&stack.getTag().getBoolean("REVERSE");}
    private GemCoordinates(){}
}
