package ttmodern;

@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid=Tinkerer.ID,bus=net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.MOD,value=net.minecraftforge.api.distmarker.Dist.CLIENT)
public final class TalismanModels {
    @net.minecraftforge.eventbus.api.SubscribeEvent public static void setup(net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent event){event.enqueueWork(()->{
        net.minecraft.client.renderer.item.ItemProperties.register(Tinkerer.BLOOD_SWORD.get(),net.minecraft.resources.ResourceLocation.parse(Tinkerer.ID+":enabled"),(stack,level,entity,seed)->BloodSwordItem.active(stack)?1:0);
        net.minecraft.client.renderer.item.ItemProperties.register(Tinkerer.INFUSED_SEEDS.get(),net.minecraft.resources.ResourceLocation.parse(Tinkerer.ID+":aspect"),(stack,level,entity,seed)->CropTraits.texture(InfusedSeedItem.aspect(stack)));
        for(var tool:java.util.List.of(Tinkerer.AWAKENED_PICK.get(),Tinkerer.AWAKENED_SHOVEL.get(),Tinkerer.AWAKENED_AXE.get(),Tinkerer.AWAKENED_SWORD.get()))net.minecraft.client.renderer.item.ItemProperties.register(tool,net.minecraft.resources.ResourceLocation.parse(Tinkerer.ID+":tool_mode"),(stack,level,entity,seed)->AwakenedModes.mode(stack));
        net.minecraft.client.renderer.item.ItemProperties.register(Tinkerer.BLOCK_TALISMAN.get(),net.minecraft.resources.ResourceLocation.parse(Tinkerer.ID+":enabled"),(stack,level,entity,seed)->BlockTalismanItem.active(stack)?1:0);
        var key=net.minecraft.resources.ResourceLocation.parse(Tinkerer.ID+":enabled");
        net.minecraft.client.renderer.item.ItemProperties.register(Tinkerer.XP_TALISMAN.get(),key,(stack,level,entity,seed)->XpTalismanItem.active(stack)?1:0);
        net.minecraft.client.renderer.item.ItemProperties.register(Tinkerer.CLEANSING_TALISMAN.get(),key,(stack,level,entity,seed)->CleansingTalismanItem.active(stack)?1:0);
    });}
}
