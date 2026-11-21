package technomancy;

import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;
import thaumcraft.api.wands.IWandable;

public class NodePartBlock extends Block implements IWandable {
    public static final DirectionProperty FACING=BlockStateProperties.HORIZONTAL_FACING;
    public static final IntegerProperty COLUMN=IntegerProperty.create("column",0,2),ROW=IntegerProperty.create("row",0,2);
    public NodePartBlock(){super(Properties.copy(Blocks.IRON_BLOCK).strength(-1,3600000).noOcclusion().noLootTable());registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH).setValue(COLUMN,1).setValue(ROW,0));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(FACING,COLUMN,ROW);}
    public static BlockPos base(BlockPos pos,BlockState s){return pos.relative(s.getValue(FACING).getClockWise(),1-s.getValue(COLUMN)).below(s.getValue(ROW));}
    @Override public RenderShape getRenderShape(BlockState state){return RenderShape.INVISIBLE;}
    @Override public void onPlace(BlockState s,Level level,BlockPos pos,BlockState old,boolean moving){if(!level.isClientSide)level.scheduleTick(pos,this,20);}
    @Override public void tick(BlockState s,ServerLevel level,BlockPos pos,RandomSource random){
        var p=base(pos,s);
        if(level.hasChunkAt(p)){
            if(!(level.getBlockEntity(p) instanceof MachineEntity m)||m.kind!=MachineKind.NODE_FABRICATOR||m.facing()!=s.getValue(FACING)){level.removeBlock(pos,false);return;}
        }level.scheduleTick(pos,this,20);
    }
    @Override public InteractionResult use(BlockState s,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
        var p=base(pos,s);if(!level.hasChunkAt(p)||!(level.getBlockEntity(p) instanceof MachineEntity m))return InteractionResult.PASS;
        if(player.getItemInHand(hand).is(TechRegistry.item("potency_gem"))&&!m.boost){
            if(!level.isClientSide){MachineLogic.setBoost(m,true,player);if(!player.isCreative())player.getItemInHand(hand).shrink(1);}return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return m.getBlockState().use(level,player,hand,new BlockHitResult(hit.getLocation(),hit.getDirection(),p,hit.isInside()));
    }
    @Override public InteractionResult onWandUseFirst(ItemStack stack,UseOnContext c){
        var p=base(c.getClickedPos(),c.getLevel().getBlockState(c.getClickedPos()));
        if(c.getLevel().hasChunkAt(p)&&c.getLevel().getBlockEntity(p) instanceof MachineEntity m){if(!c.getLevel().isClientSide)m.activate(c.getPlayer());return InteractionResult.sidedSuccess(c.getLevel().isClientSide);}
        return InteractionResult.PASS;
    }
}
