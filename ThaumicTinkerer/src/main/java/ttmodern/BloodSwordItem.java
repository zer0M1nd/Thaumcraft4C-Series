package ttmodern;

import java.util.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.*;

@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid=Tinkerer.ID)
public final class BloodSwordItem extends SwordItem implements thaumcraft.api.IRepairable {
    private static final Tier TIER=new Tier(){public int getUses(){return 950;}public float getSpeed(){return 0;}public float getAttackDamageBonus(){return 0;}public int getLevel(){return 0;}public int getEnchantmentValue(){return 22;}public Ingredient getRepairIngredient(){return Ingredient.EMPTY;}};
    private static final Set<LivingEntity> SELF_DAMAGE=Collections.newSetFromMap(new IdentityHashMap<>());
    private static final UUID SPEED_ID=UUID.fromString("b7a91fa9-6861-4f1f-8dc6-eb113511dadf");
    public BloodSwordItem(){super(TIER,10,-2.4f,new Properties());}
    @Override public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer){consumer.accept(new tc4revival.client.LegacySwordPose());}
    public static boolean active(ItemStack stack){return stack.hasTag()&&stack.getTag().getBoolean("Activated");}
    @Override public UseAnim getUseAnimation(ItemStack stack){return UseAnim.BLOCK;}
    @Override public int getUseDuration(ItemStack stack){return 72000;}
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
        var stack=player.getItemInHand(hand);if(player.isShiftKeyDown()){
            if(!level.isClientSide){stack.getOrCreateTag().putBoolean("Activated",!active(stack));player.displayClientMessage(net.minecraft.network.chat.Component.translatable(active(stack)?"ttmisc.bloodSword.activateEssentiaHarvest":"ttmisc.bloodSword.deactivateEssentiaHarvest"),false);}
        }else player.startUsingItem(hand);return InteractionResultHolder.sidedSuccess(stack,level.isClientSide);
    }
    @Override public com.google.common.collect.Multimap<Attribute,AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot){
        var values=com.google.common.collect.HashMultimap.create(super.getDefaultAttributeModifiers(slot));if(slot==EquipmentSlot.MAINHAND)values.put(Attributes.MOVEMENT_SPEED,new AttributeModifier(SPEED_ID,"Cursed blade speed",.25,AttributeModifier.Operation.MULTIPLY_BASE));return values;
    }
    private static void cost(LivingEntity entity,float amount){if(!SELF_DAMAGE.add(entity))return;try{entity.hurt(entity.damageSources().magic(),amount);}finally{SELF_DAMAGE.remove(entity);}}
    @net.minecraftforge.eventbus.api.SubscribeEvent public static void attack(LivingAttackEvent event){
        var target=event.getEntity();if(target.level().isClientSide||SELF_DAMAGE.contains(target))return;
        if(event.getSource().getDirectEntity() instanceof LivingEntity attacker&&attacker.getMainHandItem().getItem() instanceof BloodSwordItem)cost(attacker,2);
        if(target instanceof Player&&target.isUsingItem()&&target.getUseItem().getItem() instanceof BloodSwordItem){event.setCanceled(true);cost(target,3);}
    }
    @net.minecraftforge.eventbus.api.SubscribeEvent public static void drops(LivingDropsEvent event){
        if(event.getEntity().level().isClientSide||!(event.getEntity() instanceof Mob)||!(event.getSource().getDirectEntity() instanceof Player player))return;
        var held=player.getMainHandItem();if(!(held.getItem() instanceof BloodSwordItem)||!active(held))return;var recipe=MobEssenceCatalog.find(event.getEntity());if(recipe==null)return;
        event.getDrops().clear();for(var aspect:recipe.aspects()){var drop=new net.minecraft.world.entity.item.ItemEntity(event.getEntity().level(),event.getEntity().getX(),event.getEntity().getY(),event.getEntity().getZ(),MobEssenceItem.stack(aspect,0));drop.setPickUpDelay(10);event.getDrops().add(drop);}
    }
}
