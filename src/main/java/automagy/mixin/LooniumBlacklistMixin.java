package automagy.mixin;

import java.util.List;
import java.util.function.Predicate;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets="vazkii.botania.common.block.flower.functional.LooniumBlockEntity",remap=false)
public class LooniumBlacklistMixin {
    @Redirect(method="pickRandomLootItem",at=@At(value="INVOKE",target="Ljava/util/List;removeIf(Ljava/util/function/Predicate;)Z"),remap=false)
    private static boolean automagy$phials(List<ItemStack> loot,Predicate<ItemStack> original) {
        return loot.removeIf(stack->original.test(stack)||(automagy.AutomagyConfig.LOONIUM_BLACKLIST.get()
                &&stack.is(automagy.content.Content.EXPERIENCE_PHIAL.get())));
    }
}
