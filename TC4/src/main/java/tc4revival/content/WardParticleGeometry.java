package tc4revival.content;

import net.minecraft.core.Direction;
import org.joml.Matrix4f;

/** Exact legacy FXBlockWard face/rotation/offset order, independent of client rendering. */
public  class WardParticleGeometry {
    private WardParticleGeometry() {}
    public static Matrix4f transform(Direction face, float degrees) {
        var matrix=new Matrix4f().rotate((float)Math.PI/2,face.getStepY(),-face.getStepX(),face.getStepZ())
                .rotateZ((float)Math.toRadians(degrees)).translate(0,0,face==Direction.SOUTH?.505f:-.505f);
        if(face==Direction.SOUTH)matrix.rotateY(-(float)Math.PI);
        return matrix;
    }
}
