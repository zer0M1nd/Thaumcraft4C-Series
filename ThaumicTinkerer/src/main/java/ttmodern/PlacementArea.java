package ttmodern;

import net.minecraft.core.*;

public final class PlacementArea {
    public static java.util.List<BlockPos> positions(BlockPos origin,Direction face,Direction facing,float pitch,int size){
        int range=(Math.max(3,Math.min(11,size|1))-1)/2;boolean horizontal=face.getAxis().isVertical();int x=face.getAxis()==Direction.Axis.X?0:horizontal&&pitch<=75&&facing.getAxis()==Direction.Axis.X?0:range;
        int y=horizontal&&pitch>75?0:range;int z=face.getAxis()==Direction.Axis.Z?0:horizontal&&pitch<=75&&facing.getAxis()==Direction.Axis.Z?0:range;
        var result=new java.util.ArrayList<BlockPos>();for(int dx=-x;dx<=x;dx++)for(int dy=0;dy<=y*2;dy++)for(int dz=-z;dz<=z;dz++)result.add(origin.offset(dx,dy,dz));return result;
    }
    private PlacementArea(){}
}
