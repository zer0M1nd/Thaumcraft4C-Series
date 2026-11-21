package advancedthaumaturgy.client;
import java.util.*;
import java.io.*;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import com.mojang.blaze3d.vertex.*;
/** Reads the author's external OBJ, retaining group names and UV coordinates. */
public class OriginalObj {
    record Vertex(Vec3 pos,float u,float v){}
    record Triangle(Vertex a,Vertex b,Vertex c,Vec3 normal,String group){}
    private static final Map<ResourceLocation,OriginalObj> CACHE=new HashMap<>();
    private final List<Triangle> triangles=new ArrayList<>();
    public static void clear(){CACHE.clear();}
    public static OriginalObj get(ResourceLocation path){return CACHE.computeIfAbsent(path,OriginalObj::new);}
    private OriginalObj(ResourceLocation path){
        List<Vec3> vertices=new ArrayList<>();List<float[]> coords=new ArrayList<>();String group="";
        try(var reader=new BufferedReader(new InputStreamReader(Minecraft.getInstance().getResourceManager().open(path),java.nio.charset.StandardCharsets.UTF_8))){
            for(String line;(line=reader.readLine())!=null;){String[] words=line.trim().split("\\s+");if(words.length<2)continue;
                switch(words[0]){
                    case "v"->vertices.add(new Vec3(Double.parseDouble(words[1]),Double.parseDouble(words[2]),Double.parseDouble(words[3])));
                    case "vt"->coords.add(new float[]{Float.parseFloat(words[1]),1-Float.parseFloat(words[2])});
                    case "g","o"->group=words[1];
                    case "f"->{var a=vertex(words[1],vertices,coords);for(int i=2;i<words.length-1;i++){var b=vertex(words[i],vertices,coords);var c=vertex(words[i+1],vertices,coords);triangles.add(new Triangle(a,b,c,b.pos.subtract(a.pos).cross(c.pos.subtract(a.pos)).normalize(),group));}}
                }
            }
        }catch(IOException|RuntimeException e){org.slf4j.LoggerFactory.getLogger("AdvancedThaumaturgy").error("Cannot load external model {}",path,e);}
    }
    private static int index(String s,int count){int n=Integer.parseInt(s);return n>0?n-1:count+n;}
    private static Vertex vertex(String s,List<Vec3> vertices,List<float[]> coords){var parts=s.split("/");var uv=parts.length>1&&!parts[1].isEmpty()?coords.get(index(parts[1],coords.size())):new float[]{0,0};return new Vertex(vertices.get(index(parts[0],vertices.size())),uv[0],uv[1]);}
    public void draw(PoseStack pose,VertexConsumer buffer,int light,int overlay,int rgb,java.util.function.Predicate<String> group){for(var t:triangles)if(group.test(t.group)){emit(t.a,t.normal,pose,buffer,light,overlay,rgb);emit(t.b,t.normal,pose,buffer,light,overlay,rgb);emit(t.c,t.normal,pose,buffer,light,overlay,rgb);emit(t.c,t.normal,pose,buffer,light,overlay,rgb);}}
    private static void emit(Vertex v,Vec3 n,PoseStack p,VertexConsumer b,int light,int overlay,int rgb){b.vertex(p.last().pose(),(float)v.pos.x,(float)v.pos.y,(float)v.pos.z).color(rgb>>16&255,rgb>>8&255,rgb&255,255).uv(v.u,v.v).overlayCoords(overlay).uv2(light).normal(p.last().normal(),(float)n.x,(float)n.y,(float)n.z).endVertex();}
}
