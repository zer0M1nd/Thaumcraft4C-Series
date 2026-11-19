package wtmodern;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.client.renderer.entity.CreeperRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.client.event.*;

@Mod.EventBusSubscriber(modid="warptheory",value=Dist.CLIENT)
public class WarpClient {
    public static void blink(WarpNetwork.Blink msg){var level=Minecraft.getInstance().level;if(level==null)return;var r=level.random;for(int i=0;i<128;i++)level.addParticle(net.minecraft.core.particles.ParticleTypes.PORTAL,msg.x()+r.nextDouble()-r.nextDouble(),msg.y()+r.nextDouble()-r.nextDouble(),msg.z()+r.nextDouble()-r.nextDouble(),(r.nextFloat()-.5f)*.2f,(r.nextFloat()-.5f)*.2f,(r.nextFloat()-.5f)*.2f);}
    // Each legacy blood packet adds a drip source, even at an already selected position.
    private static final List<BlockPos> BLOODY=new ArrayList<>();private static int ears;private static boolean easyRecipes;private static ClientLevel lastWorld;
    public static void accept(WarpNetwork.Visual msg){switch(msg.action){case "easy_recipes"->easyRecipes=msg.amount!=0;case "ears"->ears=Math.max(0,msg.amount);case "blood"->BLOODY.add(msg.pos);case "clear_blood"->BLOODY.clear();case "clear"->{ears=0;BLOODY.clear();}}}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){if(event.phase!=TickEvent.Phase.END)return;var mc=Minecraft.getInstance();if(lastWorld!=mc.level){BLOODY.clear();if(mc.level==null)ears=0;lastWorld=mc.level;}if(mc.level==null||mc.level.getGameTime()%5!=0)return;for(var pos:BLOODY)if(mc.level.isEmptyBlock(pos.below(2))&&mc.level.getBlockState(pos.below()).blocksMotion())mc.level.addParticle(Content.BLOOD.get(),pos.getX()+mc.level.random.nextDouble(),pos.getY()-1.05,pos.getZ()+mc.level.random.nextDouble(),0,0,0);}
    @SubscribeEvent public static void chat(ClientChatReceivedEvent event){var player=Minecraft.getInstance().player;if(player==null||ears<=0||event.isSystem()||event.getSender()==null||event.getSender().equals(player.getUUID()))return;if(!(event instanceof ClientChatReceivedEvent.Player chat))return;var body=Component.literal(WarpEvents.garble(chat.getPlayerChatMessage().decoratedContent().getString(),player.getRandom()));event.setMessage(event.getBoundChatType().decorate(body));ears--;WarpNetwork.heard();}
    @Mod.EventBusSubscriber(modid="warptheory",value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
    public static class Registration {
        @SubscribeEvent public static void setup(net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent e){e.enqueueWork(()->thaumcraft.api.client.ResearchPageRenderers.register(new net.minecraft.resources.ResourceLocation("warptheory","recipe"),data->(context,g,mx,my,partial)->{
            var keys=new ArrayList<String>();for(var key:data.getAsJsonArray("recipes"))keys.add(key.getAsString()+(easyRecipes?"_easy":""));
            var page=new tc4revival.research.ResearchPage(tc4revival.research.ResearchPage.Type.valueOf(data.get("recipe_type").getAsString()),Optional.empty(),Optional.empty(),Optional.empty(),Map.of(),Optional.empty(),new tc4revival.research.ResearchPage.Reference(keys,"modern")).withId("warptheory:recipe_preview/"+String.join("/",keys));
            tc4revival.client.ResearchCraftingArtwork.draw(context.screen(),g,page,0,context.bounds().x(),context.bounds().y(),mx,my);
        }));}
        @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers e){e.registerEntityRenderer(Content.FRIEND.get(),CreeperRenderer::new);}
        @SubscribeEvent public static void particles(RegisterParticleProvidersEvent e){e.registerSpriteSet(Content.BLOOD.get(),sprites->(type,level,x,y,z,dx,dy,dz)->new BloodDrop(level,x,y,z,sprites));}
    }
    /** Original 1.7.10 blood droplet timing and gravity, with the corresponding vanilla drip atlas sprites. */
    public static class BloodDrop extends TextureSheetParticle {
        private final SpriteSet sprites;private int bob=40;
        public BloodDrop(ClientLevel level,double x,double y,double z,SpriteSet sprites){super(level,x,y,z);this.sprites=sprites;xd=yd=zd=0;setColor(.2f+random.nextFloat(),0,0);setSize(.01f,.01f);quadSize=.1f*(1+random.nextFloat());lifetime=(int)(48/(random.nextDouble()*.8+.2));setSprite(sprites.get(0,2));}
        @Override public ParticleRenderType getRenderType(){return ParticleRenderType.PARTICLE_SHEET_OPAQUE;}
        @Override public void tick(){xo=x;yo=y;zo=z;yd-=.06;if(bob-->0){xd*=.02;yd*=.02;zd*=.02;setSprite(sprites.get(0,2));}else setSprite(sprites.get(1,2));move(xd,yd,zd);xd*=.98;yd*=.98;zd*=.98;if(lifetime--<=0)remove();if(onGround){setSprite(sprites.get(2,2));xd*=.7;zd*=.7;}var pos=BlockPos.containing(x,y,z);var state=level.getBlockState(pos);if((state.blocksMotion()||!state.getFluidState().isEmpty())&&y<pos.getY()+(state.getFluidState().isEmpty()?1:state.getFluidState().getHeight(level,pos)))remove();}
    }
}
