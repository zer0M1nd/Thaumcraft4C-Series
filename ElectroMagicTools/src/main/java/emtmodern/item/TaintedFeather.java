package emtmodern.item;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import tc4revival.content.TCEffects;

public class TaintedFeather extends LegacyMaterialItem {
    public static boolean shouldPoison(LivingEntity entity,ItemStack stack,int roll){return !entity.isInvertedHealAndHarm()&&!entity.hasEffect(TCEffects.FLUX_TAINT.get())&&roll<=stack.getCount();}
    @Override public void inventoryTick(ItemStack stack,Level level,Entity entity,int slot,boolean selected){
        if(!level.isClientSide&&entity instanceof LivingEntity living&&!living.isInvertedHealAndHarm()&&!living.hasEffect(TCEffects.FLUX_TAINT.get())&&shouldPoison(living,stack,level.random.nextInt(4321)))poison(living);
    }
    public void poison(LivingEntity living){
        living.addEffect(new MobEffectInstance(TCEffects.FLUX_TAINT.get(),120,0,false,true));
        if(living instanceof Player player)for(ItemStack stack:player.getInventory().items)if(stack.is(this)){stack.shrink(1);break;}
    }
}
