package temodern.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.phys.BlockHitResult;

public final class MachineBlock extends BaseEntityBlock {
    public static final net.minecraft.world.level.block.state.properties.DirectionProperty FACING=HorizontalDirectionalBlock.FACING;
    public static final net.minecraft.world.level.block.state.properties.IntegerProperty STATE=net.minecraft.world.level.block.state.properties.IntegerProperty.create("activity",0,2);
    public final String kind;
    public MachineBlock(String kind) { super(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(3, 10).noOcclusion()); this.kind = kind;registerDefaultState(stateDefinition.any().setValue(FACING,net.minecraft.core.Direction.NORTH).setValue(STATE,0)); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(FACING,STATE);}
    @Override public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context){return defaultBlockState().setValue(FACING,context.getHorizontalDirection().getOpposite());}
    @Override public BlockState rotate(BlockState state,Rotation rotation){return state.setValue(FACING,rotation.rotate(state.getValue(FACING)));}
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new NetworkMachine(pos, state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, Machines.TYPE.get(), (world,pos,block,machine) -> machine.tick());
    }
    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity entity, ItemStack stack) {
        if (!level.isClientSide && entity instanceof Player player && level.getBlockEntity(pos) instanceof NetworkMachine machine)
            machine.node().setOwningPlayer(player);
    }
    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof NetworkMachine machine) {
            if(kind.equals("infusion_provider") && player.isShiftKeyDown() && appeng.util.InteractionUtil.canWrenchDisassemble(player.getItemInHand(hand))){
                if(level.isClientSide)return InteractionResult.SUCCESS;
                if(!(player instanceof net.minecraft.server.level.ServerPlayer server) || !level.mayInteract(player,pos)
                        || !player.mayUseItemAt(pos,hit.getDirection(),player.getItemInHand(hand))
                        || !appeng.util.Platform.hasPermissions(new appeng.api.util.DimensionalBlockPos(level,pos),player)
                        || net.minecraftforge.common.ForgeHooks.onBlockBreakEvent(level,server.gameMode.getGameModeForPlayer(),server,pos)<0)
                    return InteractionResult.FAIL;
                return level.destroyBlock(pos,!player.getAbilities().instabuild,player)?InteractionResult.CONSUME:InteractionResult.FAIL;
            }
            if(machine.memoryCard(player,player.getItemInHand(hand)))return InteractionResult.sidedSuccess(level.isClientSide);
            if(kind.equals("vis_interface") && player.isShiftKeyDown() && player.getItemInHand(hand).isEmpty()){
                if(!level.isClientSide){machine.toggleVisOutput();player.displayClientMessage(net.minecraft.network.chat.Component.translatable(machine.visOutput()?"gui.thaumicenergistics.vis_output":"gui.thaumicenergistics.vis_input"),true);}
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
            if(kind.equals("arcane_terminal")){
                if(player instanceof net.minecraft.server.level.ServerPlayer server)temodern.crafting.ArcaneTerminalMenu.open(server,machine);
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
            if(kind.equals("knowledge_inscriber")){
                if(player instanceof net.minecraft.server.level.ServerPlayer server)net.minecraftforge.network.NetworkHooks.openScreen(server,new net.minecraft.world.SimpleMenuProvider((id,inv,p)->new temodern.crafting.InscriberMenu(id,inv,machine.workstation.inventory,pos),getName()),pos);
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
            if(kind.equals("essentia_vibration_chamber")){
                if(player instanceof net.minecraft.server.level.ServerPlayer server)net.minecraftforge.network.NetworkHooks.openScreen(server,new net.minecraft.world.SimpleMenuProvider((id,inventory,p)->new temodern.crafting.GeneratorMenu(id,inventory,machine),getName()),pos);
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
            if(kind.equals("arcane_assembler")){
                if(player instanceof net.minecraft.server.level.ServerPlayer server)net.minecraftforge.network.NetworkHooks.openScreen(server,
                        new net.minecraft.world.SimpleMenuProvider((id,inventory,p)->new temodern.crafting.ArcaneAssemblerMenu(id,inventory,machine),getName()),pos);
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
            if(machine.workstation.enabled()){
                if(player instanceof net.minecraft.server.level.ServerPlayer server)net.minecraftforge.network.NetworkHooks.openScreen(server,
                        new net.minecraft.world.SimpleMenuProvider((id,inventory,p)->new temodern.crafting.WorkMenu(id,inventory,machine.workstation.inventory,pos),getName()),pos);
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
            if (kind.endsWith("gearbox")) {
                if (player instanceof net.minecraftforge.common.util.FakePlayer &&
                        (!kind.equals("thaumium_gearbox") || !player.getGameProfile().getName().equalsIgnoreCase("FakeThaumcraftGolem"))) return InteractionResult.PASS;
                if (!level.isClientSide) machine.crank();
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }
    @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        if (!state.is(next.getBlock()) && level.getBlockEntity(pos) instanceof NetworkMachine machine && !level.isClientSide) machine.dropContents();
        super.onRemove(state, level, pos, next, moving);
    }
}
