package heartwork.compat.blade;

import mods.flammpfeil.slashblade.client.renderer.entity.JudgementCutRenderer;
import mods.flammpfeil.slashblade.client.renderer.entity.SummonedSwordRenderer;
import net.minecraftforge.client.event.EntityRenderersEvent;

public final class BladeClient {
    public static void models(net.minecraftforge.client.event.ModelEvent.ModifyBakingResult event) {
        net.minecraftforge.registries.ForgeRegistries.ITEMS.getEntries().forEach(entry -> {
            if(!entry.getKey().location().getNamespace().equals("heartwork") || !(entry.getValue() instanceof mods.flammpfeil.slashblade.item.ItemSlashBlade))return;
            var location=new net.minecraft.client.resources.model.ModelResourceLocation(entry.getKey().location(),"inventory");
            var base=event.getModels().get(location);
            if(base!=null)event.getModels().put(location,new mods.flammpfeil.slashblade.client.renderer.model.BladeModel(base,event.getModelBakery()) {
                private final net.minecraft.client.renderer.block.model.ItemOverrides overrides=new net.minecraft.client.renderer.block.model.ItemOverrides() {
                    @Override public net.minecraft.client.resources.model.BakedModel resolve(net.minecraft.client.resources.model.BakedModel model,net.minecraft.world.item.ItemStack stack,
                            net.minecraft.client.multiplayer.ClientLevel level,net.minecraft.world.entity.LivingEntity entity,int seed) {
                        mods.flammpfeil.slashblade.client.renderer.model.BladeModel.user=entity;
                        return model;
                    }
                };
                @Override public net.minecraft.client.renderer.block.model.ItemOverrides getOverrides(){return overrides;}
            });
        });
    }
    private static final net.minecraft.client.KeyMapping EMPOWER=new net.minecraft.client.KeyMapping("key.heartwork.empower",org.lwjgl.glfw.GLFW.GLFW_KEY_V,"key.categories.heartwork");
    public static void keys(net.minecraftforge.client.event.RegisterKeyMappingsEvent event){
        event.register(EMPOWER);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(BladeClient::tick);
    }
    private static void tick(net.minecraftforge.event.TickEvent.ClientTickEvent event){
        if(event.phase!=net.minecraftforge.event.TickEvent.Phase.END)return;
        var client=net.minecraft.client.Minecraft.getInstance();
        while(EMPOWER.consumeClick())if(client.player!=null&&client.screen==null&&client.player.getMainHandItem().getItem() instanceof FluxBladeItem)BladeNetwork.toggle();
    }
    public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(BladeCompat.CUT.get(),JudgementCutRenderer::new);
        event.registerEntityRenderer(BladeCompat.SWORDS.get(),SummonedSwordRenderer::new);
        event.registerEntityRenderer(BladeCompat.DRIVE.get(),mods.flammpfeil.slashblade.client.renderer.entity.DriveRenderer::new);
    }
}
