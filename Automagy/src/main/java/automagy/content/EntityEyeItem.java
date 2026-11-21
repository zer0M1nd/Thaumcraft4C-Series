package automagy.content;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;

public final class EntityEyeItem extends Item {
    public EntityEyeItem(){super(new Properties());}
    public static boolean linked(ItemStack stack){
        if(!(stack.getItem() instanceof EntityEyeItem)||!stack.hasTag())return false;
        var tag=stack.getTag();if(tag.hasUUID("Player"))return true;
        var type=ResourceLocation.tryParse(tag.getString("EntityType"));return type!=null&&ForgeRegistries.ENTITY_TYPES.containsKey(type);
    }
    private void bind(ItemStack stack,LivingEntity target){
        var tag=stack.getOrCreateTag();tag.putBoolean("HasAge",target instanceof AgeableMob);tag.remove("Player");tag.remove("PlayerName");tag.remove("EntityType");
        if(target instanceof Player player){tag.putUUID("Player",player.getUUID());tag.putString("PlayerName",player.getGameProfile().getName());}
        else tag.putString("EntityType",ForgeRegistries.ENTITY_TYPES.getKey(target.getType()).toString());
        target.level().playSound(null,target.blockPosition(),net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME,net.minecraft.sounds.SoundSource.PLAYERS,.5f,1);
    }
    @Override public InteractionResult interactLivingEntity(ItemStack stack,Player player,LivingEntity target,InteractionHand hand){
        if(player.isShiftKeyDown())return InteractionResult.PASS;
        if(!player.level().isClientSide)bind(stack,target);
        return InteractionResult.sidedSuccess(player.level().isClientSide);
    }
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
        if(!player.isShiftKeyDown())return InteractionResultHolder.pass(player.getItemInHand(hand));
        player.startUsingItem(hand);return InteractionResultHolder.consume(player.getItemInHand(hand));
    }
    @Override public int getUseDuration(ItemStack stack){return 40;}
    @Override public UseAnim getUseAnimation(ItemStack stack){return UseAnim.BLOCK;}
    @Override public ItemStack finishUsingItem(ItemStack stack,Level level,LivingEntity entity){if(!level.isClientSide&&entity instanceof Player)bind(stack,entity);return stack;}
    @Override public boolean isFoil(ItemStack stack){return linked(stack);}
    public static boolean matches(ItemStack eye,LivingEntity entity,boolean adults,boolean children,boolean specificPlayer){
        if(!linked(eye))return false;var tag=eye.getTag();
        if(tag.hasUUID("Player"))return entity instanceof Player&&(!specificPlayer||entity.getUUID().equals(tag.getUUID("Player")));
        if(entity instanceof Player||!ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()).toString().equals(tag.getString("EntityType")))return false;
        return !(entity instanceof AgeableMob ageable)||(ageable.isBaby()?children:adults);
    }
    @Override public void appendHoverText(ItemStack stack,Level level,List<Component> lines,TooltipFlag flags){
        if(!linked(stack)){lines.add(Component.translatable("tooltip.automagy.entity_eye.unbound"));return;}
        var tag=stack.getTag();
        Component target=tag.hasUUID("Player")?Component.literal(tag.getString("PlayerName"))
                :ForgeRegistries.ENTITY_TYPES.getValue(ResourceLocation.parse(tag.getString("EntityType"))).getDescription();
        lines.add(Component.translatable("tooltip.automagy.entity_eye.target",target));
    }
}
