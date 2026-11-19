package conciliumaudit;

import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

/** Read-only timing probes for the host's spell-data/JEI startup failure. */
@Mod.EventBusSubscriber(modid=ConciliumScenes.ID,value=Dist.CLIENT)
public final class ArsClientDiagnostics {
    private static int ticks=-1;
    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void login(ClientPlayerNetworkEvent.LoggingIn event){
        if(!ModList.get().isLoaded("arsmagicalegacy"))return;
        ticks=0;record("login-highest");
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void loggedIn(ClientPlayerNetworkEvent.LoggingIn event){
        if(ticks==0)record("login-lowest");
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if(event.phase!=TickEvent.Phase.END||ticks<0)return;
        if(Minecraft.getInstance().level==null)return;
        ticks++;if(ticks==1||ticks==20||ticks==100||ticks==600)record("tick-"+ticks);
    }
    private static void record(String phase){
        var out=new LinkedHashMap<String,Object>();out.put("phase",phase);
        try{
            var api=com.github.minecraftschurlimods.arsmagicalegacy.api.ArsMagicaAPI.get();
            var registry=api.getSpellPartRegistry();var manager=api.getSpellDataManager();
            var missing=new ArrayList<String>();int present=0;
            for(var part:registry.getValues()){
                if(manager.getDataForPart(part)==null)missing.add(registry.getKey(part).toString());else present++;
            }
            out.put("present",present);out.put("missing",missing);
            var raid=registry.getValue(ResourceLocation.parse("thaumicconcilium:crimson_raid"));
            var data=raid==null?null:manager.getDataForPart(raid);
            out.put("raid",data==null?"missing":Map.of("mana",data.manaCost(),"burnout",data.getBurnout(),"recipeSize",data.recipe().size()));
            out.put("success",true);
        }catch(Throwable error){out.put("success",false);out.put("error",error.toString());}
        ConciliumScenes.record("ars-data",out);
    }
    private ArsClientDiagnostics(){}
}
