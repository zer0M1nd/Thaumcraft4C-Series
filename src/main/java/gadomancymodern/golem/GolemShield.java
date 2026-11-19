package gadomancymodern.golem;
import net.minecraft.world.item.ItemStack;
import tc4revival.content.*;
public class GolemShield {
    public static boolean upgraded(GolemEntity golem){return GadGolems.data(golem).getBoolean("RunicShield");}
    public static boolean upgraded(ItemStack item){return item.hasTag()&&item.getTag().getCompound("AddonData").getCompound(GadGolems.DATA.toString()).getBoolean("RunicShield");}
    public static int capacity(GolemEntity golem){return (int)(GadGolems.silverwood(golem)?golem.getMaxHealth():golem.getMaxHealth()/2.6);}
    public static float charge(GolemEntity golem){var tag=golem.getPersistentData();if(!tag.contains("lastRunicDischarge"))tag.putInt("lastRunicDischarge",golem.tickCount);return RunicCharge.charged(golem.tickCount-tag.getInt("lastRunicDischarge"),capacity(golem));}
    public static float absorb(GolemEntity golem,float amount,net.minecraft.world.damagesource.DamageSource source){float charge=charge(golem);if(Math.floor(charge)<=0)return amount;int target=source.getEntity()!=null?source.getEntity().getId():source.is(net.minecraft.world.damagesource.DamageTypes.FALL)?-2:source.is(net.minecraft.world.damagesource.DamageTypes.FALLING_BLOCK)?-3:-1;tc4revival.network.TCNetwork.CHANNEL.send(net.minecraftforge.network.PacketDistributor.NEAR.with(()->new net.minecraftforge.network.PacketDistributor.TargetPoint(golem.getX(),golem.getY(),golem.getZ(),64,golem.level().dimension())),new tc4revival.network.RunicHitPacket(golem.getId(),target));var hit=RunicCharge.hit(charge,amount);golem.getPersistentData().putInt("lastRunicDischarge",golem.tickCount-RunicCharge.ticks(hit.nextCharge()));return hit.remainingDamage();}
}
