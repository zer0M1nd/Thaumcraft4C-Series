package conciliummodern;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;

/** Modern held-use drain; it does not enable the removed sword-blocking mechanic. */
public final class PontifexHammer extends SwordItem implements thaumcraft.api.IRepairable,thaumcraft.api.IWarpingGear {
    private static final Tier TIER=new Tier(){public int getUses(){return 1000;}public float getSpeed(){return 8;}public float getAttackDamageBonus(){return 14;}public int getLevel(){return 4;}public int getEnchantmentValue(){return 20;}public Ingredient getRepairIngredient(){return Ingredient.EMPTY;}};
    public PontifexHammer(){super(TIER,4,-2.4f,new Properties().rarity(Rarity.EPIC));}
    @Override public int getWarp(ItemStack stack,Player player){return 3;}
    @Override public void inventoryTick(ItemStack stack,Level level,Entity entity,int slot,boolean selected){super.inventoryTick(stack,level,entity,slot,selected);if(!level.isClientSide&&entity instanceof LivingEntity&&entity.tickCount%20==0&&stack.isDamaged())stack.setDamageValue(stack.getDamageValue()-1);}
    @Override public int getUseDuration(ItemStack stack){return 72000;}
    @Override public UseAnim getUseAnimation(ItemStack stack){return UseAnim.NONE;}
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){player.startUsingItem(hand);return InteractionResultHolder.consume(player.getItemInHand(hand));}
    @Override public boolean canContinueUsing(ItemStack oldStack,ItemStack newStack){return oldStack.getItem()==newStack.getItem();}
    @Override public boolean shouldCauseReequipAnimation(ItemStack oldStack,ItemStack newStack,boolean slotChanged){return slotChanged||oldStack.getItem()!=newStack.getItem();}
    @Override public void onUseTick(Level level,LivingEntity user,ItemStack stack,int remaining){
        if(!(level instanceof ServerLevel server)||!(user instanceof Player player)||remaining%20!=0)return;
        for(var target:level.getEntitiesOfClass(LivingEntity.class,player.getBoundingBox().inflate(6,2,6),e->e!=player&&e.isAlive()&&!e.isSpectator()&&!e.isAlliedTo(player))){
            if(!player.isUsingItem()||stack.isEmpty())break;
            if(target instanceof Player other&&(!server.getServer().isPvpAllowed()||!player.canHarmPlayer(other)||other.isCreative()))continue;
            if(net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.entity.player.AttackEntityEvent(player,target)))continue;
            if(target.hurt(level.damageSources().indirectMagic(player,player),(float)player.getAttributeValue(Attributes.ATTACK_DAMAGE))){
                stack.hurtAndBreak(2,player,entity->entity.broadcastBreakEvent(player.getUsedItemHand()));player.heal(2);
                CrimsonDrainNetwork.drain(player,target);
            }
        }
    }
    @Override public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer){consumer.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions(){@Override public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer(){return CrimsonObjRenderer.instance();}});}
}
