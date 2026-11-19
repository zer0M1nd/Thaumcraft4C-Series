package gadomancymodern.claw;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import tc4revival.content.InfusionMatrixBlockEntity;

/** Stateless handlers: no globally shared mutable world/position as in the old API. */
public interface ClawBehavior {
    boolean accepts(ServerLevel level,BlockPos target);
    default boolean consumesVis(){return false;}
    default int comparator(ServerLevel level,BlockPos target){return 0;}
    default void instability(ServerLevel level,BlockPos target,int amount){}
    static java.util.List<ClawBehavior> handlers(){return Registry.HANDLERS;}
    static void register(ClawBehavior handler){Registry.HANDLERS.add(java.util.Objects.requireNonNull(handler));}
    static ClawBehavior find(ServerLevel level,BlockPos target){return handlers().stream().filter(handler->handler.accepts(level,target)).findFirst().orElse(null);}
    class Registry {
        public static final java.util.List<ClawBehavior> HANDLERS=new java.util.concurrent.CopyOnWriteArrayList<>();
        static {
            HANDLERS.add((level,pos)->level.getBlockState(pos).is(Blocks.BOOKSHELF)||level.getBlockState(pos).is(Blocks.CAULDRON));
            HANDLERS.add(new ClawBehavior(){
                @Override public boolean accepts(ServerLevel level,BlockPos pos){return level.getBlockEntity(pos) instanceof InfusionMatrixBlockEntity;}
                @Override public boolean consumesVis(){return true;}
                @Override public int comparator(ServerLevel level,BlockPos pos){return ((InfusionMatrixBlockEntity)level.getBlockEntity(pos)).crafting()?15:0;}
                @Override public void instability(ServerLevel level,BlockPos pos,int amount){((InfusionMatrixBlockEntity)level.getBlockEntity(pos)).addInstability(amount);}
            });
        }
    }
}
