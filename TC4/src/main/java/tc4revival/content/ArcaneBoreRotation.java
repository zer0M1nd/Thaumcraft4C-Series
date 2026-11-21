package tc4revival.content;

import net.minecraft.core.Direction;

/** Old accelerating turn and overshoot decay, shared by server work gating and client pose. */
public  class ArcaneBoreRotation {
    private int x,z,targetX,targetZ,speedX,speedZ;
    public int x() { return x; }
    public int z() { return z; }
    public int speedX() { return speedX; }
    public int speedZ() { return speedZ; }
    public boolean settled() { return x==targetX && z==targetZ; }
    public void face(Direction direction,boolean initial) {
        targetX=switch(direction) { case NORTH->270; case SOUTH->90; case EAST->180; default->0; };
        targetZ=switch(direction) { case DOWN->180; case UP->0; default->90; };
        if (initial) { x=targetX; z=targetZ; }
    }
    public void tick() {
        if (x<targetX) { x+=speedX; speedX=x<targetX?speedX+1:(int)(speedX/3.0f); }
        else if (x>targetX) { x+=speedX; speedX=x>targetX?speedX-1:(int)(speedX/3.0f); }
        else speedX=0;
        if (z<targetZ) { z+=speedZ; speedZ=z<targetZ?speedZ+1:(int)(speedZ/3.0f); }
        else if (z>targetZ) { z+=speedZ; speedZ=z>targetZ?speedZ-1:(int)(speedZ/3.0f); }
        else speedZ=0;
    }
}
