package wgmodern;

/** Exact enabled original animation; its commented-out portal loop is deliberately absent. */
public class WallMirrorRules {
    public record Step(int frame,boolean active,boolean activating,boolean deactivating){}
    public static Step advance(int frame,boolean active,boolean activating,boolean deactivating){
        if(activating){if(frame<31)frame++;else{active=true;activating=false;}}
        if(deactivating){if(frame>0){if(frame<17)frame--;frame--;}else{active=false;deactivating=false;}}
        return new Step(frame,active,activating,deactivating);
    }
    public static float reflectionScale(double distance){return .8125f*(float)(9-distance)/9f;}
    /** Includes the yOffset subtraction performed by the old RenderPlayer, before model rendering. */
    public static float reflectionOriginY(double feetY,double mirrorY,boolean localPlayer,boolean crouching){
        return (float)(feetY-mirrorY)-(localPlayer?1.62f/2:.75f)-(crouching&&!localPlayer?.125f:0);
    }
}
