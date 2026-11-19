package thmodern.blood;

import net.minecraft.world.effect.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraftforge.registries.*;
import thmodern.Horizons;

public final class BloodEffects {
    public static final DeferredRegister<MobEffect> REGISTRY=DeferredRegister.create(ForgeRegistries.MOB_EFFECTS,Horizons.ID);
    public static final RegistryObject<MobEffect> VIS_REGEN=REGISTRY.register("vis_regeneration",()->new Effect("vis_regeneration",5701759));
    public static final RegistryObject<MobEffect> VACUUM=REGISTRY.register("vacuum",()->new Effect("vacuum",4210752));
    public static final RegistryObject<MobEffect> SHOCK=REGISTRY.register("shock",()->new Effect("shock",11513861));
    public static final RegistryObject<MobEffect> SYNTHESIS=REGISTRY.register("synthesis",()->new Effect("synthesis",3253280));
    private static final class Effect extends MobEffect {
        private final String kind;
        Effect(String kind,int color){super(MobEffectCategory.BENEFICIAL,color);this.kind=kind;}
        @Override public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientMobEffectExtensions> consumer){consumer.accept(new thmodern.client.BloodEffectIcons(()->kind));}
        @Override public boolean isDurationEffectTick(int ticks,int amplifier){return true;}
        @Override public void applyEffectTick(LivingEntity target,int amplifier){
            if(!(target.level() instanceof ServerLevel level))return;
            int boost=Math.max(0,Math.min(255,amplifier));
            switch(kind){
                case "vis_regeneration" -> {
                    if(target.tickCount%Math.max(1,20-3*boost)!=0)return;
                    String key=java.util.List.of("aer","terra","ignis","aqua","ordo","perditio").get(level.random.nextInt(6));
                    level.addFreshEntity(new tc4revival.content.AspectOrbEntity(level,target.getX(),target.getY(),target.getZ(),tc4revival.aspect.TCAspects.catalog().require(key),1));
                }
                case "synthesis" -> {
                    if(target.tickCount%Math.max(1,15-2*boost)==0&&level.isDay()&&level.canSeeSky(target.blockPosition())){target.heal(.5f);if(target instanceof Player player)player.getFoodData().eat(1,.5f);}
                }
                case "vacuum" -> {
                    double radius=10+2*boost;
                    for(var item:level.getEntitiesOfClass(ItemEntity.class,target.getBoundingBox().inflate(radius),e->e.isAlive()&&e.distanceToSqr(target)<=radius*radius)){
                        var difference=target.position().add(0,target.getBbHeight()*.5,0).subtract(item.position());if(difference.lengthSqr()<1e-8)continue;
                        var motion=item.getDeltaMovement().add(difference.normalize().scale(.1));
                        item.setDeltaMovement(Mth.clamp(motion.x,-.35,.35),Mth.clamp(motion.y,-.35,.35),Mth.clamp(motion.z,-.35,.35));item.hasImpulse=true;
                    }
                }
                case "shock" -> {
                    for(var victim:level.getEntitiesOfClass(LivingEntity.class,target.getBoundingBox().inflate(10),e->e!=target&&e.isAlive()&&e.distanceToSqr(target)<=100)){
                        if(level.random.nextInt(1000)>=20*(1+boost))continue;
                        victim.hurt(target instanceof Player player?target.damageSources().playerAttack(player):target.damageSources().magic(),1);
                        var from=target.position().add(0,target.getBbHeight()/2,0);var to=victim.position().add(0,victim.getBbHeight()/2,0);
                        tc4revival.network.FocusEffectPacket.lightning(level,from,to);
                        level.playSound(null,target.blockPosition(),tc4revival.content.TCSounds.ZAP.get(),net.minecraft.sounds.SoundSource.PLAYERS,1,1+(level.random.nextFloat()-level.random.nextFloat())*.2f);
                    }
                }
            }
        }
    }
    private BloodEffects(){}
}
