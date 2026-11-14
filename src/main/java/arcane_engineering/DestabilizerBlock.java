package arcane_engineering;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.*;
import net.minecraft.world.level.BlockGetter;

public class DestabilizerBlock extends BaseEntityBlock {
    public DestabilizerBlock() { super(Properties.of().mapColor(MapColor.METAL).strength(5).requiresCorrectToolForDrops().noOcclusion()); }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.ENTITYBLOCK_ANIMATED; }
    @Override public VoxelShape getBlockSupportShape(BlockState state,BlockGetter level,BlockPos pos) { return Block.box(0,0,0,16,1,16); }
    @Override public boolean canEntityDestroy(BlockState state,BlockGetter level,BlockPos pos,net.minecraft.world.entity.Entity entity) { return false; }
    @Override public void onRemove(BlockState state,Level level,BlockPos pos,BlockState replacement,boolean moving) {
        if(!level.isClientSide && !state.is(replacement.getBlock()) && level.getBlockEntity(pos) instanceof DestabilizerBlockEntity device && device.open && level.getBlockEntity(pos.above()) instanceof tc4revival.content.AuraNodeBlockEntity node) {
            device.open=false;
            thaumcraft.api.nodes.NodeApi.configure(node,new thaumcraft.api.aspects.AspectList().add(thaumcraft.api.aspects.Aspect.TAINT,1),tc4revival.content.AuraNodeType.TAINTED,tc4revival.content.AuraNodeModifier.FADING,false);
            level.explode(null,pos.getX()+.5,pos.getY()+1.5,pos.getZ()+.5,2,Level.ExplosionInteraction.BLOCK);
        }
        super.onRemove(state,level,pos,replacement,moving);
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new DestabilizerBlockEntity(pos,state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type,ArcaneEngineering.DESTABILIZER_TILE.get(),DestabilizerBlockEntity::tick);
    }
    public static class Item extends net.minecraft.world.item.BlockItem {
        public Item(Block block) { super(block,new Properties()); }
        @Override public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) { consumer.accept(new arcane_engineering.client.DestabilizerItemExtension()); }
    }
}
