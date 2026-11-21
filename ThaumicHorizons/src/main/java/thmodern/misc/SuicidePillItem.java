package thmodern.misc;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
/** A repeatable in-game death test item, retained for testing soul transfer. */
public final class SuicidePillItem extends Item {
 public SuicidePillItem(){super(new Properties());}
 @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
  if(!level.isClientSide){var key=ResourceKey.create(Registries.DAMAGE_TYPE,ResourceLocation.parse("thaumichorizons:suicide"));player.hurt(new net.minecraft.world.damagesource.DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(key)),1000);}
  return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand),level.isClientSide);
 }
 @Override public void appendHoverText(ItemStack stack,Level level,java.util.List<Component> lines,TooltipFlag flag){lines.add(Component.translatable("thaumichorizons.suicide_pill.tip"));}
}
