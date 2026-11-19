package thmodern.client;

import java.io.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.resources.ResourceLocation;
import com.mojang.blaze3d.vertex.PoseStack;

/** Geometry comes from the player's resource pack stack, never from an embedded legacy asset. */
public final class ExternalMesh {
    private record Vertex(float x,float y,float z,float u,float v,float nx,float ny,float nz){}
    private static final Map<ResourceLocation,List<Vertex>> CACHE=new HashMap<>();
    public static void clear(){CACHE.clear();AmplifierRenderer.reload();}
    public static void render(ResourceLocation model,ResourceLocation texture,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        var vertices=CACHE.computeIfAbsent(model,ExternalMesh::read);
        var consumer=buffers.getBuffer(RenderType.entityTranslucent(texture));
        for(var v:vertices)consumer.vertex(pose.last().pose(),v.x,v.y,v.z).color(255,255,255,255).uv(v.u,v.v)
                .overlayCoords(overlay).uv2(light).normal(pose.last().normal(),v.nx,v.ny,v.nz).endVertex();
    }
    private static List<Vertex> read(ResourceLocation id){
        var positions=new ArrayList<float[]>();var uv=new ArrayList<float[]>();var normals=new ArrayList<float[]>();var result=new ArrayList<Vertex>();
        try(var reader=Minecraft.getInstance().getResourceManager().getResourceOrThrow(id).openAsReader()){
            String line;while((line=reader.readLine())!=null){
                var p=line.trim().split("\\s+");
                if(p[0].equals("v"))positions.add(vector(p,3));
                else if(p[0].equals("vt"))uv.add(vector(p,2));
                else if(p[0].equals("vn"))normals.add(vector(p,3));
                else if(p[0].equals("f")){
                    var face=new ArrayList<Vertex>();
                    for(int i=1;i<p.length;i++){
                        var indices=p[i].split("/");var a=positions.get(index(indices[0],positions.size()));
                        var b=indices.length>1&&!indices[1].isEmpty()?uv.get(index(indices[1],uv.size())):new float[]{0,0};
                        var c=indices.length>2?normals.get(index(indices[2],normals.size())):new float[]{0,1,0};
                        face.add(new Vertex(a[0],a[1],a[2],b[0],1-b[1],c[0],c[1],c[2]));
                    }
                    if(face.size()==4)result.addAll(face);
                    else for(int i=1;i+1<face.size();i++)result.addAll(List.of(face.get(0),face.get(i),face.get(i+1),face.get(i+1)));
                }
            }
        }catch(IOException|RuntimeException failure){throw new IllegalStateException("Cannot read external Horizons mesh "+id,failure);}
        return List.copyOf(result);
    }
    private static int index(String value,int size){int i=Integer.parseInt(value);return i<0?size+i:i-1;}
    private static float[] vector(String[] input,int count){var result=new float[count];for(int i=0;i<count;i++){result[i]=Float.parseFloat(input[i+1]);if(!Float.isFinite(result[i]))throw new IllegalArgumentException("Nonfinite vertex");}return result;}
    private ExternalMesh(){}
}
