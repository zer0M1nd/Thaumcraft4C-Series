package heartwork.compat.blade;

import mods.flammpfeil.slashblade.capability.slashblade.NamedBladeStateCapabilityProvider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.ICapabilityProvider;

/** The old host's wrapper bridge, needed for Heartwork's eighteen wrap registrations. */
public class WrappedBladeItem extends HeartworkBladeItem {
    public WrappedBladeItem() {super("wrapped");}
    public static ItemStack wrapped(ItemStack stack) {return ItemStack.of(stack.getOrCreateTag().getCompound("WrapItem"));}
    @Override public ICapabilityProvider initCapabilities(ItemStack stack,CompoundTag nbt) {
        var provider=new NamedBladeStateCapabilityProvider(stack);
        if(!stack.getOrCreateTag().contains("bladeState"))provider.getCapability(BLADESTATE,null).ifPresent(s -> {
            s.setNonEmpty();s.setSealed(true);s.setMaxDamage(40);s.setBaseAttackModifier(4);
            s.setModel(new ResourceLocation("heartwork:blade/blade.obj"));s.setTexture(new ResourceLocation("heartwork:blade/scabbard.png"));
        });
        return provider;
    }
    @Override public boolean hurtEnemy(ItemStack stack,LivingEntity target,LivingEntity attacker) {
        var inner=wrapped(stack);
        if(!inner.isEmpty()) {
            int immunity=target.invulnerableTime;
            if(attacker instanceof net.minecraft.world.entity.player.Player player)inner.getItem().onLeftClickEntity(inner,player,target);
            target.invulnerableTime=0;inner.getItem().hurtEnemy(inner,target,attacker);target.invulnerableTime=immunity;
        }
        return super.hurtEnemy(stack,target,attacker);
    }
    @Override public boolean isValidRepairItem(ItemStack stack,ItemStack material) {
        var inner=wrapped(stack);return (!inner.isEmpty()&&inner.getItem().isValidRepairItem(inner,material))||super.isValidRepairItem(stack,material);
    }
}
