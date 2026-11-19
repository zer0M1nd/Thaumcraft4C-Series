package tbmodern.resources;

/** Fits external OBJ geometry after the GUI rotation, retaining its proportions. */
public class GuiModelFit {
    public static final float X_ROTATION = 15, Y_ROTATION = 135;
    public final float x, y, z, scale;

    private GuiModelFit(float x, float y, float z, float scale) {
        this.x=x; this.y=y; this.z=z; this.scale=scale;
    }

    public static ObjMesh.Point project(ObjMesh.Point point) {
        double a=Math.toRadians(Y_ROTATION), b=Math.toRadians(X_ROTATION);
        double x=point.x()*Math.cos(a)+point.z()*Math.sin(a);
        double z=point.z()*Math.cos(a)-point.x()*Math.sin(a);
        return new ObjMesh.Point((float)x, (float)(point.y()*Math.cos(b)-z*Math.sin(b)),
                (float)(point.y()*Math.sin(b)+z*Math.cos(b)));
    }

    public static GuiModelFit of(ObjMesh mesh) {
        float minX=Float.POSITIVE_INFINITY, minY=minX, minZ=minX;
        float maxX=Float.NEGATIVE_INFINITY, maxY=maxX, maxZ=maxX;
        for(var group:mesh.groups().values()) for(var face:group)
            for(var vertex:java.util.List.of(face.a(),face.b(),face.c())) {
                var p=project(vertex.position());
                minX=Math.min(minX,p.x()); maxX=Math.max(maxX,p.x());
                minY=Math.min(minY,p.y()); maxY=Math.max(maxY,p.y());
                minZ=Math.min(minZ,p.z()); maxZ=Math.max(maxZ,p.z());
            }
        if(!Float.isFinite(minX))return new GuiModelFit(0,0,0,1);
        return new GuiModelFit((minX+maxX)/2,(minY+maxY)/2,(minZ+maxZ)/2,
                .84F/Math.max(.001F,Math.max(maxX-minX,maxY-minY)));
    }
}
