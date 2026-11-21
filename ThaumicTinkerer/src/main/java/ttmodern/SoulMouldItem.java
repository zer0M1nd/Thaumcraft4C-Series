package ttmodern;

import net.minecraft.world.item.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.*;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

public final class SoulMouldItem extends Item {
    public SoulMouldItem(){super(new Properties().stacksTo(1));}
    public static String pattern(ItemStack stack){return stack.hasTag()?stack.getTag().getString("pattern"):"";}
    public static Component patternName(ItemStack stack){var id=ResourceLocation.tryParse(pattern(stack));var type=id==null?null:ForgeRegistries.ENTITY_TYPES.getValue(id);return type==null?Component.translatable("ttmisc.none"):type.getDescription();}
    @Override public InteractionResult interactLivingEntity(ItemStack stack,Player player,LivingEntity target,InteractionHand hand){
        if(!(target instanceof Player)&&!player.level().isClientSide){var id=ForgeRegistries.ENTITY_TYPES.getKey(target.getType());if(id!=null)stack.getOrCreateTag().putString("pattern",id.toString());}
        return InteractionResult.sidedSuccess(player.level().isClientSide);
    }
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){var stack=player.getItemInHand(hand);if(player.isShiftKeyDown()&&!level.isClientSide&&stack.hasTag())stack.getTag().remove("pattern");return InteractionResultHolder.sidedSuccess(stack,level.isClientSide);}
    @Override public void appendHoverText(ItemStack stack,Level level,java.util.List<Component> lines,TooltipFlag flag){lines.add(pattern(stack).isEmpty()?Component.translatable("ttmisc.soulMould.nonAssigned"):Component.translatable("ttmisc.soulMould.pattern",patternName(stack)));}
}
