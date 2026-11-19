package tc4revival.debug;

import com.mojang.brigadier.builder.ArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import tc4revival.aspect.item.ItemStackAspects;
import tc4revival.aspect.item.PotionAspects;
import tc4revival.aspect.recipe.RecipeAspectData.Key;
import tc4revival.aspect.recipe.RecipeAspects;

 class PotionAspectDebugCommands {
    private PotionAspectDebugCommands() {}
    static ArgumentBuilder<CommandSourceStack, ?> tree() {
        return Commands.literal("potion_aspects").then(Commands.literal("verify").executes(context -> {
            int checked = 0;
            for (var entry : PotionAspects.data().defaults().entrySet()) {
                var stack = new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation(entry.getKey().item())));
                var tag = new CompoundTag();
                tag.putString("Potion", entry.getKey().potion());
                stack.setTag(tag);
                var expectedKey = new Key("minecraft:potion", entry.getValue());
                var actual = ItemStackAspects.resolve(stack);
                var expected = RecipeAspects.newEngine().object(expectedKey);
                if (!PotionAspects.key(stack).orElseThrow().equals(expectedKey) || actual.isEmpty() || !actual.get().view().equals(expected.view())) {
                    context.getSource().sendFailure(Component.literal("Potion identity/aspect mismatch: " + entry));
                    return 0;
                }
                checked++;
            }
            int total = checked;
            context.getSource().sendSuccess(() -> Component.literal("Potion checks passed: " + total + " actual drink/splash variants and legacy metadata identities."), false);
            return 1;
        }));
    }
}
