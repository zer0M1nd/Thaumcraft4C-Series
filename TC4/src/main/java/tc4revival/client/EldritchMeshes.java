package tc4revival.client;

import java.util.HashMap;
import java.util.Map;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

/** Original OBJ data, read through the shared modern mesh parser and invalidated on resource reload. */
public  class EldritchMeshes {
    private static final Map<String,GroupedObjMesh> MODELS=new HashMap<>();
    private EldritchMeshes() {}
    public static void reload(ResourceManager resources) { MODELS.clear(); }
    public static void draw(String model,String group,PoseStack pose,MultiBufferSource buffers,RenderType type,int light,int overlay,float alpha) {
        draw(model,group,pose,buffers,type,light,overlay,1,alpha);
    }
    public static void draw(String model,String group,PoseStack pose,MultiBufferSource buffers,RenderType type,int light,int overlay,float brightness,float alpha) {
        drawTint(model,group,pose,buffers,type,light,overlay,brightness,brightness,brightness,alpha);
    }
    public static void drawTint(String model,String group,PoseStack pose,MultiBufferSource buffers,RenderType type,int light,int overlay,float red,float green,float blue,float alpha){
        GroupedObjMesh mesh=MODELS.computeIfAbsent(model,name->{
            try (var reader=new java.io.InputStreamReader(Minecraft.getInstance().getResourceManager()
                    .getResourceOrThrow(ResourceLocation.parse("thaumcraft:models/"+name+".obj")).open(),java.nio.charset.StandardCharsets.UTF_8)) {
                return GroupedObjMesh.read(reader);
            } catch (java.io.IOException failure) { throw new IllegalStateException("Cannot read eldritch model "+name,failure); }
        });
        var vertices=mesh.groups().get(group); if (vertices==null) throw new IllegalStateException("Missing eldritch mesh group "+group);
        var frame=pose.last(); var out=buffers.getBuffer(type);
        for (var vertex:vertices) out.vertex(frame.pose(),vertex.x(),vertex.y(),vertex.z()).color(red,green,blue,alpha).uv(vertex.u(),vertex.v())
                .overlayCoords(overlay).uv2(light).normal(frame.normal(),vertex.nx(),vertex.ny(),vertex.nz()).endVertex();
    }
}
