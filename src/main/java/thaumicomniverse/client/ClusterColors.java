package thaumicomniverse.client;

import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

/** One tinted model per cluster, like GT's material forms and Productive Bees' colored item variants. */
@Mod.EventBusSubscriber(modid="thaumicomniverse",bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class ClusterColors {
    private static final Map<String,Integer> COLORS=new HashMap<>();
    @SubscribeEvent public static void colors(RegisterColorHandlersEvent.Item event) {
        event.register((stack,tint)->tint==0?color(stack):-1,thaumicomniverse.ThaumicOmniverse.ORE_CLUSTER.get());
    }
    @SubscribeEvent public static void reload(ModelEvent.BakingCompleted event){COLORS.clear();}
    public static int color(ItemStack stack) {
        if(!stack.hasTag())return 0xffffff;
        String product=stack.getTag().getString("Product"),material=stack.getTag().getString("Material");
        return COLORS.computeIfAbsent(material+"|"+product,key->{
            if(net.minecraftforge.fml.ModList.get().isLoaded("gtceu")) {
                Integer nativeColor=thaumicomniverse.compat.gregtech.ArcaneGT.materialColor(material);
                if(nativeColor!=null)return nativeColor;
            }
            var id=ResourceLocation.tryParse(product);var item=id==null?null:ForgeRegistries.ITEMS.getValue(id);
            if(item==null||item==Items.AIR)return 0xffffff;
            var mc=Minecraft.getInstance();var reference=new ItemStack(item);var sprite=mc.getItemRenderer().getModel(reference,null,null,0).getParticleIcon();
            int tint=mc.getItemColors().getColor(reference,0);long red=0,green=0,blue=0,total=0;
            for(int y=0;y<sprite.contents().height();y++)for(int x=0;x<sprite.contents().width();x++) {
                int pixel=sprite.getPixelRGBA(0,x,y),alpha=pixel>>>24;if(alpha==0)continue;
                int r=pixel&255,g=pixel>>8&255,b=pixel>>16&255;
                if(tint!=-1){r=r*(tint>>16&255)/255;g=g*(tint>>8&255)/255;b=b*(tint&255)/255;}
                // Prefer the mineral's colored body over white glints and black outlines.
                int weight=alpha*(32+Math.max(r,Math.max(g,b))-Math.min(r,Math.min(g,b)));
                red+=(long)r*weight;green+=(long)g*weight;blue+=(long)b*weight;total+=weight;
            }
            return total==0?0xffffff:(int)(red/total)<<16|(int)(green/total)<<8|(int)(blue/total);
        });
    }
    private ClusterColors() {}
}
