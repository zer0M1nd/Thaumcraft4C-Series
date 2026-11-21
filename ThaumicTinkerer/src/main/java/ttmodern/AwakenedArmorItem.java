package ttmodern;

import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import thaumcraft.api.IGoggles;
import thaumcraft.api.aspects.Aspect;

public final class AwakenedArmorItem extends IchorArmorItem implements IGoggles {
    public AwakenedArmorItem(Type type){super(type);}
    @Override public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer){consumer.accept(new AwakenedArmorClient.Extensions());}
    /** Extension hook; call on the logical server, then synchronize the owning inventory. */
    public static void configureBoots(ItemStack stack,double speed,double jump,boolean omni,boolean cancelInertia){
        if(!(stack.getItem() instanceof AwakenedArmorItem armor)||armor.getType()!=Type.BOOTS)throw new IllegalArgumentException("Awakened ichorcloth boots required");
        var tag=stack.getOrCreateTag();tag.putDouble("speed",BootMotion.multiplier(speed));tag.putDouble("jump",BootMotion.multiplier(jump));tag.putBoolean("omni",omni);tag.putBoolean("inertiacanceling",cancelInertia);
    }
    public static boolean enabled(ItemStack stack){return !stack.hasTag()||!stack.getTag().getBoolean("AbilityDisabled");}
    @Override public int getVisDiscount(ItemStack stack,Player player,Aspect aspect){return 7;}
    @Override public boolean showNodes(ItemStack stack,LivingEntity wearer){return getType()==Type.HELMET;}
    @Override public boolean showIngamePopups(ItemStack stack,LivingEntity wearer){return getType()==Type.HELMET;}
    @Override public String getArmorTexture(ItemStack stack,Entity wearer,EquipmentSlot slot,String layer){return Tinkerer.ID+":textures/model/ichor_gem_"+(slot==EquipmentSlot.LEGS?2:1)+".png";}
    @Override public InteractionResultHolder<ItemStack> use(Level world,Player player,InteractionHand hand){
        var stack=player.getItemInHand(hand);if(!player.isShiftKeyDown())return super.use(world,player,hand);
        if(!world.isClientSide){stack.getOrCreateTag().putBoolean("AbilityDisabled",enabled(stack));player.displayClientMessage(net.minecraft.network.chat.Component.translatable("ttmisc.awakenedArmor"+(enabled(stack)?0:1)),true);world.playSound(null,player.blockPosition(),net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP,net.minecraft.sounds.SoundSource.PLAYERS,.3f,.1f);}
        return InteractionResultHolder.sidedSuccess(stack,world.isClientSide);
    }
    @Override public void appendHoverText(ItemStack stack,Level world,java.util.List<net.minecraft.network.chat.Component> lines,TooltipFlag flags){if(!enabled(stack))lines.add(net.minecraft.network.chat.Component.translatable("ttmisc.awakenedArmor1"));}
}
