package conciliummodern;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import tc4revival.client.GroupedObjMesh;

public final class CastingBraceletRenderer extends BlockEntityWithoutLevelRenderer {
    private static CastingBraceletRenderer instance;
    private static GroupedObjMesh mesh;
    private static final net.minecraft.client.model.geom.ModelPart FOCUS=ConciliumFocusGeometry.createBodyLayer().bakeRoot().getChild("focus");
    public static CastingBraceletRenderer instance(){if(instance==null)instance=new CastingBraceletRenderer();return instance;}
    private CastingBraceletRenderer(){super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels());}
    public static void reload(){mesh=null;}
    @Override public void renderByItem(ItemStack stack,ItemDisplayContext context,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        if(context.firstPerson()||context==ItemDisplayContext.THIRD_PERSON_LEFT_HAND||context==ItemDisplayContext.THIRD_PERSON_RIGHT_HAND)return;
        pose.pushPose();pose.translate(.5,.5,.5);
        if(context==ItemDisplayContext.GUI){pose.scale(.625f,-.625f,-.625f);pose.mulPose(Axis.XP.rotationDegrees(210));pose.mulPose(Axis.YP.rotationDegrees(-45));}
        if(context==ItemDisplayContext.GROUND)pose.translate(0,-.25,0);
        if(context==ItemDisplayContext.FIXED)pose.mulPose(Axis.YP.rotationDegrees(90));
        pose.scale(.5f,.5f,.5f);pose.scale(.2f,.2f,.2f);renderModel(stack,pose,buffers,light,overlay);pose.popPose();
    }
    public static void renderModel(ItemStack stack,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        var mc=Minecraft.getInstance();if(mesh==null){
            var resource=mc.getResourceManager().getResource(ResourceLocation.parse("thaumicbases:models/bracelet/bracelet.obj"));
            if(resource.isEmpty()){mesh=new GroupedObjMesh(java.util.Map.of());return;}
            try(var reader=resource.get().openAsReader()){mesh=GroupedObjMesh.read(reader);}catch(java.io.IOException failure){throw new IllegalStateException("Cannot load original bracelet",failure);}
        }
        var bracelet=(CastingBracelet)stack.getItem();var texture=ResourceLocation.parse(bracelet.definition().material());
        var out=buffers.getBuffer(RenderType.entityCutoutNoCull(texture));var frame=pose.last();float ticks=mc.player==null?0:mc.player.tickCount;
        int rodLight=bracelet.rod(stack).glowing()?(int)(205+Math.sin(ticks)*5):light;
        for(var group:mesh.groups().values())for(var v:group)out.vertex(frame.pose(),v.x(),v.y(),v.z()).color(255,255,255,255).uv(v.u(),v.v()).overlayCoords(overlay).uv2(rodLight).normal(frame.normal(),v.nx(),v.ny(),v.nz()).endVertex();
        var attached=tc4revival.content.WandFocusItem.attached(stack);if(attached.getItem() instanceof tc4revival.content.WandFocusItem focus){
            String ornament=focus.ornamentTexture(attached),depth=focus.depthTexture(attached);
            if(ornament!=null){pose.pushPose();pose.scale(5,5,5);pose.translate(0,.7,0);
                for(int face=0;face<2;face++){pose.pushPose();pose.mulPose(Axis.YP.rotationDegrees(face*90));pose.translate(-.25,-.1,.0275);pose.scale(.5f,.5f,.5f);ornament(pose,buffers,ornament,light,overlay);pose.popPose();}pose.popPose();}
            if(depth!=null){pose.pushPose();pose.scale(5,5,5);pose.translate(0,.87-.15,0);pose.scale(.165f,.1765f,.165f);focusInterior(pose,buffers,depth,light,overlay);pose.popPose();}
            pose.pushPose();pose.scale(5,5,5);pose.translate(0,.87-.0475,0);pose.scale(.525f,.5525f,.525f);
            var date=java.time.LocalDate.now();String skin=date.getMonthValue()==10&&date.getDayOfMonth()==31?"spec_h":"wand";int color=focus.color(attached);
            FOCUS.render(pose,buffers.getBuffer(RenderType.entityTranslucent(ResourceLocation.parse("thaumcraft:textures/models/"+skin+".png"))),(int)(205+Math.sin(ticks/3)*10),overlay,(color>>16&255)/255f,(color>>8&255)/255f,(color&255)/255f,depth==null?.95f:.6f);pose.popPose();
        }
    }
    private static void ornament(PoseStack pose,MultiBufferSource buffers,String texture,int light,int overlay){
        var model=Minecraft.getInstance().getModelManager().getModel(ResourceLocation.parse(texture.contains(":")?texture:"thaumcraft:item/"+texture));
        var out=buffers.getBuffer(RenderType.entityCutoutNoCull(net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS));
        for(var quad:model.getQuads(null,null,net.minecraft.util.RandomSource.create(0))){var data=quad.getVertices();int stride=data.length/4;var normal=quad.getDirection().getNormal();
            for(int i=0;i<4;i++){int j=i*stride;float x=Float.intBitsToFloat(data[j]),y=Float.intBitsToFloat(data[j+1]),z=(Float.intBitsToFloat(data[j+2])-.5f)*1.6f-.05f;
                out.vertex(pose.last().pose(),x,y,z).color(1f,1f,1f,1f).uv(Float.intBitsToFloat(data[j+4]),Float.intBitsToFloat(data[j+5])).overlayCoords(overlay).uv2(light).normal(pose.last().normal(),normal.getX(),normal.getY(),normal.getZ()).endVertex();}
        }
    }
    /** Same original unit cube used by the core WandRenderer depth layer. */
    private static void focusInterior(PoseStack pose,MultiBufferSource buffers,String texture,int light,int overlay){
        var out=texture.contains(":")?Minecraft.getInstance().getTextureAtlas(net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS).apply(ResourceLocation.parse(texture)).wrap(buffers.getBuffer(RenderType.entityCutoutNoCull(net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS))):buffers.getBuffer(RenderType.entityCutoutNoCull(ResourceLocation.parse("thaumcraft:textures/item/"+texture+".png")));
        for(var direction:net.minecraft.core.Direction.values()){var normal=direction.getNormal();var n=new net.minecraft.world.phys.Vec3(normal.getX(),normal.getY(),normal.getZ());var up=direction.getAxis()==net.minecraft.core.Direction.Axis.Y?new net.minecraft.world.phys.Vec3(0,0,1):new net.minecraft.world.phys.Vec3(0,1,0);var right=up.cross(n);var center=n.scale(.5);
            for(int i=0;i<4;i++){int sx=i==0||i==1?-1:1,sy=i==0||i==3?-1:1;var point=center.add(right.scale(sx*.5)).add(up.scale(sy*.5));out.vertex(pose.last().pose(),(float)point.x,(float)point.y,(float)point.z).color(1f,1f,1f,1f).uv((sx+1)/2f,(1-sy)/2f).overlayCoords(overlay).uv2(light).normal(pose.last().normal(),normal.getX(),normal.getY(),normal.getZ()).endVertex();}
        }
    }
}
