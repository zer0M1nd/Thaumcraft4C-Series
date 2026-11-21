package temodern.test;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import temodern.essentia.EssentiaKey;

/** Actual GPU checks for the reported fringe and native resource/GUI parsing; excluded from releases. */
public class ResourceReviewClientAudit {
    public static void verify(Minecraft client) throws Exception {
        var style=appeng.client.gui.style.StyleManager.loadStyleDoc("/screens/terminals/thaumicenergistics_arcane.json");
        if(style.getTerminalStyle().getScreenHeight(3)!=243)throw new IllegalStateException("Legacy terminal height changed");
        for(String texture:new String[]{"arcane.crafting","states"})
            client.getResourceManager().getResourceOrThrow(ResourceLocation.parse("thaumicenergistics:textures/gui/"+texture+".png"));
        var target=new TextureTarget(512,280,true,Minecraft.ON_OSX);
        var projection=new Matrix4f(RenderSystem.getProjectionMatrix());
        var sorting=RenderSystem.getVertexSorting();
        var modelView=RenderSystem.getModelViewStack();modelView.pushPose();modelView.setIdentity();
        modelView.translate(0,0,-11000);RenderSystem.applyModelViewMatrix();
        RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0,512,280,0,1000,21000),VertexSorting.ORTHOGRAPHIC_Z);
        try{
            target.setClearColor(170/255f,170/255f,170/255f,1);target.clear(Minecraft.ON_OSX);target.bindWrite(true);
            var g=new GuiGraphics(client,client.renderBuffers().bufferSource());
            var key=EssentiaKey.parse("praecantatio");
            // Reproduce AE2's amount-entry caller: blending is disabled when it invokes our renderer.
            RenderSystem.disableBlend();new temodern.client.EssentiaRendering().drawInGui(client,g,16,16,key);
            g.drawString(client.font,"GPU: AE2 key icon, terminal model, original lower GUI",40,20,0x202020,false);
            style.getTerminalStyle().getBottom().dest(16,72).blit(g);
            g.pose().pushPose();g.pose().translate(258,70,0);g.pose().scale(9,9,1);
            var item=net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(ResourceLocation.parse("thaumicenergistics:arcane_terminal"));
            g.renderItem(new ItemStack(item),0,0);g.pose().popPose();g.flush();
            try(var rendered=Screenshot.takeScreenshot(target);
                var source=NativeImage.read(client.getResourceManager().open(key.aspect().texture()))){
                rendered.writeToFile(client.gameDirectory.toPath().resolve("resource-review-render.png"));
                int softPixels=0;
                for(int y=0;y<16;y++)for(int x=0;x<16;x++){
                    int tex=source.getPixelRGBA(Math.min(source.getWidth()-1,(2*x+1)*source.getWidth()/32),Math.min(source.getHeight()-1,(2*y+1)*source.getHeight()/32));
                    int alpha=tex>>>24;if(alpha==0||alpha==255)continue;
                    softPixels++;int actual=rendered.getPixelRGBA(16+x,16+y);
                    for(int channel=0;channel<3;channel++){
                        int tint=(key.aspect().color()>>(16-channel*8))&255;
                        double colored=((tex>>(channel*8))&255)*tint/255d;
                        int expected=(int)Math.round(colored*alpha/255d+170*(1-alpha/255d));
                        int got=(actual>>(channel*8))&255;
                        if(Math.abs(expected-got)>4)throw new IllegalStateException("Aspect alpha fringe: pixel "+x+","+y+" channel "+channel+" expected "+expected+" got "+got);
                    }
                }
                if(softPixels==0)throw new IllegalStateException("Regression fixture lacks translucent edge pixels");
            }
        }finally{
            modelView.popPose();RenderSystem.applyModelViewMatrix();RenderSystem.setProjectionMatrix(projection,sorting);
            target.destroyBuffers();client.getMainRenderTarget().bindWrite(true);RenderSystem.setShaderColor(1,1,1,1);
        }
    }
}
