package tbmodern;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.shapes.*;

/** Legacy pyrofluid is a stationary drying block, not an infinitely flowing liquid. */
public final class PyrofluidBlock extends Block {
    public static final IntegerProperty AGE=IntegerProperty.create("age",0,8);
    public PyrofluidBlock() { super(Properties.of().strength(5).noCollission().noOcclusion().randomTicks().lightLevel(s->15));registerDefaultState(stateDefinition.any().setValue(AGE,0)); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder) { builder.add(AGE); }
    @Override public void randomTick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random) {
        if(state.getValue(AGE)<8)level.setBlock(pos,state.setValue(AGE,state.getValue(AGE)+1),3);
    }
    @Override public float getDestroyProgress(BlockState state,Player player,BlockGetter level,BlockPos pos) {
        return state.getValue(AGE)<8?0:super.getDestroyProgress(state,player,level,pos);
    }
    @Override public VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context) {
        return Block.box(0,0,0,16,state.getValue(AGE)==8?1:14-state.getValue(AGE)*1.5,16);
    }
    @Override public void entityInside(BlockState state,Level level,BlockPos pos,Entity entity) {
        if(!level.isClientSide && !entity.fireImmune() && state.getValue(AGE)<8) {
            entity.setSecondsOnFire(15);entity.hurt(level.damageSources().lava(),4);
        }
    }
    @Override public List<ItemStack> getDrops(BlockState state,LootParams.Builder builder) {
        if(state.getValue(AGE)<8)return List.of();
        int remaining=Configuration.powderCount(builder.getLevel().random);var result=new ArrayList<ItemStack>();
        while(remaining>0) {int count=Math.min(64,remaining);result.add(new ItemStack(Items.BLAZE_POWDER,count));remaining-=count;}
        return result;
    }
    @Override public void animateTick(BlockState state,Level level,BlockPos pos,RandomSource random) {
        if(state.getValue(AGE)<8 && level.isEmptyBlock(pos.above()) && random.nextInt(100)==0)
            level.addParticle(ParticleTypes.LAVA,pos.getX()+random.nextDouble(),pos.getY()+.9,pos.getZ()+random.nextDouble(),0,0,0);
    }
    public static final class Bucket extends BlockItem {
        public Bucket() { super(Content.block("pyrofluid"),new Item.Properties().stacksTo(1).craftRemainder(Items.BUCKET)); }
        @Override public String getDescriptionId() {return "item.thaumicbases.pyrofluid_bucket";}
        @Override public net.minecraft.world.InteractionResult place(net.minecraft.world.item.context.BlockPlaceContext context) {
            var result=super.place(context);
            if(result.consumesAction() && !context.getLevel().isClientSide && context.getPlayer()!=null && !context.getPlayer().getAbilities().instabuild)
                context.getPlayer().setItemInHand(context.getHand(),new ItemStack(Items.BUCKET));
            return result;
        }
    }
}
