package automagy.content;

import automagy.logic.EssentiaLocusStructure;
import net.minecraft.core.BlockPos;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

public final class EssentiaLocusBlock extends BaseEntityBlock {
    public EssentiaLocusBlock(){super(Properties.of().strength(2.5f).noOcclusion());}
    @Override public BlockState getStateForPlacement(BlockPlaceContext context){return EssentiaLocusStructure.validate(new EssentiaLocusEnvironment(context.getLevel(),context.getClickedPos()),true)==EssentiaLocusStructure.Validity.VALID?defaultBlockState():null;}
    @Override public void onPlace(BlockState state,Level level,BlockPos pos,BlockState old,boolean moving){
        super.onPlace(state,level,pos,old,moving);
        if(!level.isClientSide&&!old.is(this)){
            if(EssentiaLocusStructure.validate(new EssentiaLocusEnvironment(level,pos),true)==EssentiaLocusStructure.Validity.VALID)level.setBlock(pos.below(),Blocks.AIR.defaultBlockState(),3);
            else{level.setBlock(pos,Blocks.AIR.defaultBlockState(),3);Block.popResource(level,pos,new net.minecraft.world.item.ItemStack(this));}
        }
    }
    @Override public InteractionResult use(BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
        var held=player.getItemInHand(hand);if(!player.mayBuild()||!held.isEmpty()&&!(held.getItem() instanceof automagy.api.LocationLink))return InteractionResult.PASS;
        if(!level.isClientSide&&level.getBlockEntity(pos) instanceof EssentiaLocusBlockEntity tile){
            if(held.isEmpty()){var eye=tile.remove();if(!player.getInventory().add(eye))player.drop(eye,false);}
            else if(tile.insert(held)&&!player.getAbilities().instabuild)held.shrink(1);
        }return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override public void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving){if(!state.is(next.getBlock())&&level.getBlockEntity(pos) instanceof EssentiaLocusBlockEntity tile)tile.dropEyes();super.onRemove(state,level,pos,next,moving);}
    private long nextNoise;
    @Override public void animateTick(BlockState state,Level level,BlockPos pos,net.minecraft.util.RandomSource random){
        var base=new net.minecraft.world.phys.Vec3(pos.getX(),pos.getY(),pos.getZ());
        if(random.nextBoolean())thaumcraft.api.fx.LegacyEffects.emit(level,"rune",base,net.minecraft.world.phys.Vec3.ZERO,0x6666e6,1,12,1,0,.02f);
        boolean feeding=level.getBlockState(pos.below()).is(Content.LOCUS_AGGREGATOR.get())&&level.getBlockEntity(pos) instanceof EssentiaLocusBlockEntity tile&&!tile.revealedAspects().isEmpty();
        int count=2+random.nextInt(2);
        for(int i=0;i<count;i++){
            double dx=-1+random.nextInt(3)+random.nextFloat(),dz=-1+random.nextInt(3)+random.nextFloat();
            var from=base.add(dx,-1.2,dz);
            int blue=0x333300|Math.round((.7f+random.nextFloat()*.3f)*255);
            if(feeding)thaumcraft.api.fx.LegacyEffects.emit(level,"sparkle_path",from,base.add(1-dx,1.5,1-dz),random.nextBoolean()?blue:0x33ffff,1,21,1,0,-.05f);
            else thaumcraft.api.fx.LegacyEffects.emit(level,"sparkle",from,net.minecraft.world.phys.Vec3.ZERO,blue,.5f+random.nextFloat(),18,1,0,-.25f);
        }
        long now=level.getGameTime();if(now>=nextNoise){nextNoise=now+500+random.nextInt(400);level.playLocalSound(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,net.minecraft.sounds.SoundEvent.createVariableRangeEvent(net.minecraft.resources.ResourceLocation.parse("thaumcraft:brain")),net.minecraft.sounds.SoundSource.BLOCKS,.05f,.2f+random.nextFloat()*.5f,false);}
    }
    @Override public RenderShape getRenderShape(BlockState state){return RenderShape.ENTITYBLOCK_ANIMATED;}
    @Override public VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return box(4,5.12,4,12,14.4,12);}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new EssentiaLocusBlockEntity(pos,state);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){return level.isClientSide?null:createTickerHelper(type,Content.ESSENTIA_LOCUS_TILE.get(),EssentiaLocusBlockEntity::tick);}
}
