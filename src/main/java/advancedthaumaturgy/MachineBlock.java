package advancedthaumaturgy;

import net.minecraft.core.BlockPos;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

public class MachineBlock extends BaseEntityBlock {
    public enum Kind {
        FERTILIZER("thaumic_fertilizer","fertilizer"),ENGINE("essentia_engine","engine"),
        BASE("microlith_base","microlith"),FLUX("flux_dissipator","microlith"),WATCHFUL("watchful_microlith","microlith"),
        HEALING("healing_microlith","microlith"),FIRE("burning_sentry","microlith"),
        LIGHTNING("lightning_microlith","microlith"),FROST("frost_microlith","microlith"),EXCAVATOR("excavator_microlith","microlith");
        public final String id,feature;Kind(String id,String feature){this.id=id;this.feature=feature;}
        public boolean microlith(){return ordinal()>=BASE.ordinal();}
    }
    public final Kind kind;
    public MachineBlock(Kind kind){super(Properties.of().strength(1).noOcclusion().requiresCorrectToolForDrops());this.kind=kind;}
    @Override public RenderShape getRenderShape(BlockState state){return RenderShape.ENTITYBLOCK_ANIMATED;}
    @Override public VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return kind.microlith()?box(2,0,2,14,16,14):box(1,0,1,15,16,15);}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new MachineEntity(pos,state);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){return level.isClientSide?null:createTickerHelper(type,AdvancedThaumaturgy.MACHINE_ENTITY.get(),(l,p,s,e)->e.tick());}
    @Override public InteractionResult use(BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
        if(!level.mayInteract(player,pos)||!player.mayUseItemAt(pos,hit.getDirection(),player.getItemInHand(hand)))return InteractionResult.FAIL;
        if(!(level.getBlockEntity(pos) instanceof MachineEntity entity))return InteractionResult.PASS;
        if(entity.phial(player,hand))return InteractionResult.sidedSuccess(level.isClientSide);
        if(!player.getItemInHand(hand).isEmpty())return InteractionResult.PASS;
        if(kind==Kind.WATCHFUL||kind==Kind.HEALING||kind==Kind.FIRE){
            if(!level.isClientSide){entity.setActive(!entity.active());player.displayClientMessage(net.minecraft.network.chat.Component.translatable(entity.active()?"advancedthaumaturgy.active":"advancedthaumaturgy.inactive"),true);}
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if(kind==Kind.LIGHTNING||kind==Kind.FROST||kind==Kind.EXCAVATOR){if(!level.isClientSide)player.displayClientMessage(net.minecraft.network.chat.Component.translatable("advancedthaumaturgy.original_wip"),true);return InteractionResult.sidedSuccess(level.isClientSide);}
        return InteractionResult.PASS;
    }
    @Override public void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving){
        if(!state.is(next.getBlock())&&level.getBlockEntity(pos) instanceof MachineEntity machine)machine.releaseTicket();
        super.onRemove(state,level,pos,next,moving);
    }
}
