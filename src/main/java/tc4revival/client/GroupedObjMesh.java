package tc4revival.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Named triangle/quad OBJ groups, expanded to quads for the modern entity vertex format. */
public record GroupedObjMesh(Map<String, List<ScannerMesh.Vertex>> groups) {
    public GroupedObjMesh { var copy = new LinkedHashMap<String, List<ScannerMesh.Vertex>>(); groups.forEach((name, vertices) -> copy.put(name, List.copyOf(vertices))); groups = Map.copyOf(copy); }
    public static GroupedObjMesh read(Reader input) throws IOException {
        var positions = new ArrayList<float[]>(); var uvs = new ArrayList<float[]>(); var normals = new ArrayList<float[]>();
        var groups = new LinkedHashMap<String, List<ScannerMesh.Vertex>>(); String group = "default";
        try (var lines = new BufferedReader(input)) {
            String line;
            while ((line = lines.readLine()) != null) {
                String[] words = line.trim().split("\\s+");
                try {
                    switch (words[0]) {
                        case "v" -> positions.add(vector(words, 3));
                        case "vt" -> uvs.add(vector(words, 2));
                        case "vn" -> normals.add(vector(words, 3));
                        case "g" -> group = words[1];
                        case "f" -> {
                            if (words.length != 4 && words.length != 5) throw new IOException("Expected triangle or quad");
                            var vertices = groups.computeIfAbsent(group, key -> new ArrayList<>());
                            // OBJ permits v/vt and v//vn faces. TC6's original pillar
                            // omits normals; use the face normal without changing its UVs.
                            float[][] face = new float[3][];
                            for (int i = 0; i < 3; i++)
                                face[i] = positions.get(index(words[i + 1].split("/", -1)[0], positions.size()));
                            float[] normal = faceNormal(face[0], face[1], face[2]);
                            ScannerMesh.Vertex last = null;
                            for (int i = 1; i < words.length; i++) {
                                String[] indices = words[i].split("/", -1);
                                var p = positions.get(index(indices[0], positions.size()));
                                var uv = indices.length > 1 && !indices[1].isEmpty()
                                        ? uvs.get(index(indices[1], uvs.size())) : new float[]{0, 0};
                                var n = indices.length > 2 && !indices[2].isEmpty()
                                        ? normals.get(index(indices[2], normals.size())) : normal;
                                last = new ScannerMesh.Vertex(p[0], p[1], p[2], uv[0], 1 - uv[1], n[0], n[1], n[2]); vertices.add(last);
                            }
                            if (words.length == 4) vertices.add(last);
                        }
                        default -> {}
                    }
                } catch (RuntimeException malformed) { throw new IOException("Invalid OBJ line: " + line, malformed); }
            }
        }
        if (groups.isEmpty()) throw new IOException("OBJ has no faces");
        return new GroupedObjMesh(groups);
    }
    private static float[] faceNormal(float[] a, float[] b, float[] c) {
        float ux = b[0] - a[0], uy = b[1] - a[1], uz = b[2] - a[2];
        float vx = c[0] - a[0], vy = c[1] - a[1], vz = c[2] - a[2];
        float x = uy * vz - uz * vy, y = uz * vx - ux * vz, z = ux * vy - uy * vx;
        float length = (float) Math.sqrt(x * x + y * y + z * z);
        return length == 0 ? new float[]{0, 1, 0} : new float[]{x / length, y / length, z / length};
    }
    private static int index(String word, int count) { int value = Integer.parseInt(word); return value > 0 ? value - 1 : count + value; }
    private static float[] vector(String[] words, int count) throws IOException {
        float[] result = new float[count];
        for (int i = 0; i < count; i++) { result[i] = Float.parseFloat(words[i + 1]); if (!Float.isFinite(result[i])) throw new IOException("Nonfinite OBJ coordinate"); }
        return result;
    }
}
