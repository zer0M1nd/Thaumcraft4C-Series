package emtmodern.item;

import ic2.api.item.ElectricItem;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.BonemealEvent;
import net.minecraftforge.eventbus.api.Event;
import tc4revival.network.FocusEffectPacket;

public class ElectricGrowthHoe extends ElectricTool {
    public static final ToolProfile PROFILE=new ToolProfile("electric_growth_hoe","hoe_growth",20000,1,1000,25,0,0,1,false,false,false,false);
    public ElectricGrowthHoe(){super(PROFILE);}
    @Override public boolean canProvideEnergy(ItemStack stack){return true;}
    @Override public boolean mineBlock(ItemStack stack,Level level,BlockState state,BlockPos pos,LivingEntity entity){return false;}
    @Override public boolean hurtEnemy(ItemStack stack,LivingEntity target,LivingEntity attacker){return false;}
    @Override public boolean isEnchantable(ItemStack stack){return true;}
    @Override public int getEnchantmentValue(){return 10;}
    @Override public boolean canPerformAction(ItemStack stack,net.minecraftforge.common.ToolAction action){return action==net.minecraftforge.common.ToolActions.HOE_TILL&&powered(stack);}
    @Override public InteractionResult useOn(UseOnContext context){
        var player=context.getPlayer();if(player==null)return InteractionResult.PASS;var level=context.getLevel();var stack=context.getItemInHand();BlockPos center=context.getClickedPos();boolean did=false;
        for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++){
            var pos=center.offset(x,0,z);if(ElectricItem.manager.canUse(stack,25)&&LegacyHoeTilling.till(context,pos,false)){
                if(!level.isClientSide){ElectricItem.manager.use(stack,25,player);FocusEffectPacket.blockSparkle(level,pos,8401408,2);}did=true;
            }
        }
        if(did)return InteractionResult.SUCCESS;
        if(!ElectricItem.manager.canUse(stack,250)||!level.hasChunkAt(center)||!level.mayInteract(player,center)||!player.mayUseItemAt(center,context.getClickedFace(),stack))return InteractionResult.PASS;
        var state=level.getBlockState(center);var event=new BonemealEvent(player,level,center,state,stack);if(MinecraftForge.EVENT_BUS.post(event))return InteractionResult.PASS;
        boolean fertilized=event.getResult()==Event.Result.ALLOW;
        if(!fertilized&&state.getBlock() instanceof BonemealableBlock plant&&plant.isValidBonemealTarget(level,center,state,level.isClientSide)){
            if(level instanceof ServerLevel server&&plant.isBonemealSuccess(level,level.random,center,state))plant.performBonemeal(server,level.random,center,state);fertilized=true;
        }
        if(fertilized){if(!level.isClientSide){ElectricItem.manager.use(stack,250,player);FocusEffectPacket.blockSparkle(level,center,0,3);}did=true;}
        else if(state.getBlock() instanceof tc4revival.content.MagicalSaplingBlock sapling){
            // The author requires >=250 EU but forgets the debit in this special branch.
            // Keep the free magical-tree growth and its two black sparkles.
            if(level instanceof ServerLevel server){sapling.growTree(state,server,center,level.random);FocusEffectPacket.blockSparkle(level,center,0,2);}did=true;
        }
        if(did&&!level.isClientSide)level.playSound(null,center,tc4revival.content.TCSounds.WAND.get(),net.minecraft.sounds.SoundSource.BLOCKS,.75f,.9f+level.random.nextFloat()*.2f);
        return did?InteractionResult.SUCCESS:InteractionResult.PASS;
    }
}
