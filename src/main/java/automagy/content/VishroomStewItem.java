package automagy.content;
import net.minecraft.world.item.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.effect.*;
import net.minecraft.server.level.ServerPlayer;
public final class VishroomStewItem extends Item {
 public VishroomStewItem(){super(new Properties().stacksTo(1).food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(6).saturationMod(.6f).alwaysEat().build()));}
 @Override public ItemStack finishUsingItem(ItemStack stack,Level level,LivingEntity entity){var result=super.finishUsingItem(stack,level,entity);
  if(entity instanceof ServerPlayer player){
   if(!tc4revival.config.TCConfig.SERVER.disableWarp.get()){tc4revival.research.KnowledgeSavedData.get(player.server).update(player.getUUID(),level.random::nextInt,k->{k.changeWarp(tc4revival.research.PlayerKnowledge.WarpKind.TEMPORARY,level.random.nextInt(10)+8);return null;});tc4revival.content.WarpEvents.check(player);}
   player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,800,1));player.addEffect(new MobEffectInstance(tc4revival.content.TCEffects.BLURRED_VISION.get(),800,1));player.addEffect(new MobEffectInstance(MobEffects.CONFUSION,400,1));player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS,160,1));player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION,60,1));player.addEffect(new MobEffectInstance(MobEffects.POISON,60,2));
  }
  return entity instanceof net.minecraft.world.entity.player.Player player&&player.getAbilities().instabuild?result:new ItemStack(Items.BOWL);
 }
 @Override public void appendHoverText(ItemStack stack,Level level,java.util.List<net.minecraft.network.chat.Component> lines,TooltipFlag flag){lines.add(net.minecraft.network.chat.Component.translatable("tooltip.automagy.vishroom_stew"));}
}
