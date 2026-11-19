package tc4revival.content;

import net.minecraft.core.Direction;

/** Connected-texture selection expressed in structure coordinates instead of a client-only world lookup. */
public  class InfernalFurnaceTextures {
    private InfernalFurnaceTextures() {}
    public static int texture(int part,int layer,Direction outlet,Direction face) {
        int band=(2-layer)*9;
        int add=touchesOutlet(part,layer,outlet,face) ? 3 : 0;
        if (face.getAxis()==Direction.Axis.Y) {
            if (face==Direction.UP && band==18) {
                int special=switch(part) { case 2->16; case 4->17; case 6->26; case 8->25; default->-1; };
                if (special>=0) return special;
            }
            if (add==0) return part==5 ? 10 : (part-1)%3+(part-1)/3*9;
            return 6;
        }
        int column=switch(face) {
            case NORTH->switch(part) { case 1->2; case 2->1; case 3->0; default->-1; };
            case SOUTH->switch(part) { case 7->0; case 8->1; case 9->2; default->-1; };
            case WEST->switch(part) { case 1->0; case 4->1; case 7->2; default->-1; };
            case EAST->switch(part) { case 3->2; case 6->1; case 9->0; default->-1; };
            default->-1;
        };
        return column<0 ? (band==9 ? 6 : 7) : column+band+add;
    }
    private static boolean touchesOutlet(int part,int layer,Direction outlet,Direction face) {
        int dx=1+outlet.getStepX()-(part-1)%3,dz=1+outlet.getStepZ()-(part-1)/3,dy=1-layer;
        if (face==Direction.DOWN || face==Direction.UP) return dx==0 && dz==0 && dy==face.getStepY();
        if (dx==0 && dy==0 && dz==0) return false;
        return Math.abs(dy)<=1 && (face.getAxis()==Direction.Axis.X ? dx==0 && Math.abs(dz)<=1 : dz==0 && Math.abs(dx)<=1);
    }
}
