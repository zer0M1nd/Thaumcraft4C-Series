package conciliummodern;

import java.util.*;
import net.minecraft.world.phys.Vec3;

/** CPU port of the used GLE polycone ANGLE/PATH_EDGE path (six sides, no caps).
 * The first and last points supply endpoint tangents; they are not tube segments.
 */
final class RiftTube {
    record Segment(Vec3[] front,Vec3[] back){}
    private static final double EPS=2e-6;
    static List<Segment> build(List<Vec3> points,double[] radii){
        var result=new ArrayList<Segment>();int size=points.size();if(size<4)return result;
        int first=next(points,0);var initial=points.get(first).subtract(points.get(first-1));
        Vec3 up=initial.x==0&&initial.z==0?new Vec3(1,1,1):new Vec3(0,1,0);
        Vec3 direction=points.get(1).subtract(points.get(0));
        for(int i=1;direction.lengthSqr()==0&&i<size-2;i++)direction=points.get(i+1).subtract(points.get(i));
        if(direction.lengthSqr()==0)return result;direction=unit(direction);up=perpendicular(up,direction);
        if(up.lengthSqr()==0)up=direction;
        int i=1,end=next(points,1);double segmentLength=points.get(end).subtract(points.get(end-1)).length();Vec3 frontPlane=bisector(points.get(0),points.get(1),points.get(end));up=reflect(up,frontPlane);
        while(end<size-1){
            int after=next(points,end);Vec3 start=points.get(i),finish=points.get(end),delta=finish.subtract(start);
            double length=delta.length();if(length==0)break;
            Vec3 backPlane=bisector(start,finish,points.get(after));
            Vec3 z=delta.scale(-1/length),y=unit(perpendicular(up,z));
            if(y.lengthSqr()==0)y=unit(perpendicular(Math.abs(z.y)<.9?new Vec3(0,1,0):new Vec3(1,0,0),z));
            Vec3 x=unit(y.cross(z));
            // GLE retains the last nondegenerate pair's length when it skips repeated points.
            delta=z.scale(-segmentLength);Vec3 backCenter=start.add(delta);
            Vec3[] front=new Vec3[6],back=new Vec3[6];
            double cx=1,cy=0,cos=Math.cos(Math.PI/3),sin=Math.sin(Math.PI/3);
            for(int side=0;side<6;side++){
                Vec3 offset0=x.scale(cx*radii[end-1]).add(y.scale(cy*radii[end-1]));
                Vec3 offset1=x.scale(cx*radii[end]).add(y.scale(cy*radii[end]));
                front[side]=intersect(start,start.add(offset0),delta,frontPlane,start);
                back[side]=intersect(backCenter,start.add(offset1),delta,backPlane,start);
                double nx=cx*cos-cy*sin;cy=cx*sin+cy*cos;cx=nx;
            }
            result.add(new Segment(front,back));segmentLength=points.get(after).subtract(points.get(after-1)).length();i=end;end=after;frontPlane=backPlane;up=reflect(up,frontPlane);
        }return result;
    }
    private static Vec3 intersect(Vec3 plane,Vec3 line,Vec3 delta,Vec3 normal,Vec3 origin){double denominator=delta.dot(normal);return denominator==0?origin:line.add(delta.scale(plane.subtract(line).dot(normal)/denominator));}
    private static int next(List<Vec3> p,int index){int i=index;do{i++;}while(i<p.size()-1&&p.get(i).subtract(p.get(i-1)).length()<=p.get(i).add(p.get(i-1)).length()*EPS);return i;}
    private static Vec3 perpendicular(Vec3 vector,Vec3 normal){return vector.subtract(normal.scale(vector.dot(normal)));}
    private static Vec3 unit(Vec3 vector){double length=vector.length();return length==0?Vec3.ZERO:vector.scale(1/length);}
    private static Vec3 reflect(Vec3 vector,Vec3 normal){return vector.subtract(normal.scale(2*vector.dot(normal)));}
    private static Vec3 bisector(Vec3 a,Vec3 b,Vec3 c){
        Vec3 before=b.subtract(a),after=c.subtract(b);double l0=before.length(),l1=after.length();
        if(l0<=EPS*l1)return l1==0?Vec3.ZERO:after.scale(1/l1);
        if(l1<=EPS*l0)return before.scale(1/l0);
        before=before.scale(1/l0);after=after.scale(1/l1);double dot=after.dot(before);
        return dot>=.999998||dot<=-.999998?before:unit(after.add(before).scale(dot-1));
    }
}
