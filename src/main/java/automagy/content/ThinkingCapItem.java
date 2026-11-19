package automagy.content;
import net.minecraft.world.item.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
/** Uses the public configuration snapshot/setter, never reloads the entity or loses its cargo/home. */
public final class ThinkingCapItem extends Item {
 public ThinkingCapItem(){super(new Properties().rarity(Rarity.EPIC));}
 @Override public InteractionResult interactLivingEntity(ItemStack stack,Player player,LivingEntity entity,InteractionHand hand){
    if(!(entity instanceof tc4revival.content.GolemEntity golem))return InteractionResult.PASS;
    if(!player.level().isClientSide&&!golem.configuration().advanced()){
        var snapshot=new net.minecraft.nbt.CompoundTag();golem.configuration().save(snapshot);snapshot.putBoolean("advanced",true);
        golem.state().configure(tc4revival.content.GolemConfiguration.load(snapshot));golem.refreshConfiguration();
        stack.shrink(1);golem.playSound(net.minecraft.sounds.SoundEvents.ENCHANTMENT_TABLE_USE,1,1);
    }return InteractionResult.sidedSuccess(player.level().isClientSide);
 }
 @Override public void appendHoverText(ItemStack stack,net.minecraft.world.level.Level level,java.util.List<net.minecraft.network.chat.Component> lines,TooltipFlag flag){lines.add(net.minecraft.network.chat.Component.translatable("tooltip.automagy.creative_only").withStyle(net.minecraft.ChatFormatting.ITALIC));if(automagy.AutomagyConfig.THINKING_CAP_RECIPE.get())lines.add(net.minecraft.network.chat.Component.translatable("tooltip.automagy.thinking_cap"));}
}
