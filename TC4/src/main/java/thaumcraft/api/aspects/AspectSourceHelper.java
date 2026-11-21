package thaumcraft.api.aspects;

public  class AspectSourceHelper {
    private AspectSourceHelper() {}
    public static tc4revival.content.EssentiaVessel phialVessel(net.minecraft.world.level.block.entity.BlockEntity entity,tc4revival.aspect.Aspect requested) {
        if(entity instanceof tc4revival.content.EssentiaVessel vessel)return vessel;
        if(!(entity instanceof IAspectContainer container))return null;
        var selected=requested!=null?requested:container.getAspects().unwrap().aspects().stream()
                .filter(aspect->container.contains(aspect,8)).findFirst().orElse(null);
        if(selected==null)return null;
        return new tc4revival.content.EssentiaVessel() {
            public tc4revival.aspect.Aspect aspect(){return selected;}
            public int amount(){return container.getAspects().unwrap().amount(selected);}
            public int capacity(){return container.capacity(selected);}
            public boolean accepts(tc4revival.aspect.Aspect aspect){return container.accepts(aspect);}
            public boolean acceptsPhials(){return true;}
            public boolean contains(tc4revival.aspect.Aspect aspect,int amount){return container.contains(aspect,amount);}
            public boolean takeFromContainer(tc4revival.aspect.Aspect aspect,int amount){return container.takeFromContainer(aspect,amount);}
            public int addToContainer(tc4revival.aspect.Aspect aspect,int amount){return container.addToContainer(aspect,amount);}
        };
    }
    public static boolean drainEssentia(net.minecraft.world.level.block.entity.BlockEntity consumer,Aspect aspect,net.minecraft.core.Direction direction,int range) { return tc4revival.content.EssentiaSources.drain(consumer,aspect.unwrap(),direction,range,false).isPresent(); }
    public static boolean findEssentia(net.minecraft.world.level.block.entity.BlockEntity consumer,Aspect aspect,net.minecraft.core.Direction direction,int range) { return tc4revival.content.EssentiaSources.contains(consumer,aspect.unwrap(),direction,range); }
    public static void refresh(net.minecraft.world.level.block.entity.BlockEntity consumer) { tc4revival.content.EssentiaSources.refresh(consumer); }
}
