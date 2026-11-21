package automagy.client;

import com.mojang.blaze3d.vertex.*;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.*;
import org.joml.Vector3f;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Read-only Wavefront geometry from the selected resource pack; no old classes or GL display lists. */
public final class ExternalObj implements ResourceManagerReloadListener {
    public static final ExternalObj HOURGLASS = new ExternalObj(new ResourceLocation("automagy", "models/external/hourglass.obj"));
    public static final ExternalObj REDCRYSTAL = new ExternalObj(new ResourceLocation("automagy", "models/external/redcrystal.obj"));
    public static final ExternalObj MERCCRYSTAL=new ExternalObj(ResourceLocation.parse("automagy:models/external/merccrystal.obj"));
    public static final ExternalObj COMPARATOR = new ExternalObj(new ResourceLocation("automagy", "models/external/comparator.obj"));
    public static final ExternalObj BOILER=new ExternalObj(ResourceLocation.parse("automagy:models/external/boiler.obj"));
    private final ResourceLocation location;
    private Map<String, List<Vertex[]>> groups = Map.of();
    private record Vertex(float x, float y, float z, float u, float v, float nx, float ny, float nz) {}
    private ExternalObj(ResourceLocation location) { this.location = location; }
    @Override public void onResourceManagerReload(ResourceManager manager) {
        groups = Map.of();
        try (var in = manager.open(location)) {
            byte[] bytes = in.readNBytes(8 * 1024 * 1024 + 1);
            if (bytes.length > 8 * 1024 * 1024) throw new IOException("OBJ exceeds size limit");
            groups = parse(new String(bytes, StandardCharsets.UTF_8));
        } catch (IOException | IllegalArgumentException | IndexOutOfBoundsException error) {
            LogUtils.getLogger().warn("Could not load Automagy model {}: {}", location, error.toString());
        }
    }
    private static Map<String, List<Vertex[]>> parse(String text) {
        var positions = new ArrayList<Vector3f>();
        var normals = new ArrayList<Vector3f>();
        var uv = new ArrayList<float[]>();
        var result = new LinkedHashMap<String, List<Vertex[]>>();
        List<Vertex[]> current = new ArrayList<>(); result.put("default", current);
        for (String line : text.split("\\R")) {
            line = line.trim(); if (line.isEmpty() || line.startsWith("#")) continue;
            String[] s = line.split("\\s+");
            switch (s[0]) {
                case "v" -> positions.add(new Vector3f(number(s[1]), number(s[2]), number(s[3])));
                case "vn" -> normals.add(new Vector3f(number(s[1]), number(s[2]), number(s[3])));
                case "vt" -> uv.add(new float[]{number(s[1]), 1 - number(s[2])});
                case "g", "o" -> current = result.computeIfAbsent(s[1], key -> new ArrayList<>());
                case "f" -> {
                    if (s.length < 4 || s.length > 65) throw new IllegalArgumentException("Invalid polygon");
                    Vertex[] polygon = new Vertex[s.length - 1];
                    for (int i = 1; i < s.length; i++) {
                        String[] indices = s[i].split("/", -1);
                        Vector3f p = positions.get(index(indices[0], positions.size()));
                        float[] t = indices.length > 1 && !indices[1].isEmpty() ? uv.get(index(indices[1], uv.size())) : new float[]{0, 0};
                        Vector3f n = indices.length > 2 && !indices[2].isEmpty() ? normals.get(index(indices[2], normals.size())) : new Vector3f();
                        polygon[i-1] = new Vertex(p.x, p.y, p.z, t[0], t[1], n.x, n.y, n.z);
                    }
                    for (int i=1; i<polygon.length-1; i++) current.add(new Vertex[]{polygon[0], polygon[i], polygon[i+1], polygon[i+1]});
                }
                default -> { /* MTL paths and commands are not loaded or executed. */ }
            }
        }
        return result;
    }
    private static float number(String value) {
        float result = Float.parseFloat(value);
        if (!Float.isFinite(result) || Math.abs(result) > 10000) throw new IllegalArgumentException("Invalid coordinate");
        return result;
    }
    private static int index(String value, int size) { int i = Integer.parseInt(value); return i < 0 ? size + i : i - 1; }
    public void draw(PoseStack pose, VertexConsumer output, int light, int overlay, float shade, float alpha, String... names) {
        var transform = pose.last();
        for (String name : names) for (Vertex[] face : groups.getOrDefault(name, List.of())) {
            Vector3f fallback = new Vector3f(face[1].x-face[0].x, face[1].y-face[0].y, face[1].z-face[0].z)
                    .cross(new Vector3f(face[2].x-face[0].x, face[2].y-face[0].y, face[2].z-face[0].z));
            if (fallback.lengthSquared() > 0) fallback.normalize(); else fallback.set(0, 1, 0);
            for (Vertex v : face) {
                boolean missingNormal = v.nx == 0 && v.ny == 0 && v.nz == 0;
                output.vertex(transform.pose(), v.x, v.y, v.z).color(shade, shade, shade, alpha).uv(v.u, v.v)
                        .overlayCoords(overlay).uv2(light).normal(transform.normal(), missingNormal ? fallback.x : v.nx,
                                missingNormal ? fallback.y : v.ny, missingNormal ? fallback.z : v.nz).endVertex();
            }
        }
    }
    public void drawAll(PoseStack pose, VertexConsumer output, int light, int overlay) {
        draw(pose,output,light,overlay,1,1,groups.keySet().toArray(String[]::new));
    }
}
