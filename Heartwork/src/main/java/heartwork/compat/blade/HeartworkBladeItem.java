package heartwork.compat.blade;

import java.util.List;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.ItemTags;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

/** The distinct item classes used by the original named-blade registrations. */
public class HeartworkBladeItem extends ItemSlashBlade implements thaumcraft.api.IWarpingGear {
    protected final String kind;
    public HeartworkBladeItem(String kind) { super(Tiers.IRON,4,0,new Properties().stacksTo(1));this.kind=kind; }
    @Override public int getWarp(ItemStack stack,Player player) {return kind.equals("void")?2:0;}
    @Override public void onCraftedBy(ItemStack stack,Level level,Player player) {
        super.onCraftedBy(stack,level,player);
        if(kind.equals("thw")||kind.equals("xf"))stack.getOrCreateTag().putString("HeartworkCrafter",player.getGameProfile().getName());
    }
    @Override public Rarity getRarity(ItemStack stack) {
        return stack.getCapability(BLADESTATE).map(s -> s.isDefaultBewitched()||stack.isEnchanted()?Rarity.EPIC:Rarity.RARE).orElse(Rarity.RARE);
    }
    @Override public boolean hurtEnemy(ItemStack stack,LivingEntity target,LivingEntity attacker) {
        if(kind.equals("void")&&!target.level().isClientSide&&(!(target instanceof Player&&attacker instanceof Player)||target.getServer().isPvpAllowed()))
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,60));
        return super.hurtEnemy(stack,target,attacker);
    }
    @Override public void inventoryTick(ItemStack stack,Level level,Entity holder,int slot,boolean selected) {
        super.inventoryTick(stack,level,holder,slot,selected);
        if(kind.equals("void")&&!level.isClientSide&&holder instanceof LivingEntity&&holder.tickCount%20==0&&stack.isDamaged())
            stack.setDamageValue(stack.getDamageValue()-1);
    }
    @Override public boolean isValidRepairItem(ItemStack stack,ItemStack material) {
        if(kind.equals("void")&&material.is(ItemTags.create(new ResourceLocation("forge:ingots/void"))))return true;
        return super.isValidRepairItem(stack,material);
    }
    @Override public void appendHoverText(ItemStack stack,Level level,List<Component> lines,TooltipFlag flag) {
        super.appendHoverText(stack,level,lines,flag);
        if(kind.equals("void"))lines.add(Component.translatable("enchantment.special.sapless"));
        if(kind.equals("tls"))lines.add(Component.translatable("blades.crafter").append(": ").append(Component.translatable("the_last_smith.name")));
        else if(stack.hasTag()&&stack.getTag().contains("HeartworkCrafter"))
            lines.add(Component.translatable("blades.crafter").append(": "+stack.getTag().getString("HeartworkCrafter")));
    }
}
