package automagy.content;
import net.minecraft.world.item.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.*;
import net.minecraft.world.level.Level;
public final class ExperiencePhialItem extends Item {
 public ExperiencePhialItem(){super(new Properties().craftRemainder(tc4revival.content.TCItems.EMPTY_PHIAL.get()));}
 public static int value(){return automagy.AutomagyConfig.PHIAL_XP.get();}
 public static ItemStack empty(){return new ItemStack(tc4revival.content.TCItems.EMPTY_PHIAL.get());}
 @Override public boolean isFoil(ItemStack stack){return true;}
 @Override public int getUseDuration(ItemStack stack){return 12;}
 @Override public UseAnim getUseAnimation(ItemStack stack){return UseAnim.DRINK;}
 @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){return ItemUtils.startUsingInstantly(level,player,hand);}
 @Override public ItemStack finishUsingItem(ItemStack stack,Level level,LivingEntity entity){if(!(entity instanceof Player player))return stack;if(!level.isClientSide){player.giveExperiencePoints(value());level.playSound(null,player.blockPosition(),net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP,net.minecraft.sounds.SoundSource.PLAYERS,.5f,.9f);}return ItemUtils.createFilledResult(stack,player,empty());}
 @Override public InteractionResult onItemUseFirst(ItemStack stack,net.minecraft.world.item.context.UseOnContext context){var level=context.getLevel();var pos=context.getClickedPos();var player=context.getPlayer();if(player==null||!player.mayBuild()||!ExperienceJarBlockEntity.convertible(level,pos))return InteractionResult.PASS;if(!level.isClientSide){var jar=ExperienceJarBlockEntity.getOrConvert(level,pos);if(jar!=null&&jar.insertExact(value())){player.setItemInHand(context.getHand(),ItemUtils.createFilledResult(stack,player,empty()));level.playSound(null,pos,net.minecraft.sounds.SoundEvents.GENERIC_SWIM,net.minecraft.sounds.SoundSource.BLOCKS,.25f,1);}}return InteractionResult.sidedSuccess(level.isClientSide);}
 @Override public void appendHoverText(ItemStack stack,Level level,java.util.List<net.minecraft.network.chat.Component> lines,TooltipFlag flag){lines.add(net.minecraft.network.chat.Component.translatable("tooltip.automagy.experience_phial",value()));}
}
