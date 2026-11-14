package technomancy;

import net.minecraft.core.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;
import net.minecraftforge.network.NetworkHooks;
import thaumcraft.api.wands.IWandable;

public class MachineBlock extends BaseEntityBlock implements IWandable {
    public static final DirectionProperty FACING=BlockStateProperties.FACING;
    public static final BooleanProperty ACTIVE=BlockStateProperties.LIT;
    public static final BooleanProperty REVERSED=BooleanProperty.create("reversed");
    public final MachineKind kind;
    public MachineBlock(MachineKind kind){
        super(Properties.copy(Blocks.IRON_BLOCK).strength(3.5f,12).noOcclusion().lightLevel(s->s.getValue(ACTIVE)?10:0));
        this.kind=kind;registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH).setValue(ACTIVE,false).setValue(REVERSED,false));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(FACING,ACTIVE,REVERSED);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext c){
        var face=c.getHorizontalDirection().getOpposite();
        if(kind==MachineKind.NODE_FABRICATOR&&!NodeStructure.room(c.getLevel(),c.getClickedPos(),face))return null;
        return defaultBlockState().setValue(FACING,face);
    }
    @Override public RenderShape getRenderShape(BlockState s){return RenderShape.MODEL;}
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return MachineEntity.create(p,s);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> type){
        return l.isClientSide?null:createTickerHelper(type,TechRegistry.MACHINE.get(),(level,pos,state,entity)->entity.tick());
    }
    @Override public void setPlacedBy(Level l,BlockPos p,BlockState s,LivingEntity owner,ItemStack stack){
        if(l.getBlockEntity(p) instanceof MachineEntity m){if(owner!=null&&m.owner==null)m.owner=owner.getUUID();m.changed();
            if(!l.isClientSide&&kind==MachineKind.NODE_FABRICATOR&&!NodeStructure.create(m))l.destroyBlock(p,true,owner);
        }
    }
    @Override public void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving){
        if(!state.is(next.getBlock())&&kind==MachineKind.NODE_FABRICATOR)NodeStructure.clear(level,pos,state.getValue(FACING));
        super.onRemove(state,level,pos,next,moving);
    }
    @Override public InteractionResult use(BlockState s,Level l,BlockPos p,Player player,InteractionHand hand,BlockHitResult hit){
        if(!(l.getBlockEntity(p) instanceof MachineEntity m))return InteractionResult.PASS;
        var held=player.getItemInHand(hand);
        if(kind.catalyst()&&player.isShiftKeyDown()&&held.isEmpty()){if(!l.isClientSide)m.activate(player);return InteractionResult.sidedSuccess(l.isClientSide);}
        if(held.getItem() instanceof TechItem)return InteractionResult.PASS;
        if(kind==MachineKind.ESSENTIA_FUSOR&&hit.getDirection()==Direction.UP){
            double x=hit.getLocation().x-p.getX(),z=hit.getLocation().z-p.getZ();
            Direction slot=z<.33&&x>.33&&x<.66?Direction.NORTH:z>.66&&x>.33&&x<.66?Direction.SOUTH:x<.33&&z>.33&&z<.66?Direction.WEST:x>.66&&z>.33&&z<.66?Direction.EAST:null;
            if(slot!=null&&MachineLogic.fusorInteract(m,player,hand,slot))return InteractionResult.sidedSuccess(l.isClientSide);
        }
        if(m.interact(player,hand,hit.getDirection()))return InteractionResult.sidedSuccess(l.isClientSide);
        if(!l.isClientSide&&kind==MachineKind.SANGUINE_INFUSER&&m.owner==null){m.owner=player.getUUID();m.changed();}
        if(!l.isClientSide&&player instanceof ServerPlayer sp)NetworkHooks.openScreen(sp,m,p);
        return InteractionResult.sidedSuccess(l.isClientSide);
    }
    @Override public InteractionResult onWandUseFirst(ItemStack wand,UseOnContext c){
        if(c.getLevel().getBlockEntity(c.getClickedPos()) instanceof MachineEntity m){
            if(!c.getLevel().isClientSide)m.activate(c.getPlayer());
            return InteractionResult.sidedSuccess(c.getLevel().isClientSide);
        }
        return InteractionResult.PASS;
    }
    @Override public java.util.List<ItemStack> getDrops(BlockState state,net.minecraft.world.level.storage.loot.LootParams.Builder builder){
        var stack=new ItemStack(this);
        if(builder.getOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_ENTITY) instanceof MachineEntity m)m.saveToItem(stack);
        return java.util.List.of(stack);
    }
    @Override public boolean isSignalSource(BlockState state){return kind==MachineKind.ITEM_COIL||kind==MachineKind.ESSENTIA_COIL;}
    @Override public int getSignal(BlockState state,BlockGetter level,BlockPos pos,Direction side){
        return level.getBlockEntity(pos) instanceof MachineEntity m&&m.boost&&m.state.getBoolean("coilSignal")?15:0;
    }
    @Override public VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){
        if(kind==MachineKind.ESSENTIA_RESERVOIR)return box(2,2,2,14,14,14);
        if(kind.jar())return box(3,0,3,13,12,13);
        if(kind==MachineKind.ESSENTIA_FUSOR)return box(0,0,0,16,12,16);
        if(kind.crystal()){
            int stage=0;for(int i=1;i<=2;i++){if(l.getBlockState(p.below(i)).getBlock() instanceof MachineBlock b&&b.kind.crystal())stage++;else break;}
            int inset=stage==0?0:stage==1?4:6;return box(inset,0,inset,16-inset,16,16-inset);
        }
        return Shapes.block();
    }
    @Override public BlockState rotate(BlockState state,Rotation r){return state.setValue(FACING,r.rotate(state.getValue(FACING)));}
    @Override public BlockState mirror(BlockState state,Mirror m){return rotate(state,m.getRotation(state.getValue(FACING)));}
}
