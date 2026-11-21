package fmmodern;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;
import thaumcraft.api.IRepairable;

public final class TaintTools {
    public static boolean isTaint(BlockState state){var id=ForgeRegistries.BLOCKS.getKey(state.getBlock());return id!=null&&(id.getNamespace().equals("thaumcraft")||id.getNamespace().equals(ForbiddenMagic.ID))&&(id.getPath().contains("taint")||id.getPath().startsWith("flux_"));}
    public static class Pick extends PickaxeItem implements IRepairable {
        public Pick(){super(ToolTiers.TAINT,1,-2.8f,new Properties().rarity(Rarity.RARE));}
    }
    public static class Spade extends ShovelItem implements IRepairable {
        public Spade(){super(tc4revival.content.ElementalToolTier.INSTANCE,1.5f,-3,new Properties().rarity(Rarity.RARE));}
        @Override public boolean canPerformAction(ItemStack stack,net.minecraftforge.common.ToolAction action){return action==net.minecraftforge.common.ToolActions.SHOVEL_DIG;}
        @Override public float getDestroySpeed(ItemStack stack,BlockState state){return isTaint(state)&&!state.is(net.minecraft.tags.BlockTags.LOGS)&&!state.is(net.minecraft.tags.BlockTags.PLANKS)&&!state.is(net.minecraft.tags.BlockTags.MINEABLE_WITH_PICKAXE)?10:super.getDestroySpeed(stack,state);}
        @Override public InteractionResult useOn(UseOnContext context){
            var player=context.getPlayer();var level=context.getLevel();if(player==null)return InteractionResult.PASS;
            if(!(level instanceof ServerLevel server))return InteractionResult.SUCCESS;
            int purified=0;var origin=context.getClickedPos();
            for(var pos:BlockPos.betweenClosed(origin.offset(-5,-4,-5),origin.offset(5,4,5))){
                if(!level.isLoaded(pos)||!level.mayInteract(player,pos)||!player.mayUseItemAt(pos,context.getClickedFace(),context.getItemInHand()))continue;
                var id=ForgeRegistries.BLOCKS.getKey(level.getBlockState(pos).getBlock());
                if(id==null||!id.getNamespace().equals("thaumcraft")||!(id.getPath().equals("flux_goo")||id.getPath().equals("flux_gas")))continue;
                if(player instanceof net.minecraft.server.level.ServerPlayer sp&&net.minecraftforge.common.ForgeHooks.onBlockBreakEvent(server,sp.gameMode.getGameModeForPlayer(),sp,pos)==-1)continue;
                if(level.setBlock(pos,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),3)){purified++;tc4revival.network.FocusEffectPacket.nodeZap(level,player.position(),new net.minecraft.world.phys.Vec3(pos.getX()+level.random.nextFloat(),pos.getY()+level.random.nextFloat(),pos.getZ()+level.random.nextFloat()));}
            }
            if(purified==0)return InteractionResult.PASS;
            context.getItemInHand().hurtAndBreak(Math.min(15,purified),player,p->p.broadcastBreakEvent(context.getHand()));return InteractionResult.SUCCESS;
        }
    }
}
