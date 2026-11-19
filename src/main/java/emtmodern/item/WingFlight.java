package emtmodern.item;

import ic2.api.item.ElectricItem;
import ic2.api.util.Keys;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="emt")
public class WingFlight {
    public record Profile(String id,float lift,float horizontal,float glide,int energy,float fall,int discount) {}
    public static final java.util.Map<String,Profile> PROFILES=java.util.Map.of(
        "feather",new Profile("feather",.11f,.9f,.9f,0,.75f,0),
        "thaumium",new Profile("thaumium",.15f,.7f,.5f,0,.5f,4),
        "nano",new Profile("nano",.25f,.6f,.3f,5,.2f,5),
        "quantum",new Profile("quantum",.33f,.5f,.2f,7,0,6));
    public static void tick(Player player,ItemStack stack,Profile profile){
        tick(player,stack,profile,Keys.instance!=null&&Keys.instance.isJumpKeyDown(player));
    }
    public static void tick(Player player,ItemStack stack,Profile profile,boolean jump){
        var tag=stack.getOrCreateTag();boolean previous=tag.getBoolean("isJumping"),holding=tag.getBoolean("isHolding");
        tag.putBoolean("isJumping",jump);int force=Math.max(0,Math.min(8,tag.getByte("f")));
        var motion=player.getDeltaMovement();
        if(previous){tag.putBoolean("isHolding",true);tag.putByte("f",(byte)(force>7?7:force+1));}
        else if(holding){
            tag.putBoolean("isHolding",false);double lift=profile.lift()*force;
            // The author applies the impulse even when the optional EU debit cannot be paid.
            if(profile.energy()>0)ElectricItem.manager.use(stack,lift*10*profile.energy(),player);
            double x=motion.x,z=motion.z;
            if(x<.5&&x>-.5&&z<.5&&z>-.5){x/=profile.horizontal();z/=profile.horizontal();}
            motion=new net.minecraft.world.phys.Vec3(x,lift,z);tag.putByte("f",(byte)0);
            if(player.level().isClientSide){
                player.level().playLocalSound(player.getX(),player.getY(),player.getZ(),SoundEvents.GHAST_SHOOT,SoundSource.PLAYERS,1,1,false);
                for(int i=0;i<4;i++)player.level().addParticle(ParticleTypes.CLOUD,player.getX()-1+player.getRandom().nextInt(100)/50d,player.getY()-1,player.getZ()-1+player.getRandom().nextInt(100)/50d,0,-.5,0);
            }
        }
        if(previous&&!player.onGround()&&motion.y<0)motion=motion.multiply(1,profile.glide(),1);
        if(player.isInWater()&&!player.getAbilities().instabuild)motion=motion.add(0,-.03,0);
        var world=player.level();var pos=player.blockPosition();
        if(emtmodern.energy.EmtEnergyConfig.RAIN_IMPACT.get()&&!player.getAbilities().instabuild&&world.isRaining()&&world.canSeeSky(pos)&&world.getBiome(pos).value().hasPrecipitation())motion=motion.add(0,-.03,0);
        if(player.isShiftKeyDown()&&!player.onGround()&&motion.y<0)motion=motion.multiply(1,.6,1);
        player.setDeltaMovement(motion);
    }
    @SubscribeEvent public static void fallProtection(LivingHurtEvent event){
        if(event.getEntity() instanceof Player player&&event.getSource().is(net.minecraft.world.damagesource.DamageTypes.FALL)&&player.getItemBySlot(EquipmentSlot.CHEST).getItem() instanceof WingEquipment wings)event.setAmount(event.getAmount()*wings.wingProfile().fall());
    }
    @SubscribeEvent public static void risingFallDistance(TickEvent.PlayerTickEvent event){
        if(event.phase==TickEvent.Phase.END&&event.player.getItemBySlot(EquipmentSlot.CHEST).getItem() instanceof WingEquipment&&event.player.getDeltaMovement().y>0&&event.player.fallDistance>0)event.player.fallDistance-=event.player.getDeltaMovement().y;
    }
    @SubscribeEvent public static void taintedFeathers(net.minecraftforge.event.entity.living.LivingDropsEvent event){
        var id=net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(event.getEntity().getType());
        if(id!=null&&id.toString().equals("thaumcraft:tainted_chicken")){
            int count=event.getEntity().getRandom().nextInt(3);if(count>0)event.getDrops().add(new net.minecraft.world.entity.item.ItemEntity(event.getEntity().level(),event.getEntity().getX(),event.getEntity().getY()+1,event.getEntity().getZ(),new ItemStack(emtmodern.ElectroMagicTools.CONTENT.get("tainted_feather").get(),count)));
        }
    }
}
