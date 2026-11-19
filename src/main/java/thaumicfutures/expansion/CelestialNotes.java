package thaumicfutures.expansion;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tc4revival.content.TCItems;

/** The server derives the sky target and obstruction from the real player's held lens and look vector. */
@Mod.EventBusSubscriber(modid="thaumicfutures")
public final class CelestialNotes {
    public static final Map<String,List<String>> TYPES=new LinkedHashMap<>();
    static {
        TYPES.put("sun",List.of("ignis","lux","potentia"));
        TYPES.put("stars_north",List.of("ordo","cognitio","alienis"));
        TYPES.put("stars_east",List.of("aer","volatus","sensus"));
        TYPES.put("stars_south",List.of("perditio","tenebrae","vacuos"));
        TYPES.put("stars_west",List.of("terra","spiritus","iter"));
        String[][] moons={{"aqua","sano","victus"},{"aqua","permutatio","auram"},{"aqua","vitreus","lux"},{"aqua","mortuus","tenebrae"},
                {"aqua","vacuos","alienis"},{"aqua","gelum","spiritus"},{"aqua","sensus","cognitio"},{"aqua","motus","potentia"}};
        for(int i=0;i<8;i++)TYPES.put("moon_"+i,List.of(moons[i]));
    }
    private record Lock(String target,int ticks){}
    private static final Map<UUID,Lock> TRACKING=new HashMap<>();
    @SubscribeEvent public static void use(LivingEntityUseItemEvent.Tick event){
        if(!(event.getEntity() instanceof ServerPlayer player)||!event.getItem().is(TCItems.THAUMOMETER.get())
                ||!thaumicfutures.FutureFeatures.Group.RESTORATION.enabled())return;
        var target=target(player);
        if(target==null){TRACKING.remove(player.getUUID());return;}
        var previous=TRACKING.get(player.getUUID());
        int ticks=previous==null||!target.equals(previous.target())||player.getTicksUsingItem()<=1?1:previous.ticks()+1;
        TRACKING.put(player.getUUID(),new Lock(target,ticks));
        if(ticks<20)return;
        TRACKING.remove(player.getUUID());
        if(player.getInventory().items.stream().noneMatch(stack->stack.is(Items.PAPER))){player.stopUsingItem();return;}
        var persistent=player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        String firstKey="OmniSkySeen_"+target;
        if(!persistent.getBoolean(firstKey)){ResearchCurio.reward(player,TYPES.get(target));persistent.putBoolean(firstKey,true);}
        long day=Math.floorDiv(player.level().getDayTime(),24000L);
        if(!persistent.contains("OmniSkyDay")||persistent.getLong("OmniSkyDay")!=day){
            for(int slot=0;slot<player.getInventory().items.size();slot++){
                var paper=player.getInventory().getItem(slot);if(!paper.is(Items.PAPER))continue;
                var note=new ItemStack(RestoredContent.NOTES.get(target).get());
                paper.shrink(1);if(!player.getInventory().add(note))player.drop(note,false);
                persistent.putLong("OmniSkyDay",day);break;
            }
        }
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG,persistent);
        player.stopUsingItem();
    }
    public static String target(Player player){
        var level=player.level();if(!level.dimensionType().hasSkyLight()||level.getRainLevel(1)>.2f)return null;
        var look=player.getLookAngle();if(look.y<.15)return null;
        var start=player.getEyePosition();var end=start.add(look.scale(Math.max(256,level.getMaxBuildHeight()-start.y+32)));
        if(level.clip(new ClipContext(start,end,ClipContext.Block.OUTLINE,ClipContext.Fluid.NONE,player)).getType()!=HitResult.Type.MISS)return null;
        double angle=level.getTimeOfDay(1)*Math.PI*2;
        // LevelRenderer rotates the celestial plane by -90 degrees around Y, then by time around X.
        var sun=new Vec3(-Math.sin(angle),Math.cos(angle),0);
        if(sun.y>0&&look.dot(sun)>.985)return "sun";
        if(sun.y<0&&look.dot(sun.scale(-1))>.985)return "moon_"+level.getMoonPhase();
        // ClientLevel does not maintain the server's sky-darken value used by isNight().
        // Use the same celestial angle on both sides, including worlds with frozen time.
        if(sun.y<0)return "stars_"+player.getDirection().getName();
        return null;
    }
    @SubscribeEvent public static void logout(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event){TRACKING.remove(event.getEntity().getUUID());}
    @SubscribeEvent public static void stopped(net.minecraftforge.event.server.ServerStoppedEvent event){TRACKING.clear();}
    private CelestialNotes(){}
}
