package thaumicomniverse.compat.create;
import java.util.function.BiConsumer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.*;
public final class LegacySafeNbt {
    public static final LegacyTransforms.Table<BlockEntityType<?>,BiConsumer<BlockEntity,CompoundTag>> REGISTRY=new LegacyTransforms.Table<>();
    /** Only schematic export loses stored contents. Contraption movement retains full NBT. */
    public static void sanitize(CompoundTag data,net.minecraft.world.level.Level level,net.minecraft.core.BlockPos origin){
        var palette=data.getList("palette",net.minecraft.nbt.Tag.TAG_COMPOUND);
        for(var value:data.getList("blocks",net.minecraft.nbt.Tag.TAG_COMPOUND)){
            var block=(CompoundTag)value;var xyz=block.getList("pos",net.minecraft.nbt.Tag.TAG_INT);
            var pos=origin.offset(xyz.getInt(0),xyz.getInt(1),xyz.getInt(2));
            var state=level.getBlockState(pos);var tile=level.getBlockEntity(pos);
            if(!ArcaneContraptions.thaumic(state))continue;
            var filtered=ArcaneSchematic.state(tile,state);
            if(filtered!=state){
                var encoded=net.minecraft.nbt.NbtUtils.writeBlockState(filtered);int index=palette.indexOf(encoded);
                if(index<0){index=palette.size();palette.add(encoded);}block.putInt("state",index);
                block.remove("nbt");
            }else if(tile!=null){
                var writer=REGISTRY.get(tile.getType());if(writer==null)continue;
                var safe=new CompoundTag();writer.accept(tile,safe);
                var previous=block.getCompound("nbt");
                for(var key:java.util.List.of("id","x","y","z"))if(previous.contains(key))safe.put(key,previous.get(key).copy());
                block.put("nbt",safe);
            }
        }
    }
}
