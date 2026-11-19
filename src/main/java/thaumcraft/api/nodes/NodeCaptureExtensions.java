package thaumcraft.api.nodes;

import java.util.*;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import tc4revival.content.*;
import thaumcraft.api.util.ExtensionRegistry;

/** A capture chooses its final container before any write; the host transaction owns rollback/payment. */
public final class NodeCaptureExtensions {
    public record Context(Level level,BlockPos center,BlockEntityType<?> sourceType,CompoundTag saved,NodeJarData contents) {
        public Context {center=center.immutable();saved=saved.copy();}
        @Override public CompoundTag saved(){return saved.copy();}
    }
    public record Capture(BlockState state,CompoundTag extensions) {
        public Capture {Objects.requireNonNull(state);extensions=extensions.copy();}
        @Override public CompoundTag extensions(){return extensions.copy();}
        public void initialize(NodeJarBlockEntity jar){
            if(extensions.isEmpty())return;
            var data=jar.saveWithoutMetadata();
            for(String key:extensions.getAllKeys()){
                if(data.contains(key)) {
                    // A container may declare an empty extension by default; base node fields are reserved.
                    if(!data.get(key).equals(new CompoundTag()))throw new IllegalArgumentException("Capture extension overwrites node data: "+key);
                }
                data.put(key,Objects.requireNonNull(extensions.get(key)).copy());
            }
            jar.load(data);jar.setChanged();
        }
    }
    private static final ExtensionRegistry<Function<Context,Optional<Capture>>> PROVIDERS=new ExtensionRegistry<>();
    private NodeCaptureExtensions() {}
    public static void register(String id,int priority,Function<Context,Optional<Capture>> provider){PROVIDERS.register(id,priority,provider);}
    public static Capture plan(Context context,BlockState fallback){
        for(var entry:PROVIDERS.entries()){
            var result=Objects.requireNonNull(entry.value().apply(context));if(result.isPresent())return result.get();
        }
        return new Capture(fallback,new CompoundTag());
    }
}
