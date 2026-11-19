package thaumicomniverse.mixin;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.fml.ModList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Loot item IDs are parsed before pool conditions; gate our optional tables before that parse. */
@Mixin(value = ForgeHooks.class, remap = false)
public abstract class ConditionalLootTablesMixin {
    @Inject(method = "loadLootTable", at = @At("HEAD"), cancellable = true)
    private static void omniverse$optionalLoot(Gson gson, ResourceLocation name, JsonElement json, boolean custom,
                                               CallbackInfoReturnable<LootTable> result) {
        if (!name.getNamespace().equals("thaumicomniverse") || !json.isJsonObject()
                || !json.getAsJsonObject().has("omniverse_requires")) return;
        for (var mod : json.getAsJsonObject().getAsJsonArray("omniverse_requires"))
            if (!ModList.get().isLoaded(mod.getAsString())) { result.setReturnValue(LootTable.EMPTY); return; }
    }
}
