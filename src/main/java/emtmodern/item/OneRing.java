package emtmodern.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

public class OneRing extends Item implements ICurioItem {
    public static final String CORRUPTION="emt_one_ring_corruption";
    public OneRing(){super(new Properties().stacksTo(1));}
    @Override public Component getName(ItemStack stack){return super.getName(stack).copy().withStyle(ChatFormatting.DARK_GREEN);}
    public static int corruption(Player player){return player.getPersistentData().getInt(CORRUPTION);}
    public static void invulnerable(Player player,boolean value){
        if(player.getAbilities().invulnerable!=value){player.getAbilities().invulnerable=value;if(player instanceof net.minecraft.server.level.ServerPlayer server&&server.connection!=null)server.onUpdateAbilities();}
    }
    @Override public void curioTick(SlotContext context,ItemStack stack){
        if(!(context.entity() instanceof Player player)||player.level().isClientSide)return;
        invulnerable(player,true);
        int corruption=corruption(player);applyEffect(player,OneRingRules.effect(corruption,player.getRandom()::nextInt));
        // Ability/effect synchronization recomputes potion invisibility. Set the ring flag afterwards.
        player.setInvisible(true);
        player.getPersistentData().putInt(CORRUPTION,corruption==Integer.MAX_VALUE?corruption:corruption+1);
    }
    public static void applyEffect(Player player,String effect){
        switch(effect){
            case "warning" -> player.displayClientMessage(Component.translatable("message.emt.one_ring.warning").withStyle(ChatFormatting.DARK_PURPLE),false);
            case "blindness" -> player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS,500,2,false,true));
            case "nausea" -> player.addEffect(new MobEffectInstance(MobEffects.CONFUSION,500,2,false,true));
            case "magic" -> {invulnerable(player,false);player.hurt(player.damageSources().magic(),5);}
            case "launch" -> {invulnerable(player,false);player.setDeltaMovement(player.getDeltaMovement().add(0,2,0));player.hurtMarked=true;}
            case "wither" -> {invulnerable(player,false);player.addEffect(new MobEffectInstance(MobEffects.WITHER,5000,4,false,true));}
            // Legacy setDead removed the player without normal lifecycle cleanup. Use the normal death lifecycle.
            case "death" -> {invulnerable(player,false);player.kill();}
            default -> {}
        }
    }
    @Override public void onEquip(SlotContext context,ItemStack previous,ItemStack stack){if(context.entity() instanceof Player player&&!player.level().isClientSide)player.getPersistentData().putInt(CORRUPTION,0);}
    @Override public void onUnequip(SlotContext context,ItemStack replacement,ItemStack stack){
        if(context.entity() instanceof Player player&&!player.level().isClientSide){
            player.setInvisible(false);if(!player.getAbilities().instabuild)invulnerable(player,false);
            // Do not delete the entire ForgeData compound: it also contains unrelated mod/player progress.
            player.getPersistentData().remove(CORRUPTION);
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION,500,2,false,true));
        }
    }
    @Override public boolean canEquip(SlotContext context,ItemStack stack){return context.entity() instanceof Player;}
    @Override public boolean canUnequip(SlotContext context,ItemStack stack){return context.entity() instanceof Player player&&OneRingRules.removable(corruption(player));}
    @Override public boolean canEquipFromUse(SlotContext context,ItemStack stack){return false;}
}
