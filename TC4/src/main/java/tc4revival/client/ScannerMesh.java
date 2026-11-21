package tc4revival.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

/** Reads the retained scanner's quad OBJ asset; no Java from the old OBJ renderer is reused. */
public record ScannerMesh(List<Vertex> vertices) {
    public record Vertex(float x, float y, float z, float u, float v, float nx, float ny, float nz) {}
    public ScannerMesh { vertices = List.copyOf(vertices); }
    public static ScannerMesh read(Reader input) throws IOException {
        var positions = new ArrayList<float[]>();
        var uvs = new ArrayList<float[]>();
        var normals = new ArrayList<float[]>();
        var vertices = new ArrayList<Vertex>();
        try (var lines = new BufferedReader(input)) {
            String line;
            while ((line = lines.readLine()) != null) {
                String[] parts = line.trim().split("\\s+");
                try {
                    switch (parts[0]) {
                        case "v" -> positions.add(vector(parts, 3));
                        case "vt" -> uvs.add(vector(parts, 2));
                        case "vn" -> normals.add(vector(parts, 3));
                        case "f" -> {
                            if (parts.length != 5) throw new IOException("Scanner faces must have four corners");
                            for (int i = 1; i < parts.length; i++) {
                                var indices = parts[i].split("/");
                                if (indices.length != 3) throw new IOException("Scanner requires positions, UVs and normals");
                                var p = positions.get(Integer.parseInt(indices[0]) - 1);
                                var uv = uvs.get(Integer.parseInt(indices[1]) - 1);
                                var n = normals.get(Integer.parseInt(indices[2]) - 1);
                                vertices.add(new Vertex(p[0], p[1], p[2], uv[0], 1 - uv[1], n[0], n[1], n[2]));
                            }
                        }
                        default -> { /* Comments, object names and smoothing declarations carry no geometry. */ }
                    }
                } catch (RuntimeException invalid) { throw new IOException("Invalid scanner OBJ line: " + line, invalid); }
            }
        }
        if (vertices.isEmpty()) throw new IOException("Scanner mesh has no geometry");
        return new ScannerMesh(vertices);
    }
    private static float[] vector(String[] parts, int count) throws IOException {
        var result = new float[count];
        for (int i = 0; i < count; i++) {
            result[i] = Float.parseFloat(parts[i + 1]);
            if (!Float.isFinite(result[i])) throw new IOException("Nonfinite scanner coordinate");
        }
        return result;
    }
}
