package thaumicomniverse.compat.create;
import java.util.*;
import java.util.function.*;
import com.simibubi.create.content.contraptions.StructureTransform;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.*;

public final class LegacyTransforms {
    public static final Table<Block,BiFunction<BlockState,StructureTransform,BlockState>> BLOCK_TRANSFORMERS=new Table<>();
    public static final Table<BlockEntityType<?>,BiConsumer<BlockEntity,StructureTransform>> BLOCK_ENTITY_TRANSFORMERS=new Table<>();
    public static final class Table<K,V> extends IdentityHashMap<K,V> { public void register(K key,V handler){put(key,handler);} }
}
