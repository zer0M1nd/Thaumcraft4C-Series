package automagy.content;
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid="automagy")
public final class MobLureEvents {
 @net.minecraftforge.eventbus.api.SubscribeEvent public static void teleport(net.minecraftforge.event.entity.EntityTeleportEvent.EnderEntity event){if(!(event.getEntity() instanceof net.minecraft.world.entity.Mob mob)||mob.level().isClientSide||mob.getTarget()!=null)return;var lure=MobLureBlockEntity.nearest(mob);if(lure==null)return;var p=lure.getBlockPos();double dx=p.getX()+.5-mob.getX(),dz=p.getZ()+.5-mob.getZ();if(dx*dx+dz*dz<=16){event.setCanceled(true);return;}double x=p.getX()+(mob.getRandom().nextBoolean()?1:-1),y=p.getY(),z=p.getZ()+(mob.getRandom().nextBoolean()?1:-1);var landing=TeleportLandingAccess.find(mob,new net.minecraft.world.phys.Vec3(x,y,z));if(landing.isEmpty()||y-landing.getAsDouble()>5){event.setCanceled(true);return;}event.setTargetX(x);event.setTargetY(y);event.setTargetZ(z);}
 private MobLureEvents(){}
}
