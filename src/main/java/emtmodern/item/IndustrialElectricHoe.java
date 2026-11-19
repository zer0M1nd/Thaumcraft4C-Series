package emtmodern.item;

import ic2.api.item.ElectricItem;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;

/** Local bridge for the author's IC2 electric hoe, absent from Refactored's registry. */
public class IndustrialElectricHoe extends ElectricTool {
    public static final ToolProfile PROFILE=new ToolProfile("industrial_electric_hoe","",10000,1,100,50,0,0,16,false,false,false,false);
    public IndustrialElectricHoe(){super(PROFILE);}
    @Override public boolean effective(BlockState state){return state.is(Blocks.DIRT)||state.is(Blocks.GRASS_BLOCK)||state.is(Blocks.COARSE_DIRT)||state.is(Blocks.PODZOL)||state.is(Blocks.MYCELIUM);}
    @Override public boolean isCorrectToolForDrops(ItemStack stack,BlockState state){return effective(state);}
    @Override public boolean onBlockStartBreak(ItemStack stack,BlockPos pos,Player player){if(!player.level().isClientSide)ElectricItem.manager.use(stack,50,player);return false;}
    @Override public boolean mineBlock(ItemStack stack,net.minecraft.world.level.Level level,BlockState state,BlockPos pos,net.minecraft.world.entity.LivingEntity entity){if(!level.isClientSide&&state.getDestroySpeed(level,pos)!=0)ElectricItem.manager.use(stack,50,entity);return true;}
    @Override public InteractionResult useOn(UseOnContext context){
        if(ElectricItem.manager.canUse(context.getItemInHand(),50)&&LegacyHoeTilling.till(context,context.getClickedPos(),true)){
            if(!context.getLevel().isClientSide)ElectricItem.manager.use(context.getItemInHand(),50,context.getPlayer());return InteractionResult.SUCCESS;
        }return InteractionResult.PASS;
    }
    @Override public boolean canPerformAction(ItemStack stack,net.minecraftforge.common.ToolAction action){return action==net.minecraftforge.common.ToolActions.HOE_TILL&&powered(stack);}
}
