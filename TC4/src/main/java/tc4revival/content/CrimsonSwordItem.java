package tc4revival.content;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

public  class CrimsonSwordItem extends ThaumiumSwordItem implements WarpingGear {
    public CrimsonSwordItem() { super(CultistEquipment.CrimsonTier.INSTANCE); }
    @Override public Rarity getRarity(ItemStack stack) { return Rarity.RARE; }
    @Override public int warp(ItemStack stack,Player player) { return 2; }
    @Override public void inventoryTick(ItemStack stack,Level level,Entity holder,int slot,boolean selected) { super.inventoryTick(stack,level,holder,slot,selected); VoidEquipment.repair(stack,holder); }
    @Override public boolean hurtEnemy(ItemStack stack,LivingEntity target,LivingEntity attacker) {
        if (!target.level().isClientSide && (!(target instanceof Player && attacker instanceof Player) || target.getServer()!=null && target.getServer().isPvpAllowed())) {
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,60)); target.addEffect(new MobEffectInstance(MobEffects.HUNGER,120));
        }
        return super.hurtEnemy(stack,target,attacker);
    }
    @Override public void appendHoverText(ItemStack stack,Level level,List<Component> lines,TooltipFlag flags) { lines.add(Component.translatable("enchantment.special.sapgreat").withStyle(ChatFormatting.GOLD)); }
}
