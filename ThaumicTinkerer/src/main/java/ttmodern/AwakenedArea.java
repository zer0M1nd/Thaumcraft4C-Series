package ttmodern;

import java.util.*;
import net.minecraft.core.*;

/** Pure selection rules, also used by the server and its tests. */
public final class AwakenedArea {
    public static List<BlockPos> select(String kind,int mode,BlockPos origin,Direction face){
        var result=new ArrayList<BlockPos>();
        if(mode==1){
            for(int x=face.getAxis()==Direction.Axis.X?0:-2;x< (face.getAxis()==Direction.Axis.X?1:3);x++)
            for(int y=face.getAxis()==Direction.Axis.Y?0:-1;y< (face.getAxis()==Direction.Axis.Y?1:4);y++)
            for(int z=face.getAxis()==Direction.Axis.Z?0:-2;z< (face.getAxis()==Direction.Axis.Z?1:3);z++)result.add(origin.offset(x,y,z));
        }else if(mode==2&&kind.equals("pick")){
            for(int i=0;i<10;i++)result.add(origin.relative(face.getOpposite(),i));
        }else if(mode==2&&kind.equals("shovel")){
            for(int y=-8;y<8;y++)result.add(origin.offset(0,y,0));
        }
        result.remove(origin);return List.copyOf(result);
    }
    private AwakenedArea(){}
}
