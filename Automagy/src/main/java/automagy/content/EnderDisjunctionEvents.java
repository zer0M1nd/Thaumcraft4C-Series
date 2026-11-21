package automagy.content;
import automagy.logic.DisjunctionRules;
import java.util.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityTeleportEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.CuriosApi;
@Mod.EventBusSubscriber(modid="automagy")
public final class EnderDisjunctionEvents {
 private EnderDisjunctionEvents(){}
 private static Map<ServerPlayer,Float> nearby(LivingEntity entity,Vec3 target,Set<ServerPlayer> omit){
    var result=new LinkedHashMap<ServerPlayer,Float>();
    for(var candidate:entity.level().players())if(candidate instanceof ServerPlayer player&&!omit.contains(player)&&!player.isSpectator()&&player.isAlive()){
        var delta=player.position().subtract(target);if(!DisjunctionRules.inRange(delta.x,delta.y,delta.z))continue;
        var inventory=CuriosApi.getCuriosInventory(player).resolve();if(inventory.isEmpty())continue;
        var equipped=inventory.get().getEquippedCurios();var values=new ArrayList<Integer>();
        for(int slot=0;slot<equipped.getSlots();slot++)if(equipped.getStackInSlot(slot).getItem() instanceof EnderDisjunctionRingItem ring)values.add(ring.damageAndPay(player));
        float damage=DisjunctionRules.damage(values);if(damage>0)result.put(player,damage);
    }return result;
 }
 @SubscribeEvent(priority=EventPriority.LOW) public static void teleport(EntityTeleportEvent.EnderEntity event){
    if(!(event.getEntity() instanceof LivingEntity entity)||entity instanceof Player||entity.level().isClientSide||!entity.isAlive())return;
    var destination=new Vec3(event.getTargetX(),event.getTargetY(),event.getTargetZ());
    if(TeleportLandingAccess.find(entity,destination).isEmpty())return;
    var origin=entity.position();var damage=nearby(entity,origin,Set.of());boolean leaving=!damage.isEmpty();
    float sourceDamage=0;for(float value:damage.values())sourceDamage+=value;
    boolean arriving=false;
    if(sourceDamage<entity.getHealth()){var arrivals=nearby(entity,destination,damage.keySet());arriving=!arrivals.isEmpty();damage.putAll(arrivals);}
    if(damage.isEmpty())return;
    float total=0;for(float value:damage.values())total+=value;
    if(sourceDamage<entity.getHealth()&&total>=entity.getHealth())entity.setPos(destination);
    for(var entry:damage.entrySet()){
        entity.invulnerableTime=0;entity.hurt(entity.damageSources().indirectMagic(entry.getKey(),entry.getKey()),entry.getValue());
        if(!entity.isAlive()){event.setCanceled(true);break;}
    }
    if(entity.level() instanceof net.minecraft.server.level.ServerLevel server){
        if(leaving)feedback(server,origin);
        if(arriving)feedback(server,destination);
    }
 }
 private static void feedback(net.minecraft.server.level.ServerLevel level,Vec3 pos){
    level.sendParticles(net.minecraft.core.particles.ParticleTypes.REVERSE_PORTAL,pos.x,pos.y+1,pos.z,24,.5,1,.5,.1);
    level.playSound(null,pos.x,pos.y+1,pos.z,tc4revival.content.TCSounds.ICE.get(),net.minecraft.sounds.SoundSource.HOSTILE,.5f,1.2f+level.random.nextFloat()/4);
    level.playSound(null,pos.x,pos.y+1,pos.z,tc4revival.content.TCSounds.ICE.get(),net.minecraft.sounds.SoundSource.HOSTILE,.3f,.4f+level.random.nextFloat()/4);
 }
}
