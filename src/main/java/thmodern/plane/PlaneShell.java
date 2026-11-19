package thmodern.plane;
import java.util.*;
/** Legacy midpoint sphere with duplicate coordinates removed, in deterministic drawing order. */
public final class PlaneShell {
 public record Point(int x,int y,int z){}
 private PlaneShell(){}
 public static List<Point> points(int radius){
  if(radius<1||radius>127)throw new IllegalArgumentException("Invalid plane shell radius");var points=new LinkedHashSet<Point>();
  int x=radius,y=0,error=1-x;while(x>=y){int xx=x,z=0,inner=error;while(xx>=z){
   for(int sx:new int[]{-1,1})for(int sy:new int[]{-1,1})for(int sz:new int[]{-1,1}){
    points.add(new Point(sx*xx,sy*y,sz*z));points.add(new Point(sx*z,sy*y,sz*xx));
    points.add(new Point(sx*y,sy*xx,sz*z));points.add(new Point(sx*z,sy*xx,sz*y));
   }
   z++;if(inner<0)inner+=2*z+1;else{xx--;inner+=2*(z-xx+1);}
  }y++;if(error<0)error+=2*y+1;else{x--;error+=2*(y-x+1);}}
  return List.copyOf(points);
 }
}
