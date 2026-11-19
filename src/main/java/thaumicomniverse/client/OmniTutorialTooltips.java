package thaumicomniverse.client;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

/** Short mechanical components need a compact explanation, rather than a duplicate scene. */
@Mod.EventBusSubscriber(modid="thaumicomniverse", value=Dist.CLIENT)
public final class OmniTutorialTooltips {
    public static final List<String> ITEMS=List.of("thaumic_casing", "thaumic_hand_crank", "void_backtank",
        "void_backtank_placeable", "solid_aura_block", "solid_aura", "void_sandpaper", "magic_hand");
    @SubscribeEvent public static void tooltip(ItemTooltipEvent event) {
        var id=ForgeRegistries.ITEMS.getKey(event.getItemStack().getItem());
        if(id==null || !id.getNamespace().equals("thaumicomniverse") || !ITEMS.contains(id.getPath()))return;
        if(!Screen.hasShiftDown()) {
            event.getToolTip().add(Component.translatable("thaumicomniverse.tutorial.hold_shift").withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        String path=id.getPath().equals("void_backtank_placeable")?"void_backtank":id.getPath();
        for(int i=1;i<=3;i++) {
            String key="thaumicomniverse.tutorial."+path+"."+i;
            if(net.minecraft.client.resources.language.I18n.exists(key))
                event.getToolTip().add(Component.translatable(key).withStyle(i==1?ChatFormatting.GOLD:ChatFormatting.GRAY));
        }
    }
    private OmniTutorialTooltips() {}
}
