package wgmodern;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Photos retain the animation metadata supplied by the original resource JAR. */
@Mod.EventBusSubscriber(modid="witchinggadgets",value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
public class PhotoSprites {
    private static final Map<ResourceLocation,TextureAtlasSprite> SPRITES=new HashMap<>();

    public static ResourceLocation id(ItemStack stack,boolean overlay) {
        return new ResourceLocation("witchinggadgets","items/mat_"
                +(stack.is(Content.PLATE.get())?"photoplate":"developedphoto")
                +(overlay?"_overlay":""));
    }

    @SubscribeEvent public static void stitched(TextureStitchEvent.Post event) {
        if(!event.getAtlas().location().equals(InventoryMenu.BLOCK_ATLAS))return;
        SPRITES.clear();
        for(String material:new String[]{"photoplate","developedphoto"}) {
            for(String overlay:new String[]{"","_overlay"}) {
                var id=new ResourceLocation("witchinggadgets","items/mat_"+material+overlay);
                SPRITES.put(id,event.getAtlas().getSprite(id));
            }
        }
    }

    public static TextureAtlasSprite get(ItemStack stack,boolean overlay) {
        var id=id(stack,overlay);
        var sprite=SPRITES.get(id);
        return sprite!=null?sprite:Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(id);
    }
}
