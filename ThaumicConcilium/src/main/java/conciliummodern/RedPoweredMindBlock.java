package conciliummodern;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.*;

public final class RedPoweredMindBlock extends BaseEntityBlock {
    public RedPoweredMindBlock(){super(Properties.of().strength(.7f,1).noOcclusion().lightLevel(state->7).sound(SoundType.SLIME_BLOCK));}
    public static boolean available(){var mods=net.minecraftforge.fml.ModList.get();return mods.isLoaded("thaumichorizons")&&mods.isLoaded("automagy");}
    @Override public RenderShape getRenderShape(BlockState state){return RenderShape.ENTITYBLOCK_ANIMATED;}
    @Override public VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return box(2.4,3.52,2.4,13.6,12.8,13.6);}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new RedPoweredMindBlockEntity(pos,state);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){return createTickerHelper(type,ConciliumBlocks.RED_MIND_TILE.get(),RedPoweredMindBlockEntity::tick);}
    @Override public void neighborChanged(BlockState state,Level level,BlockPos pos,Block block,BlockPos from,boolean moving){super.neighborChanged(state,level,pos,block,from,moving);if(level.getBlockEntity(pos) instanceof RedPoweredMindBlockEntity mind)mind.sample();}
    public static final class Item extends net.minecraft.world.item.BlockItem {
        public Item(){super(ConciliumBlocks.RED_MIND.get(),new Properties());}
        @Override public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer){consumer.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions(){
            @Override public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer(){return RedPoweredMindRenderer.itemRenderer();}
        });}
    }
}
