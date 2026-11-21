package emtmodern.client;
import emtmodern.ElectroMagicTools;
import emtmodern.focus.EnergyBallEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tc4revival.client.ClientFocusEffects;
import tc4revival.network.FocusEffectPacket;
@Mod.EventBusSubscriber(modid="emt",bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public class EnergyBallEffects {
    @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers event){event.registerEntityRenderer(ElectroMagicTools.ENERGY_BALL_ENTITY.get(),NoopRenderer::new);}
    public static void arcs(EnergyBallEntity ball){var random=ball.level().random;for(int i=0;i<3;i++)ClientFocusEffects.infusionBolt(ball.position(),ball.position().add((random.nextFloat()-.5)*8,(random.nextFloat()-.5)*8,(random.nextFloat()-.5)*8));}
    public static void target(EnergyBallEntity ball,Entity target){var mc=Minecraft.getInstance();if(mc.getConnection()==null)return;ClientFocusEffects.receive(new FocusEffectPacket(ball.level().dimension().location(),FocusEffectPacket.Kind.LIGHTNING,ball.getBoundingBox().getCenter(),target.getBoundingBox().getCenter(),.125f,ball.level().random.nextLong()),mc.getConnection().getConnection());}
}
