package automagy.content;

import automagy.Automagy;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tc4revival.content.GolemEntity;

@Mod.EventBusSubscriber(modid=Automagy.ID)
public final class CourierLifecycle {
    private static final java.util.Map<ServerLevel,java.util.Set<java.util.UUID>> RESTORE=new java.util.WeakHashMap<>();
    public static void completed(GolemEntity golem){if(golem.level() instanceof ServerLevel level)RESTORE.computeIfAbsent(level,k->new java.util.HashSet<>()).add(golem.getUUID());}
    @SubscribeEvent public static void joined(net.minecraftforge.event.entity.EntityJoinLevelEvent event){
        if(event.getEntity() instanceof GolemEntity golem&&event.getLevel() instanceof ServerLevel level){
            var job=golem.configuration().addonData(InventariumDeliveryGoal.ID);
            if(job.contains("Dimension")&&!job.getString("Dimension").equals(level.dimension().location().toString())){
                release(golem,job);thaumcraft.api.golems.GolemApi.data(golem,InventariumDeliveryGoal.ID,new CompoundTag());
            }
        }
        if(event.getEntity() instanceof GolemEntity golem&&InventariumDeliveryGoal.ID.equals(golem.configuration().customTask())&&golem.configuration().addonData(InventariumDeliveryGoal.ID).isEmpty())completed(golem);
    }
    @SubscribeEvent public static void endTick(net.minecraftforge.event.TickEvent.LevelTickEvent event){
        if(event.phase!=net.minecraftforge.event.TickEvent.Phase.END||!(event.level instanceof ServerLevel level))return;
        var pending=RESTORE.remove(level);if(pending==null)return;
        for(var id:pending)if(level.getEntity(id) instanceof GolemEntity golem&&golem.isAlive()&&InventariumDeliveryGoal.ID.equals(golem.configuration().customTask())&&golem.configuration().addonData(InventariumDeliveryGoal.ID).isEmpty())
            thaumcraft.api.golems.GolemApi.configure(golem,golem.configuration().customMaterial(),null,golem.configuration().blockAppearance());
    }
    private static void release(GolemEntity golem,CompoundTag job){
        if(!(golem.level() instanceof ServerLevel current)||!job.hasUUID("Book")||!job.contains("Core",Tag.TAG_COMPOUND))return;
        ServerLevel owner=current;
        if(job.contains("Dimension")){
            var dimension=net.minecraft.resources.ResourceLocation.tryParse(job.getString("Dimension"));if(dimension==null)return;
            owner=current.getServer().getLevel(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,dimension));
        }
        if(owner!=null)CourierCleanupData.get(owner).add(NbtUtils.readBlockPos(job.getCompound("Core")),job.getUUID("Book"),job.getLong("Claim"),golem.getUUID());
    }
    @SubscribeEvent public static void livingTick(net.minecraftforge.event.entity.living.LivingEvent.LivingTickEvent event){
        if(!(event.getEntity() instanceof GolemEntity golem)||golem.level().isClientSide||InventariumDeliveryGoal.ID.equals(golem.configuration().customTask()))return;
        var job=golem.configuration().addonData(InventariumDeliveryGoal.ID);if(job.isEmpty())return;
        release(golem,job);thaumcraft.api.golems.GolemApi.data(golem,InventariumDeliveryGoal.ID,new CompoundTag());
        // A replacement task takes ownership of physical cargo; only the reservation is released.
    }
    @SubscribeEvent public static void removed(EntityLeaveLevelEvent event){
        if(!(event.getLevel() instanceof ServerLevel)||!(event.getEntity() instanceof GolemEntity golem))return;
        var reason=golem.getRemovalReason();
        if(reason==Entity.RemovalReason.KILLED||reason==Entity.RemovalReason.DISCARDED||reason==Entity.RemovalReason.CHANGED_DIMENSION)
            release(golem,golem.configuration().addonData(InventariumDeliveryGoal.ID));
        // Chunk unload preserves reservations. Death and bell recovery already drop cargo in the main mod.
    }
    private CourierLifecycle(){}
}
