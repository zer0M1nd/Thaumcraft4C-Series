package tc4revival.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.BlockFaceUV;
import net.minecraft.client.renderer.block.model.FaceBakery;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector3f;
import tc4revival.content.EssentiaTubeBlockEntity;
import tc4revival.content.EssentiaTubeFlow.Kind;

/** Legacy tube dimensions and separate valve attachments; no world state is changed while rendering. */
public  class EssentiaTubeRenderer implements BlockEntityRenderer<EssentiaTubeBlockEntity> {
    private static final ResourceLocation VALVE=ResourceLocation.parse("thaumcraft:textures/models/valve.png"),HANDLE=ResourceLocation.parse("thaumcraft:item/pipe_valve_handle");
    private record Box(String texture,float x,float y,float z,float X,float Y,float Z) {}
    private static final Map<Box,List<BakedQuad>> CUBES=new HashMap<>();
    private static final FaceBakery BAKERY=new FaceBakery();
    private static BlockEntityWithoutLevelRenderer itemRenderer;
    private final net.minecraft.client.model.geom.ModelPart rod;
    public EssentiaTubeRenderer(BlockEntityRendererProvider.Context context) {
        var mesh=new net.minecraft.client.model.geom.builders.MeshDefinition();
        mesh.getRoot().addOrReplaceChild("rod",net.minecraft.client.model.geom.builders.CubeListBuilder.create().texOffs(0,10).addBox(-1,2,-1,2,2,2),net.minecraft.client.model.geom.PartPose.ZERO);
        rod=net.minecraft.client.model.geom.builders.LayerDefinition.create(mesh,64,32).bakeRoot();
    }
    public static void reload(net.minecraft.server.packs.resources.ResourceManager resources) { CUBES.clear(); }
    @Override public void render(EssentiaTubeBlockEntity tube,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        EssentiaVentParticles.maintain(tube);
        var kind=tube.flow().kind(); boolean item=tube.getLevel()==null;
        int metal=0xffffff;
        String prefix="";
        String texture=prefix+(kind==Kind.BUFFER ? "pipe_buffer" : kind==Kind.RESTRICTED ? "pipe_restrict" : "pipe_1");
        boolean[] connected=new boolean[6]; int[] extension=new int[6]; boolean external=false; int axes=0;
        for (var side:Direction.values()) {
            boolean bellows=kind==Kind.BUFFER && tube.adjacent(side) instanceof tc4revival.content.BellowsBlockEntity b && b.facing()==side.getOpposite();
            connected[side.get3DDataValue()]=item ? side.getAxis()==Direction.Axis.Y : tube.connectable(side) && (tube.neighbor(side)!=null || bellows);
            if (!item && connected[side.get3DDataValue()] && !(tube.adjacent(side) instanceof EssentiaTubeBlockEntity)) extension[side.get3DDataValue()]=6;
            if (!item && connected[side.get3DDataValue()] && !(tube.adjacent(side) instanceof tc4revival.content.EssentiaJarBlockEntity)
                    && (!(tube.adjacent(side) instanceof EssentiaTubeBlockEntity other) || other.flow().kind()==Kind.BUFFER)) external=true;
        }
        if (connected[4] || connected[5]) { cube(pose,buffers,light,overlay,texture,connected[4]?-extension[4]:7,7,7,connected[5]?16+extension[5]:9,9,9,metal); axes++; }
        if (connected[0] || connected[1]) { cube(pose,buffers,light,overlay,texture,7,connected[0]?-extension[0]:7,7,9,connected[1]?16+extension[1]:9,9,metal); axes++; }
        if (connected[2] || connected[3]) { cube(pose,buffers,light,overlay,texture,7,7,connected[2]?-extension[2]:7,9,9,connected[3]?16+extension[3]:9,metal); axes++; }
        if (kind==Kind.FILTER) {
            cube(pose,buffers,light,overlay,prefix+"pipe_filter",5.5f,5.5f,5.5f,10.5f,10.5f,10.5f,metal);
            cube(pose,buffers,light,overlay,"pipe_filter_core",5.5f,5.5f,5.5f,10.5f,10.5f,10.5f,tube.flow().filter()==null ? metal : tube.flow().filter().color());
        } else if (kind==Kind.BUFFER) cube(pose,buffers,light,overlay,texture,4,4,4,12,12,12,metal);
        else if (kind==Kind.VALVE || !item && (axes==0 || external)) cube(pose,buffers,light,overlay,prefix+"pipe_2",6,6,6,10,10,10,metal);
        else cube(pose,buffers,light,overlay,kind==Kind.RESTRICTED ? texture : prefix+"pipe_3",6.5f,6.5f,6.5f,9.5f,9.5f,9.5f,metal);
        if (item && kind==Kind.ONEWAY) cube(pose,buffers,light,overlay,"pipe_oneway",6.984f,1.6f,6.984f,9.016f,14.4f,9.016f,0xffffff);
        if (kind==Kind.VALVE) {
            pose.pushPose(); orient(pose,item ? Direction.EAST : tube.flow().facing());
            pose.mulPose(Axis.YP.rotationDegrees(-tube.rotation()*1.5f)); pose.translate(0,-tube.rotation()/360*.12f,0);
            rod.render(pose,buffers.getBuffer(RenderType.entityCutoutNoCull(VALVE)),light,overlay);
            pose.mulPose(Axis.XP.rotationDegrees(90)); pose.translate(0,0,-.275); pose.scale(.5f,.5f,.4f);
            var mc=Minecraft.getInstance(); var model=mc.getModelManager().getModel(HANDLE);
            mc.getItemRenderer().render(new net.minecraft.world.item.ItemStack(tube.getBlockState().getBlock()),net.minecraft.world.item.ItemDisplayContext.NONE,false,pose,buffers,light,overlay,model);
            pose.popPose();
        }
        renderAttachments(tube,partial,pose,buffers,light,overlay);
        if (!item && kind==Kind.ONEWAY && tube.neighbor(tube.flow().facing().getOpposite())!=null) {
            pose.pushPose(); orient(pose,tube.flow().facing()); pose.scale(1.1f,.5f,1.1f); pose.translate(0,-.5,0);
            for (int index=0;index<3;index++) { rod.render(pose,buffers.getBuffer(RenderType.entityCutoutNoCull(VALVE)),light,overlay,.45f,.5f,1,1); pose.translate(0,-.25,0); } pose.popPose();
        }
        if (!item && kind==Kind.BUFFER) for (var side:Direction.values()) if (tube.flow().choke(side)>0 && tube.flow().open(side) && tube.neighbor(side)!=null) {
            pose.pushPose(); orient(pose,side.getOpposite()); pose.scale(1.2f,1,1.2f); pose.translate(0,-.5,0); boolean closed=tube.flow().choke(side)==2;
            rod.render(pose,buffers.getBuffer(RenderType.entityCutoutNoCull(VALVE)),light,overlay,closed?1:.3f,.3f,closed?.3f:1,1); pose.popPose();
        }
    }
    protected ResourceLocation textureId(String texture) { return ResourceLocation.parse("thaumcraft:block/"+texture); }
    protected float[] textureUv(String texture) { return null; }
    protected void renderAttachments(EssentiaTubeBlockEntity tube,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {}
    private static void orient(PoseStack pose,Direction direction) {
        pose.translate(.5,.5,.5);
        if (direction.getAxis().isHorizontal()) pose.mulPose(Axis.YP.rotationDegrees(90));
        else { pose.mulPose(Axis.XP.rotationDegrees(-90)); pose.mulPose(Axis.XP.rotationDegrees(90*direction.getStepY())); }
        pose.mulPose(new org.joml.Quaternionf().rotationAxis((float)Math.PI/2,direction.getStepX(),direction.getStepY(),direction.getStepZ()));
    }
    protected void cube(PoseStack pose,MultiBufferSource buffers,int light,int overlay,String texture,float x,float y,float z,float X,float Y,float Z,int color) {
        // Bake each extension inside a local 0..16 texture cell. Default BlockElement UVs
        // otherwise leave the sprite and sample unrelated atlas pixels inside the jar.
        if(x<0){cube(pose,buffers,light,overlay,texture,0,y,z,X,Y,Z,color);pose.pushPose();pose.translate(-1,0,0);cube(pose,buffers,light,overlay,texture,x+16,y,z,16,Y,Z,color);pose.popPose();return;}
        if(y<0){cube(pose,buffers,light,overlay,texture,x,0,z,X,Y,Z,color);pose.pushPose();pose.translate(0,-1,0);cube(pose,buffers,light,overlay,texture,x,y+16,z,X,16,Z,color);pose.popPose();return;}
        if(z<0){cube(pose,buffers,light,overlay,texture,x,y,0,X,Y,Z,color);pose.pushPose();pose.translate(0,0,-1);cube(pose,buffers,light,overlay,texture,x,y,z+16,X,Y,16,color);pose.popPose();return;}
        if(X>16){cube(pose,buffers,light,overlay,texture,x,y,z,16,Y,Z,color);pose.pushPose();pose.translate(1,0,0);cube(pose,buffers,light,overlay,texture,0,y,z,X-16,Y,Z,color);pose.popPose();return;}
        if(Y>16){cube(pose,buffers,light,overlay,texture,x,y,z,X,16,Z,color);pose.pushPose();pose.translate(0,1,0);cube(pose,buffers,light,overlay,texture,x,0,z,X,Y-16,Z,color);pose.popPose();return;}
        if(Z>16){cube(pose,buffers,light,overlay,texture,x,y,z,X,Y,16,color);pose.pushPose();pose.translate(0,0,1);cube(pose,buffers,light,overlay,texture,x,y,0,X,Y,Z-16,color);pose.popPose();return;}
        var key=new Box(getClass().getName()+":"+texture,x,y,z,X,Y,Z);
        var quads=CUBES.computeIfAbsent(key,box->{
            var id=textureId(texture);
            var sprite=Minecraft.getInstance().getTextureAtlas(net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS).apply(id);
            var faces=new EnumMap<Direction,BlockElementFace>(Direction.class); for (var side:Direction.values()) faces.put(side,new BlockElementFace(null,-1,"#tube",new BlockFaceUV(textureUv(texture),0)));
            var from=new Vector3f(x,y,z); var to=new Vector3f(X,Y,Z); new BlockElement(from,to,faces,null,true);
            var result=new ArrayList<BakedQuad>(); faces.forEach((side,face)->result.add(BAKERY.bakeQuad(from,to,face,sprite,side,net.minecraft.client.resources.model.BlockModelRotation.X0_Y0,null,true,id))); return result;
        });
        var out=buffers.getBuffer(RenderType.entityCutout(net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS));
        for (var quad:quads) out.putBulkData(pose.last(),quad,(color>>16&255)/255f,(color>>8&255)/255f,(color&255)/255f,light,overlay);
    }
    public static BlockEntityWithoutLevelRenderer itemRenderer() {
        if (itemRenderer==null) itemRenderer=new BlockEntityWithoutLevelRenderer(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels()) {
            final EssentiaTubeRenderer renderer=new EssentiaTubeRenderer(null);
            @Override public void renderByItem(net.minecraft.world.item.ItemStack stack,net.minecraft.world.item.ItemDisplayContext context,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
                if (stack.getItem() instanceof tc4revival.content.EssentiaTubeItem item) renderer.render((EssentiaTubeBlockEntity)((tc4revival.content.EssentiaTubeBlock)item.getBlock()).newBlockEntity(net.minecraft.core.BlockPos.ZERO,item.getBlock().defaultBlockState()),0,pose,buffers,light,overlay);
            }
        }; return itemRenderer;
    }
}
