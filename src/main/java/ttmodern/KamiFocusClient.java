package ttmodern;

@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid=Tinkerer.ID,bus=net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.MOD,value=net.minecraftforge.api.distmarker.Dist.CLIENT)
public final class KamiFocusClient {
    @net.minecraftforge.eventbus.api.SubscribeEvent public static void colors(net.minecraftforge.client.event.RegisterColorHandlersEvent.Item event){event.register((stack,layer)->layer==0?Tinkerer.XP_DRAIN_FOCUS.get().color(stack):0xffffff,Tinkerer.XP_DRAIN_FOCUS.get());}
    static void trail(ShadowbeamFocus.ShadowTrail packet){
        var mc=net.minecraft.client.Minecraft.getInstance();var level=mc.level;if(level==null)return;
        boolean own=mc.player!=null&&packet.caster()==mc.player.getId()&&mc.options.getCameraType().isFirstPerson();
        for(var p:packet.points())if(Double.isFinite(p.x)&&Double.isFinite(p.y)&&Double.isFinite(p.z)
                &&(!own||p.distanceToSqr(mc.player.getEyePosition())>=4))
            thaumcraft.api.fx.LegacyEffects.emit(level,"shadow",p,net.minecraft.world.phys.Vec3.ZERO,0x010101,2,5,1,0,-.7f);
    }
}
