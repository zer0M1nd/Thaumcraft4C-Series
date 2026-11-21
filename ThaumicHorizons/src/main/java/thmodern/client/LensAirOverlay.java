package thmodern.client;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import thmodern.Horizons;
import thmodern.lens.LensEquipment;
@Mod.EventBusSubscriber(modid=Horizons.ID,value=Dist.CLIENT)
public final class LensAirOverlay {
    private static final ResourceLocation RIPPLE=ResourceLocation.parse("thaumichorizons:textures/fx/ripple.png"),VORTEX=ResourceLocation.parse("thaumichorizons:textures/fx/vortex.png");
    @SubscribeEvent public static void render(net.minecraftforge.client.event.RenderGuiEvent.Post event){
        var mc=Minecraft.getInstance();if(mc.player==null||mc.level==null||mc.options.hideGui||!mc.options.getCameraType().isFirstPerson()||!LensEquipment.active(mc.player).equals("air"))return;
        var g=event.getGuiGraphics();var camera=mc.gameRenderer.getMainCamera();float partial=event.getPartialTick();
        for(var entity:mc.level.getEntitiesOfClass(LivingEntity.class,mc.player.getBoundingBox().inflate(24),e->e!=mc.player&&e.isAlive())){
            var relative=entity.getPosition(partial).add(0,entity.getBbHeight()/2,0).subtract(camera.getPosition());double distance=relative.length();if(distance<.1)continue;
            var projected=thmodern.lens.LensProjection.project(new org.joml.Vector3f((float)relative.x,(float)relative.y,(float)relative.z),camera.rotation(),g.guiWidth(),g.guiHeight(),mc.options.fov().get());if(projected==null)continue;
            double x=projected.x(),y=projected.y();
            float size=(float)Math.min(200,Math.min(entity.getBbWidth(),entity.getBbHeight())*Math.min(g.guiWidth(),g.guiHeight())/distance);
            if(entity.getMobType()==MobType.UNDEAD){float alpha=(float)Math.max(0,.1-distance/240);vortex(g,x,y,size*2,entity.tickCount%360,alpha);vortex(g,x,y,size,-(entity.tickCount%360),alpha);}
            else {int rings=Math.min(4,(int)(48/distance+1));float alpha=(float)Math.max(0,.3-distance/80);for(int i=0;i<rings;i++)draw(g,RIPPLE,x,y,size*((i*16/(rings+1)+entity.tickCount%16)%16)/12,0,alpha);}
        }
        g.setColor(1,1,1,1);
    }
    private static void vortex(GuiGraphics g,double x,double y,float radius,float angle,float alpha){
        // Original 1.8.21 uses a diamond: UV (0,0) is at its bottom tip,
        // (1,0) at its right tip. A square of half-width radius is sqrt(2) too large.
        g.pose().pushPose();g.pose().translate(x,y,0);
        g.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(angle-45));
        draw(g,VORTEX,0,0,radius/(float)Math.sqrt(2),0,alpha,true);
        g.pose().popPose();
    }
    private static void draw(GuiGraphics g,ResourceLocation texture,double x,double y,float size,float rotation,float alpha){draw(g,texture,x,y,size,rotation,alpha,false);}
    private static void draw(GuiGraphics g,ResourceLocation texture,double x,double y,float size,float rotation,float alpha,boolean flipV){if(size<=0||alpha<=0)return;g.pose().pushPose();g.pose().translate(x,y,0);g.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(rotation));g.pose().scale(size,size,1);g.setColor(1,1,1,alpha);tc4revival.client.LegacyGui.blit(g,texture,-1,-1,2,2,0,flipV?64:0,64,flipV?-64:64,64,64);g.pose().popPose();}
}
