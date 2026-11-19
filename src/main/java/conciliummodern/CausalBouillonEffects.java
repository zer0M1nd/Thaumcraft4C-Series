package conciliummodern;

@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid=ThaumicConcilium.ID,bus=net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.MOD,value=net.minecraftforge.api.distmarker.Dist.CLIENT)
public final class CausalBouillonEffects extends net.minecraft.client.renderer.DimensionSpecialEffects {
    public CausalBouillonEffects(){super(Float.NaN,false,SkyType.NONE,false,false);}
    @Override public net.minecraft.world.phys.Vec3 getBrightnessDependentFogColor(net.minecraft.world.phys.Vec3 color,float brightness){return net.minecraft.world.phys.Vec3.fromRGB24(10518688).scale(.15);}
    @Override public boolean isFoggyAt(int x,int z){return true;}
    @net.minecraftforge.eventbus.api.SubscribeEvent public static void register(net.minecraftforge.client.event.RegisterDimensionSpecialEffectsEvent event){event.register(RiftTransit.DIMENSION.location(),new CausalBouillonEffects());}
}
