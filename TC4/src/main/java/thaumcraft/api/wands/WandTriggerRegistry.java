package thaumcraft.api.wands;

import java.util.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.state.BlockState;
/** Ordered block-state triggers, including blocks owned by vanilla or another mod. PASS continues dispatch. */
public  class WandTriggerRegistry {
    @FunctionalInterface public interface Trigger { InteractionResult use(UseOnContext context); }
    private record Entry(java.util.function.Predicate<BlockState> matches,Trigger trigger) {}
    private static final Map<ResourceLocation,Entry> ENTRIES=new LinkedHashMap<>();
    public static synchronized void register(ResourceLocation id,java.util.function.Predicate<BlockState> matches,Trigger trigger) {
        if(ENTRIES.putIfAbsent(id,new Entry(Objects.requireNonNull(matches),Objects.requireNonNull(trigger)))!=null)throw new IllegalArgumentException("Duplicate wand trigger "+id);
    }
    public static InteractionResult perform(UseOnContext context) {
        var state=context.getLevel().getBlockState(context.getClickedPos());
        for(var registered:ENTRIES.entrySet()){var entry=registered.getValue();if(entry.matches.test(state)) {var result=MagicOperationRules.withTrigger(registered.getKey().toString(),context,()->entry.trigger.use(context));if(result!=InteractionResult.PASS)return result;}}
        return InteractionResult.PASS;
    }
}
