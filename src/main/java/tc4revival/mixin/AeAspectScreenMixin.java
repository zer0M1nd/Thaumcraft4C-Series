package tc4revival.mixin;

import java.util.ArrayList;
import java.util.List;
import appeng.client.gui.me.common.Repo;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tc4revival.client.search.AspectSearchQuery;
import tc4revival.client.search.ClientAspectSearch;

@Pseudo
@Mixin(targets = "appeng.client.gui.me.common.MEStorageScreen", remap = false)
public abstract class AeAspectScreenMixin {
    @Shadow @Final protected Repo repo;
    @Shadow private void setSearchText(String text) { throw new AssertionError(); }
    @Unique private List<Component> thaumcraft$originalTip;
    @Unique private List<Component> thaumcraft$aspectTip;
    @Unique private long thaumcraft$epoch;

    @ModifyArg(method = "updateSearch", at = @At(value = "INVOKE",
            target = "Lappeng/client/gui/widgets/AETextField;setTooltipMessage(Ljava/util/List;)V"), index = 0)
    private List<Component> thaumcraft$tooltip(List<Component> lines) {
        if (!lines.equals(thaumcraft$originalTip)) {
            var extended = new ArrayList<>(lines);
            extended.add(Component.translatable("thaumcraft.search.item_aspects"));
            thaumcraft$originalTip = lines;
            thaumcraft$aspectTip = List.copyOf(extended);
        }
        return thaumcraft$aspectTip;
    }

    @Inject(method = "updateSearch", at = @At("TAIL"))
    private void thaumcraft$refresh(CallbackInfo ci) {
        String text = repo.getSearchString();
        if (!AspectSearchQuery.hasAspectTerm(text)) return;
        long epoch = ClientAspectSearch.epoch();
        if (epoch != thaumcraft$epoch) {
            thaumcraft$epoch = epoch;
            setSearchText(text);
        }
    }
}
