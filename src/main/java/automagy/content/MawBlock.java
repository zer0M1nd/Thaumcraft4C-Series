package automagy.content;

import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

public final class MawBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING=BlockStateProperties.FACING;
    public final boolean finical;
    public MawBlock(boolean finical){super(Properties.of().strength(1.5f,2000).noOcclusion().noCollission());this.finical=finical;registerDefaultState(stateDefinition.any().setValue(FACING,Direction.UP));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(FACING);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext context){var state=defaultBlockState().setValue(FACING,context.getClickedFace());return canSurvive(state,context.getLevel(),context.getClickedPos())?state:null;}
    @Override public boolean canSurvive(BlockState state,LevelReader reader,BlockPos pos){
        if(!(reader instanceof Level level))return false;Direction face=state.getValue(FACING);
        return InventoryTransfer.at(level,pos.relative(face.getOpposite()),face).filter(inv->inv.getSlots()>0).isPresent();
    }
    @Override public void neighborChanged(BlockState state,Level level,BlockPos pos,Block block,BlockPos from,boolean moving){
        if(!level.isClientSide&&level.hasChunkAt(pos.relative(state.getValue(FACING).getOpposite()))&&!canSurvive(state,level,pos))level.destroyBlock(pos,true);
    }
    @Override public void animateTick(BlockState state,Level level,BlockPos pos,net.minecraft.util.RandomSource random){
        if(random.nextInt(4)!=0)return;
        var face=state.getValue(FACING);var normal=net.minecraft.world.phys.Vec3.atLowerCornerOf(face.getNormal());
        var u=face.getAxis()==Direction.Axis.X?new net.minecraft.world.phys.Vec3(0,1,0):new net.minecraft.world.phys.Vec3(1,0,0);
        var v=normal.cross(u);double angle=random.nextDouble()*Math.PI*2,dist=random.nextFloat()*.5;
        var radial=u.scale(Math.cos(angle)).add(v.scale(Math.sin(angle)));var center=pos.getCenter();
        var from=center.add(normal.scale(-.2)).add(radial.scale(dist));var to=center.add(normal.scale(-.6)).add(radial.scale(.02));
        int r=random.nextInt(120),color=r<3?0x333300|Math.round((.7f+random.nextFloat()*.3f)*255):random.nextInt(26)*0x010101;
        float gravity=r<3?random.nextFloat()*.01f*(face==Direction.DOWN?1:-1):0;
        thaumcraft.api.fx.LegacyEffects.emit(level,r<3?"wisp_path":"dark_wisp_path",from,to,color,r<3?.05f:.02f,36+random.nextInt(16),1,0,gravity);
    }
    @Override public RenderShape getRenderShape(BlockState state){return RenderShape.MODEL;}
    @Override public void onPlace(BlockState state,Level level,BlockPos pos,BlockState old,boolean moving){
        super.onPlace(state,level,pos,old,moving);
        if(!level.isClientSide&&state.getValue(FACING)==Direction.UP&&level.getBlockState(pos.below()).getBlock() instanceof tc4revival.content.HungryChestBlock)level.scheduleTick(pos,this,1);
    }
    @Override public void tick(BlockState state,net.minecraft.server.level.ServerLevel level,BlockPos pos,net.minecraft.util.RandomSource random){
        if(state.getValue(FACING)==Direction.UP&&level.getBlockState(pos.below()).getBlock() instanceof tc4revival.content.HungryChestBlock)level.destroyBlock(pos,true);
    }
    @Override public VoxelShape getCollisionShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return Shapes.empty();}
    @Override public VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return switch(state.getValue(FACING)){
        case UP->box(5,0,5,11,1,11);case DOWN->box(5,15,5,11,16,11);
        case NORTH->box(5,5,15,11,11,16);case SOUTH->box(5,5,0,11,11,1);
        case WEST->box(15,5,5,16,11,11);case EAST->box(0,5,5,1,11,11);
    };}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new MawBlockEntity(pos,state);}
    @Override public void entityInside(BlockState state,Level level,BlockPos pos,Entity entity){if(entity instanceof ItemEntity item&&level.getBlockEntity(pos) instanceof MawBlockEntity maw)maw.swallow(item);}
    @Override public InteractionResult use(BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
        if(!finical||!player.mayBuild())return InteractionResult.PASS;
        ItemStack held=player.getItemInHand(hand);
        // Legacy activation opens the editor; it never inserts/consumes the held paper.
        if(held.getItem() instanceof tc4revival.content.CastingWandItem
                ||held.getItem() instanceof FilterItem&&ItemFilter.load(held).populated()){
            if(player instanceof net.minecraft.server.level.ServerPlayer server)net.minecraftforge.network.NetworkHooks.openScreen(server,new net.minecraft.world.SimpleMenuProvider((id,inv,p)->new MawMenu(id,inv,pos),net.minecraft.network.chat.Component.translatable("block.automagy.maw_finical")),pos);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }
    @Override public void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving){
        if(!state.is(next.getBlock())&&level.getBlockEntity(pos) instanceof MawBlockEntity maw)maw.dropContents();super.onRemove(state,level,pos,next,moving);
    }
}
