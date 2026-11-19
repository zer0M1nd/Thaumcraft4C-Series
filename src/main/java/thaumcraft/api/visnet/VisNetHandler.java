package thaumcraft.api.visnet;

public  class VisNetHandler {
    private VisNetHandler() {}
    public static void register(VisEndpoint endpoint){tc4revival.content.VisNetwork.register(endpoint);}
    public static void unregister(VisEndpoint endpoint){tc4revival.content.VisNetwork.unregister(endpoint);}
    public static void refresh(VisEndpoint endpoint){tc4revival.content.VisNetwork.refresh(endpoint);}
    public static VisEndpoint parent(VisEndpoint endpoint){return tc4revival.content.VisNetwork.parent(endpoint);}
    public static VisEndpoint root(VisEndpoint endpoint){return tc4revival.content.VisNetwork.root(endpoint);}
    public static int consume(VisEndpoint endpoint,thaumcraft.api.aspects.Aspect aspect,int amount){return tc4revival.content.VisNetwork.consume(endpoint,aspect.unwrap(),amount);}
    /** Amounts are hundredths of one vis point, matching the runtime network. */
    public static int drainVis(net.minecraft.world.level.Level level,net.minecraft.core.BlockPos pos,thaumcraft.api.aspects.Aspect aspect,int amount) { return tc4revival.content.VisNetwork.drain(level,pos,aspect.unwrap(),amount); }
}
