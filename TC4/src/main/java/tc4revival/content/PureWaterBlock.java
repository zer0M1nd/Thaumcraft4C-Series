package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import tc4revival.research.*;

public  class PureWaterBlock extends LiquidBlock {
    public PureWaterBlock() { super(TCFluids.PURE,Properties.copy(Blocks.WATER).noLootTable().lightLevel(state->10)); }
    @Override public net.minecraft.world.item.ItemStack pickupBlock(net.minecraft.world.level.LevelAccessor level,BlockPos pos,BlockState state) {
        if(state.getValue(LEVEL)!=0)return net.minecraft.world.item.ItemStack.EMPTY;
        return level.setBlock(pos,Blocks.AIR.defaultBlockState(),3)?new net.minecraft.world.item.ItemStack(TCItems.PURE_WATER_BUCKET.get()):net.minecraft.world.item.ItemStack.EMPTY;
    }
    @Override public void entityInside(BlockState state,Level level,BlockPos pos,Entity entity) {
        super.entityInside(state,level,pos,entity);
        if (!(entity instanceof ServerPlayer player) || state.getValue(LEVEL)!=0 || player.hasEffect(TCEffects.WARP_WARD.get())) return;
        int permanent=KnowledgeSavedData.get(player.server).snapshot(player.getUUID())
                .map(knowledge->knowledge.warp(PlayerKnowledge.WarpKind.PERMANENT)).orElse(0);
        player.addEffect(new MobEffectInstance(TCEffects.WARP_WARD.get(),WarpCleansing.wardDuration(permanent),0,true,true));
        // removeBlock preserves the existing fluid in modern Minecraft.
        level.setBlock(pos,Blocks.AIR.defaultBlockState(),3);
    }
    @Override public void animateTick(BlockState state,Level level,BlockPos pos,RandomSource random) {
        tc4revival.client.CrucibleEffects.pureWater(level,pos,state.getValue(LEVEL));
        if (random.nextInt(25)==0) level.playLocalSound(pos.getX()+random.nextFloat(),pos.getY()+1,pos.getZ()+random.nextFloat(),
                net.minecraft.sounds.SoundEvents.LAVA_POP,net.minecraft.sounds.SoundSource.BLOCKS,.1f+random.nextFloat()*.1f,.9f+random.nextFloat()*.15f,false);
    }
}
