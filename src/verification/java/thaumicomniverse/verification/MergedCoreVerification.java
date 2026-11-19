package thaumicomniverse.verification;

import java.util.ArrayList;
import java.util.Collections;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;
import tc4revival.content.InfusionRecipe;
import tc4revival.content.TCAttributes;
import static thaumicomniverse.verification.ServerVerification.*;

/** Regression checks for the 0.4.68 API integration, separate from shipped content. */
final class MergedCoreVerification {
    static void run(MinecraftServer server) {
        check("ordinary and cheat book server item use", () -> {
            var player = net.minecraftforge.common.util.FakePlayerFactory.getMinecraft(server.overworld());
            var hand = net.minecraft.world.InteractionHand.MAIN_HAND;
            var previous = player.getItemInHand(hand).copy();
            try {
                for (String name : java.util.List.of("thaumcraft:thaumonomicon", "thaumcraft:cheat_thaumonomicon")) {
                    var stack = new ItemStack(ForgeRegistries.ITEMS.getValue(id(name)));
                    player.setItemInHand(hand, stack);
                    var result = stack.use(server.overworld(), player, hand);
                    require(result.getResult().consumesAction(), "Book use rejected " + name);
                    tc4revival.research.ResearchBookSessions.LIVE.close(player);
                }
            } finally { player.setItemInHand(hand, previous); }
        });
        if (ModList.get().isLoaded("mekanismtools") && ModList.get().isLoaded("farmersdelight"))
            check("core 0.4.68 active-hand warp for addon tools", () -> {
                for (String name : java.util.List.of("void_paxel", "void_knife")) {
                    require(ForgeRegistries.ITEMS.containsKey(id(name)), "Missing tool " + name);
                    var stack = new ItemStack(ForgeRegistries.ITEMS.getValue(id(name)));
                    require(!stack.getAttributeModifiers(EquipmentSlot.MAINHAND).get(TCAttributes.WARP.get()).isEmpty(), "Main hand loses warp: " + name);
                    require(stack.getAttributeModifiers(EquipmentSlot.OFFHAND).get(TCAttributes.WARP.get()).isEmpty(), "Offhand warp remains: " + name);
                    require(!stack.getAttributeModifiers(EquipmentSlot.MAINHAND).get(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE).isEmpty(), "Attack damage lost: " + name);
                }
            });
        if (ModList.get().isLoaded("allthemodium")) check("core 0.4.68 ordered certificate infusion and input-aware plan", () -> {
            var recipe = (InfusionRecipe) server.getRecipeManager().byKey(id("atm/thaumaturge_certificate")).orElseThrow();
            var center = new ItemStack(ForgeRegistries.ITEMS.getValue(id("thaumcraft:primordial_pearl")));
            var plan = recipe.plan(center);
            var components = new ArrayList<ItemStack>();
            for (var ingredient : plan.components()) components.add(ingredient.displayStacks().get(0).copy());
            var ordered = new InfusionRecipe.Input(center, components, key -> true, true);
            require(recipe.matches(ordered, server.overworld()), "Native ordered match rejected certificate");
            require(recipe.planForCraft(ordered, server.overworld()).components().size() == plan.components().size(), "Input-aware plan changed components");
            Collections.rotate(components, 2);
            require(recipe.matches(new InfusionRecipe.Input(center, components, key -> true, true), server.overworld()), "Cyclic rotation rejected");
            Collections.reverse(components);
            require(!recipe.matches(new InfusionRecipe.Input(center, components, key -> true, true), server.overworld()), "Reverse order accepted");
            require(recipe.matches(new InfusionRecipe.Input(center, components, key -> true, false), server.overworld()), "Default unordered behavior lost");
            require(!recipe.matches(new InfusionRecipe.Input(center, components, key -> false, false), server.overworld()), "Research gate bypassed");
        });
    }
}
