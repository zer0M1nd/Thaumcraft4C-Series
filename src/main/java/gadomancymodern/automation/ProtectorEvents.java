package gadomancymodern.automation;

import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityMobGriefingEvent;
import net.minecraftforge.event.entity.EntityTeleportEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

@Mod.EventBusSubscriber(modid="gadomancy")
public class ProtectorEvents {
    protected static final Map<Level,Set<BlockProtectorEntity>> PROTECTORS=new WeakHashMap<>();
    protected static final java.lang.reflect.Field FUSE=net.minecraftforge.fml.util.ObfuscationReflectionHelper.findField(Creeper.class,"f_32270_");
    public static void register(BlockProtectorEntity tile){if(tile.getLevel()!=null)PROTECTORS.computeIfAbsent(tile.getLevel(),world->Collections.newSetFromMap(new WeakHashMap<>())).add(tile);}
    public static void unregister(BlockProtectorEntity tile){var entries=PROTECTORS.get(tile.getLevel());if(entries!=null)entries.remove(tile);}
    public static boolean protectedAt(Level level,Vec3 pos){var entries=PROTECTORS.get(level);return entries!=null&&entries.stream().anyMatch(tile->!tile.isRemoved()&&tile.range()>0&&tile.area().contains(pos));}
    public static boolean protectedEntity(Entity entity){var entries=PROTECTORS.get(entity.level());if(entries==null)return false;
        // Preserve old AxisAlignedBB.addCoord semantics; this is expansion, not relocation.
        var bounds=entity.getBoundingBox().expandTowards(entity.position());return entries.stream().anyMatch(tile->!tile.isRemoved()&&tile.range()>0&&tile.area().intersects(bounds));
    }
    public static void resetFuse(Creeper creeper){try{FUSE.setInt(creeper,0);}catch(IllegalAccessException error){throw new IllegalStateException("Cannot apply block protector creeper suppression",error);}}
    @SubscribeEvent public static void grief(EntityMobGriefingEvent event){if(protectedEntity(event.getEntity()))event.setResult(Event.Result.DENY);}
    @SubscribeEvent public static void teleport(EntityTeleportEvent event){var entity=event.getEntity();if(!(event instanceof EntityTeleportEvent.TeleportCommand)&&!(event instanceof EntityTeleportEvent.SpreadPlayersCommand)&&entity instanceof LivingEntity&&!(entity instanceof tc4revival.content.EldritchWardenEntity)&&!(entity instanceof Player)&&entity.getType()!=EntityType.ENDER_DRAGON&&entity.getType()!=EntityType.WITHER&&protectedEntity(entity))event.setCanceled(true);}
    @SubscribeEvent public static void explosion(ExplosionEvent.Detonate event){var explosion=event.getExplosion();boolean protectedArea=explosion.getDirectSourceEntity()!=null?protectedEntity(explosion.getDirectSourceEntity()):protectedAt(event.getLevel(),explosion.getPosition());if(protectedArea)event.getAffectedBlocks().clear();}
}
