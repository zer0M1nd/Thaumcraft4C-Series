package thaumicomniverse.mixin;

import java.util.List;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Pseudo
@Mixin(targets = "dev.shadowsoffire.apotheosis.adventure.socket.gem.bonus.special.AllStatsBonus", remap = false)
public abstract class ApotheosisAllStatsMixin {
    @ModifyVariable(method = "<init>", at = @At("HEAD"), argsOnly = true, remap = false)
    private static List<Attribute> omniverse$excludeThaumcraft(List<Attribute> attributes) {
        return attributes.stream().filter(attribute -> {
            var id = ForgeRegistries.ATTRIBUTES.getKey(attribute);
            return id == null || !id.getNamespace().equals("thaumcraft");
        }).toList();
    }
}
