package thmodern.vat;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;
import net.minecraftforge.network.NetworkHooks;
import thmodern.Horizons;

public final class VatBlock extends BaseEntityBlock {
    public static final IntegerProperty PART=IntegerProperty.create("part",0,35);
    public VatBlock(){super(Properties.of().strength(3,15).sound(SoundType.WOOD).lightLevel(s->8).noOcclusion());registerDefaultState(stateDefinition.any().setValue(PART,31));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(PART);}
    @Override public RenderShape getRenderShape(BlockState state){return RenderShape.MODEL;}
    @Override public VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return state.getValue(PART)==13||state.getValue(PART)==22?Shapes.empty():Shapes.block();}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return state.getValue(PART)==31?new VatEntity(pos,state):new VatPartEntity(pos,state);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){
        if(level.isClientSide)return null;
        if(type==Horizons.VAT_PART_ENTITY.get())return createTickerHelper(type,Horizons.VAT_PART_ENTITY.get(),(world,pos,block,part)->part.tick());
        return createTickerHelper(type,Horizons.VAT_ENTITY.get(),(world,pos,block,vat)->{if(world.getGameTime()%20==0)VatStructure.check(vat);if(!vat.isRemoved())vat.tick();});
    }
    @Override public InteractionResult use(BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
        var vat=VatStructure.controller(level,pos,state);if(vat==null)return InteractionResult.PASS;
        if(player instanceof ServerPlayer server){
            if(vat.insertEffigy(server,player.getItemInHand(hand)))return InteractionResult.CONSUME;
            if(vat.exchangeJar(server,player.getItemInHand(hand)))return InteractionResult.CONSUME;
            if(vat.subject()==player)vat.release();
            else if(player.isShiftKeyDown()&&(state.getValue(PART)==31||state.getValue(PART)==13||state.getValue(PART)==22))vat.capture(player);
            else NetworkHooks.openScreen(server,vat,vat.getBlockPos());
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override public void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving){
        if(!state.is(next.getBlock())||state.getValue(PART)!=next.getValue(PART)){
            var vat=VatStructure.controller(level,pos,state);if(vat!=null)VatStructure.dismantle(vat,pos);
            super.onRemove(state,level,pos,next,moving);
        }
    }
}
