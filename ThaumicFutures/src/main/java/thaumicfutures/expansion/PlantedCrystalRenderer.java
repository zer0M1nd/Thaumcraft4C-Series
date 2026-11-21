package thaumicfutures.expansion;

import java.util.*;
import java.io.*;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import tc4revival.client.ScannerMesh;

/** The original eight-part TC6 OBJ, selecting size + 1 crystals independently for each support face. */
public final class PlantedCrystalRenderer implements BlockEntityRenderer<PlantedCrystal.Tile> {
    private static final ResourceLocation MODEL=ResourceLocation.parse("thaumicfutures:models/restored/crystal.obj");
    private static final ResourceLocation TEXTURE=ResourceLocation.parse("thaumicfutures:textures/block/tc6/crystal.png");
    private static List<List<ScannerMesh.Vertex>> groups;
    public PlantedCrystalRenderer(BlockEntityRendererProvider.Context context){}
    public static void reload(){groups=null;}
    private static void load(){
        var vertices=new ArrayList<float[]>();var uvs=new ArrayList<float[]>();
        var output=new ArrayList<List<ScannerMesh.Vertex>>();List<ScannerMesh.Vertex> group=new ArrayList<>();
        try(var reader=new BufferedReader(new InputStreamReader(Minecraft.getInstance().getResourceManager().getResourceOrThrow(MODEL).open(),java.nio.charset.StandardCharsets.UTF_8))){
            String line;
            while((line=reader.readLine())!=null){
                var words=line.trim().split("\\s+");if(words.length<2)continue;
                switch(words[0]){
                    case "v"->vertices.add(new float[]{Float.parseFloat(words[1]),Float.parseFloat(words[2]),Float.parseFloat(words[3])});
                    case "vt"->uvs.add(new float[]{Float.parseFloat(words[1]),1-Float.parseFloat(words[2])});
                    case "g"->{group=new ArrayList<>();output.add(group);}
                    case "f"->{
                        var p=new float[3][];var uv=new float[3][];
                        for(int i=0;i<3;i++){var ids=words[i+1].split("/");p[i]=vertices.get(Integer.parseInt(ids[0])-1);uv[i]=uvs.get(Integer.parseInt(ids[1])-1);}
                        var normal=new org.joml.Vector3f(p[1][0]-p[0][0],p[1][1]-p[0][1],p[1][2]-p[0][2]);
                        normal.cross(p[2][0]-p[0][0],p[2][1]-p[0][1],p[2][2]-p[0][2]).normalize();
                        for(int i:new int[]{0,1,2,2})group.add(new ScannerMesh.Vertex(p[i][0],p[i][1],p[i][2],uv[i][0],uv[i][1],normal.x,normal.y,normal.z));
                    }
                    default->{}
                }
            }
            groups=output.stream().map(List::copyOf).toList();
        }catch(IOException e){throw new IllegalStateException("Missing original crystal model",e);}
    }
    @Override public void render(PlantedCrystal.Tile tile,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        if(groups==null)load();
        var state=tile.getBlockState();if(!(state.getBlock() instanceof PlantedCrystal crystal)||tile.getLevel()==null)return;
        var out=buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));int color=crystal.shard.tint();
        for(var side:Direction.values()){
            if(!crystal.supportedAt(tile.getLevel(),tile.getBlockPos(),side))continue;
            var order=new ArrayList<Integer>();for(int i=0;i<groups.size();i++)order.add(i);
            Collections.shuffle(order,new Random(tile.getBlockPos().asLong()+side.ordinal()*5L));
            pose.pushPose();pose.translate(.5,.5,.5);pose.mulPose(side.getOpposite().getRotation());pose.translate(-.5,-.5,-.5);
            var frame=pose.last();
            for(int i=0;i<Math.min(state.getValue(PlantedCrystal.SIZE)+1,order.size());i++)for(var v:groups.get(order.get(i)))
                out.vertex(frame.pose(),v.x(),v.y(),v.z()).color(color>>16&255,color>>8&255,color&255,255).uv(v.u(),v.v())
                        .overlayCoords(overlay).uv2(light).normal(frame.normal(),v.nx(),v.ny(),v.nz()).endVertex();
            pose.popPose();
        }
    }
}
