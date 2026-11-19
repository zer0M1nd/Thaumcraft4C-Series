package thaumicfutures.mixin;

import com.reborn.modularmachinery.recipe.MachineRecipe;
import com.reborn.modularmachinery.recipe.IOType;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import thaumicfutures.compat.machinery.AspectRequirement;

/** Extend the same text groups used by native JEI measurement and drawing. */
@Mixin(targets = "com.reborn.modularmachinery.client.jei.MachineRecipeText", remap = false)
public abstract class MachineryJeiTextMixin {
    @Inject(method = "tips", at = @At("RETURN"), cancellable = true)
    private static void omniverse$aspects(MachineRecipe recipe, CallbackInfoReturnable<List<List<String>>> callback) {
        var groups = new ArrayList<>(callback.getReturnValue());
        for (var raw : recipe.requirements()) if (raw instanceof AspectRequirement requirement) {
            var lines = new ArrayList<String>();
            lines.add(Component.translatable("omniverse.machine.requirement", requirement.amount, requirement.aspectTag,
                    Component.translatable(requirement.vis ? "omniverse.machine.centivis" : "omniverse.machine.essentia"),
                    Component.translatable(requirement.ioType() == IOType.INPUT ? "omniverse.machine.input" : "omniverse.machine.output")).getString());
            if (requirement.perTick()) lines.add(Component.translatable("omniverse.machine.each_tick").getString());
            if (requirement.chance < 1) lines.add(Component.translatable("omniverse.machine.chance", requirement.chance * 100).getString());
            if (requirement.isParallelizeUnaffected()) lines.add(Component.translatable("omniverse.machine.unaffected").getString());
            groups.add(lines);
        }
        callback.setReturnValue(groups);
    }
}
