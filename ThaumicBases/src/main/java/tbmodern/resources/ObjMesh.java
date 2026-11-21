package tbmodern.resources;

import java.io.*;
import java.util.*;

/** Bounded OBJ reader. Geometry comes from the player's resource pack, never the distribution. */
public record ObjMesh(Map<String,List<Triangle>> groups) {
    public record Point(float x,float y,float z) {}
    public record UV(float u,float v) {}
    public record Vertex(Point position, UV uv, Point normal) {}
    public record Triangle(Vertex a,Vertex b,Vertex c) {}
    public ObjMesh {
        Map<String,List<Triangle>> copy=new LinkedHashMap<>();groups.forEach((name,faces)->copy.put(name,List.copyOf(faces)));groups=Collections.unmodifiableMap(copy);
    }
    public static ObjMesh read(Reader reader) throws IOException {
        var input=new BufferedReader(reader);List<Point> positions=new ArrayList<>(),normals=new ArrayList<>();List<UV> uvs=new ArrayList<>();
        Map<String,List<Triangle>> groups=new LinkedHashMap<>();String group="default",line;int count=0,faces=0;
        try {
            while((line=input.readLine())!=null) {
                if(++count>200000 || line.length()>16384)throw new IOException("OBJ exceeds geometry limits");
                int comment=line.indexOf('#');if(comment>=0)line=line.substring(0,comment);line=line.trim();if(line.isEmpty())continue;
                String[] parts=line.split("\\s+");
                switch(parts[0]) {
                    case "v" -> positions.add(point(parts));
                    case "vn" -> normals.add(point(parts));
                    case "vt" -> uvs.add(new UV(number(parts[1]),1-number(parts[2])));
                    case "o","g" -> {if(parts.length>1)group=parts[1];}
                    case "f" -> {
                        if(parts.length<4 || parts.length>65)throw new IOException("Invalid OBJ face");
                        var vertices=new ArrayList<Vertex>();
                        for(int i=1;i<parts.length;i++) {
                            String[] fields=parts[i].split("/",-1);
                            Point position=positions.get(index(fields[0],positions.size()));
                            UV uv=fields.length>1 && !fields[1].isEmpty()?uvs.get(index(fields[1],uvs.size())):new UV(0,0);
                            Point normal=fields.length>2 && !fields[2].isEmpty()?normals.get(index(fields[2],normals.size())):null;
                            vertices.add(new Vertex(position,uv,normal));
                        }
                        var destination=groups.computeIfAbsent(group,key->new ArrayList<>());
                        for(int i=1;i<vertices.size()-1;i++) {
                            if(++faces>200000)throw new IOException("Too many OBJ triangles");
                            Vertex a=vertices.get(0),b=vertices.get(i),c=vertices.get(i+1);Point normal=normal(a.position,b.position,c.position);
                            destination.add(new Triangle(withNormal(a,normal),withNormal(b,normal),withNormal(c,normal)));
                        }
                    }
                    default -> {} // Materials are selected by the renderer; no external MTL paths are followed.
                }
            }
        } catch(RuntimeException invalid) {throw new IOException("Invalid OBJ at line "+count,invalid);}
        return new ObjMesh(groups);
    }
    private static Vertex withNormal(Vertex vertex,Point normal) {return vertex.normal==null?new Vertex(vertex.position,vertex.uv,normal):vertex;}
    private static int index(String token,int size) {int index=Integer.parseInt(token);if(index==0)throw new IllegalArgumentException("Zero OBJ index");return index<0?size+index:index-1;}
    private static Point point(String[] parts) {return new Point(number(parts[1]),number(parts[2]),number(parts[3]));}
    private static float number(String token) {float value=Float.parseFloat(token);if(!Float.isFinite(value)||Math.abs(value)>1000000)throw new IllegalArgumentException("Invalid coordinate");return value;}
    private static Point normal(Point a,Point b,Point c) {
        double x1=b.x-a.x,y1=b.y-a.y,z1=b.z-a.z,x2=c.x-a.x,y2=c.y-a.y,z2=c.z-a.z;
        double x=y1*z2-z1*y2,y=z1*x2-x1*z2,z=x1*y2-y1*x2,length=Math.sqrt(x*x+y*y+z*z);
        return length<1e-12?new Point(0,1,0):new Point((float)(x/length),(float)(y/length),(float)(z/length));
    }
}
