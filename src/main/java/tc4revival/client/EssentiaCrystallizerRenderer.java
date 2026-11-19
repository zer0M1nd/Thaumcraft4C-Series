package tc4revival.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import tc4revival.content.EssentiaCrystallizerBlockEntity;

public  class EssentiaCrystallizerRenderer implements BlockEntityRenderer<EssentiaCrystallizerBlockEntity> {
    private static GroupedObjMesh machine,crystals;
    private static final ResourceLocation TEXTURE=ResourceLocation.parse("thaumcraft:textures/models/crystalizer.png"), CRYSTAL_TEXTURE=ResourceLocation.parse("thaumcraft:textures/models/vis_relay.png");
    private static BlockEntityWithoutLevelRenderer item;
    public EssentiaCrystallizerRenderer(BlockEntityRendererProvider.Context context){}
    public static void reload(net.minecraft.server.packs.resources.ResourceManager resources){machine=null;crystals=null;}
    private static GroupedObjMesh load(String name){try(var stream=Minecraft.getInstance().getResourceManager().getResourceOrThrow(ResourceLocation.parse("thaumcraft:models/"+name+".obj")).open()){return GroupedObjMesh.read(new java.io.InputStreamReader(stream,java.nio.charset.StandardCharsets.UTF_8));}catch(java.io.IOException error){throw new IllegalStateException("Cannot load crystallizer mesh",error);}}
    @Override public void render(EssentiaCrystallizerBlockEntity tile,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        var viewer=Minecraft.getInstance().getCameraEntity();float ticks=(viewer==null?0:viewer.tickCount)+partial;
        renderModel(tile.facing(),tile.spin+tile.spinIncrement*partial,ticks,tile.red,tile.green,tile.blue,pose,buffers,light,overlay);
    }
    private static void renderModel(Direction facing,float spin,float ticks,float red,float green,float blue,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        if(machine==null)machine=load("crystalizer");if(crystals==null)crystals=load("vis_relay");
        pose.pushPose();pose.translate(.5,.5,.5);
        switch(facing){case DOWN->pose.mulPose(Axis.XP.rotationDegrees(-90));case UP->pose.mulPose(Axis.XP.rotationDegrees(90));case SOUTH->pose.mulPose(Axis.YP.rotationDegrees(180));case WEST->pose.mulPose(Axis.YP.rotationDegrees(90));case EAST->pose.mulPose(Axis.YP.rotationDegrees(-90));default->{}}
        pose.translate(0,0,-.5);
        for(var vertices:machine.groups().values())draw(vertices,TEXTURE,pose,buffers,light,overlay,1,1,1);
        for(int q=0;q<4;q++){
            pose.pushPose();pose.scale(.75f,.75f,.75f);pose.mulPose(Axis.ZP.rotationDegrees(90*q));pose.translate(.34,0,1.2125);pose.mulPose(Axis.ZP.rotationDegrees(spin));
            int glow=50+(int)(150*(Math.sin((ticks+q*10)/2)*.05+.95));draw(crystals.groups().get("Crystal"),CRYSTAL_TEXTURE,pose,buffers,glow,overlay,red,green,blue);pose.popPose();
        }pose.popPose();
    }
    private static void draw(java.util.List<ScannerMesh.Vertex> vertices,ResourceLocation texture,PoseStack pose,MultiBufferSource buffers,int light,int overlay,float red,float green,float blue){
        if(vertices==null)return;var out=buffers.getBuffer(RenderType.entityTranslucent(texture));var frame=pose.last();
        for(var v:vertices)out.vertex(frame.pose(),v.x(),v.y(),v.z()).color(Math.min(1,red),Math.min(1,green),Math.min(1,blue),1).uv(v.u(),v.v()).overlayCoords(overlay).uv2(light).normal(frame.normal(),v.nx(),v.ny(),v.nz()).endVertex();
    }
    public static void vent(EssentiaCrystallizerBlockEntity tile){
        var level=tile.getLevel();var random=level.random;var pos=tile.getBlockPos();var face=tile.facing().getOpposite();
        EssentiaVentParticles.emit(level,pos.getX()+.5+.1-random.nextFloat()*.2+face.getStepX()/2.1,pos.getY()+.5+.1-random.nextFloat()*.2+face.getStepY()/2.1,pos.getZ()+.5+.1-random.nextFloat()*.2+face.getStepZ()/2.1,
            face.getStepX()/4.0+.1-random.nextFloat()*.2,face.getStepY()/4.0+.1-random.nextFloat()*.2,face.getStepZ()/4.0+.1-random.nextFloat()*.2,0xffffff);
    }
    public static BlockEntityWithoutLevelRenderer itemRenderer(){
        if(item==null)item=new BlockEntityWithoutLevelRenderer(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels()){
            @Override public void renderByItem(net.minecraft.world.item.ItemStack stack,net.minecraft.world.item.ItemDisplayContext context,PoseStack pose,MultiBufferSource buffers,int light,int overlay){renderModel(Direction.DOWN,0,0,1,1,1,pose,buffers,light,overlay);}
        };return item;
    }
}
