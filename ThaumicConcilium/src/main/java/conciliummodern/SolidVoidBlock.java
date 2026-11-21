package conciliummodern;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class SolidVoidBlock extends BaseEntityBlock {
    public SolidVoidBlock(){super(Properties.of().strength(0,6000).sound(SoundType.STONE));}
    @Override public RenderShape getRenderShape(BlockState state){return RenderShape.ENTITYBLOCK_ANIMATED;}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new Tile(pos,state);}
    public static class Tile extends BlockEntity {public Tile(BlockPos pos,BlockState state){super(ConciliumBlocks.SOLID_VOID_TILE.get(),pos,state);}}
    public static class Item extends net.minecraft.world.item.BlockItem {
        public Item(){super(ConciliumBlocks.SOLID_VOID.get(),new Properties());}
        @Override public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer){
            consumer.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions(){
                @Override public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer(){return VoidBodyRenderer.ITEM;}
            });
        }
    }
}
