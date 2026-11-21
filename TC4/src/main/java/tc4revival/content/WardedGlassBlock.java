package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public  class WardedGlassBlock extends BaseEntityBlock {
    public WardedGlassBlock() {
        super(Properties.of().strength(5,999).sound(net.minecraft.world.level.block.SoundType.STONE).noOcclusion()
                .isRedstoneConductor((state,level,pos)->true));
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state) { return new OwnedDeviceBlockEntity(pos,state); }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public float getDestroyProgress(BlockState state,Player player,BlockGetter level,BlockPos pos) {
        return tc4revival.config.TCConfig.SERVER.wardedStone.get() ? 0 : super.getDestroyProgress(state,player,level,pos);
    }
    @Override public void setPlacedBy(Level level,BlockPos pos,BlockState state,LivingEntity placer,ItemStack stack) {
        if (placer instanceof Player player && level.getBlockEntity(pos) instanceof OwnedDeviceBlockEntity entity) {
            entity.access().setOwner(player.getGameProfile().getName()); entity.changed();
        }
    }
    @Override public boolean skipRendering(BlockState state,BlockState adjacent,Direction face) { return adjacent.is(this) || super.skipRendering(state,adjacent,face); }
    @Override public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientBlockExtensions> consumer) {
        consumer.accept(new net.minecraftforge.client.extensions.common.IClientBlockExtensions() {
            @Override public boolean addHitEffects(BlockState state,Level level,net.minecraft.world.phys.HitResult hit,net.minecraft.client.particle.ParticleEngine engine) {
                if (hit instanceof net.minecraft.world.phys.BlockHitResult blockHit) tc4revival.client.WardEffects.hit(level,blockHit);
                return true;
            }
        });
    }
    @Override public boolean canEntityDestroy(BlockState state,BlockGetter level,BlockPos pos,Entity entity) { return false; }
    @Override public void onBlockExploded(BlockState state,Level level,BlockPos pos,Explosion explosion) {}
    @Override public boolean dropFromExplosion(Explosion explosion) { return false; }

}
