package emtmodern.item;

import ic2.api.item.*;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.resources.ResourceLocation;

public class ElectricThorHammer extends SwordItem implements IElectricItem {
    public static final int[][] LIGHTNING_OFFSETS={{8,-8},{-8,8},{-8,-8},{8,8},{8,0},{-8,0},{0,-8},{0,8},{8,1},{-8,2},{8,3},{-8,4},{8,-1},{-8,-2},{8,-3},{-8,-4},{1,8},{2,-8},{3,8},{4,-8},{-1,8},{-2,-8},{-3,8},{-4,-8}};
    public static final int[][] GUARDIAN_OFFSETS={{8,-8},{-8,8},{-8,-8},{8,8}};
    public ElectricThorHammer(){super(Tiers.DIAMOND,3,-2.4f,new Properties().durability(27));}
    @Override public boolean hurtEnemy(ItemStack stack,LivingEntity target,LivingEntity attacker){
        if(!attacker.level().isClientSide&&ElectricItem.manager.use(stack,5000,attacker))target.hurt(attacker instanceof Player player?attacker.damageSources().playerAttack(player):attacker.damageSources().mobAttack(attacker),19);return false;
    }
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
        var stack=player.getItemInHand(hand);if(player.getAbilities().instabuild)return InteractionResultHolder.success(stack);
        if(ElectricItem.manager.canUse(stack,75000)){
            player.swing(hand);if(!level.isClientSide){
                for(var offset:LIGHTNING_OFFSETS)ThorHammer.lightning(level,player.getX()+offset[0],player.getY(),player.getZ()+offset[1]);
                var type=ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation("emt","archer"));
                for(var offset:GUARDIAN_OFFSETS){var entity=type.create(level);if(entity!=null){entity.setPos(player.getX()+offset[0],player.getY()+2,player.getZ()+offset[1]);level.addFreshEntity(entity);}}
                ElectricItem.manager.use(stack,75000,player);
            }
        }else if(!level.isClientSide){ThorHammer.lightning(level,player.getX(),player.getY(),player.getZ());player.addEffect(new MobEffectInstance(MobEffects.HARM,1,1));}
        return InteractionResultHolder.success(stack);
    }
    @Override public boolean isValidRepairItem(ItemStack stack,ItemStack repair){return false;}
    @Override public boolean isRepairable(ItemStack stack){return false;}
    @Override public int getEnchantmentValue(){return emtmodern.energy.EmtEnergyConfig.ENCHANTING.get()?4:0;}
    @Override public boolean isEnchantable(ItemStack stack){return emtmodern.energy.EmtEnergyConfig.ENCHANTING.get()&&!stack.isEnchanted();}
    @Override public boolean isBookEnchantable(ItemStack stack,ItemStack book){return emtmodern.energy.EmtEnergyConfig.ENCHANTING.get();}
    @Override public boolean canProvideEnergy(ItemStack stack){return false;}
    @Override public double getMaxCharge(ItemStack stack){return 1000000;}
    @Override public int getTier(ItemStack stack){return 2;}
    @Override public double getTransferLimit(ItemStack stack){return 100;}
    @Override public boolean isBarVisible(ItemStack stack){return ElectricItem.manager.getCharge(stack)<getMaxCharge(stack);}
    @Override public int getBarWidth(ItemStack stack){return (int)Math.round(13*ElectricItem.manager.getCharge(stack)/getMaxCharge(stack));}
    @Override public int getBarColor(ItemStack stack){return 0x66bbff;}
    @Override public void appendHoverText(ItemStack stack,Level level,List<Component> lines,TooltipFlag flags){
        if(net.minecraftforge.fml.loading.FMLEnvironment.dist==net.minecraftforge.api.distmarker.Dist.CLIENT)emtmodern.client.HammerTooltips.electric(lines);
    }
}
