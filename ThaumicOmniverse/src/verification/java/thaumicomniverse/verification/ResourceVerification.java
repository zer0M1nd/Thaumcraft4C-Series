package thaumicomniverse.verification;

import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;
import static thaumicomniverse.verification.ServerVerification.*;

final class ResourceVerification {
    static void run(MinecraftServer server) {
        check("conditional renewable recipes and crop registration", () -> {
            try (var stream = thaumicomniverse.ThaumicOmniverse.class.getResourceAsStream("/thaumicomniverse/renewables.json");
                 var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                for (var value : JsonParser.parseReader(reader).getAsJsonArray()) {
                    var row = value.getAsJsonObject(); String name = row.get("name").getAsString();
                    boolean material = ModList.get().isLoaded(row.get("mod").getAsString()) && ForgeRegistries.ITEMS.containsKey(id(row.get("item").getAsString()));
                    boolean crops = material && ModList.get().isLoaded("mysticalagriculture");
                    boolean bees = material && ModList.get().isLoaded("productivebees");
                    require(ForgeRegistries.ITEMS.containsKey(id("mysticalagriculture:omniverse_" + name + "_seeds")) == crops, "Crop gate " + name);
                    if (crops) {
                        var crop = com.blakebr0.mysticalagriculture.registry.CropRegistry.getInstance().getCropByName("omniverse_" + name);
                        int expected = name.equals("primordial_pearl") && ModList.get().isLoaded("mysticalagradditions") ? 6 : row.get("tier").getAsInt();
                        require(crop.getTier().getValue() == expected, "Crop tier " + name);
                        if (expected == 6) require(crop.getTier().getId().toString().equals("mysticalagradditions:6"), "Native tier-six behavior");
                    }
                    require(server.getRecipeManager().byKey(id("collab/crops/" + name)).isPresent() == crops, "Crop recipe " + name);
                    require(server.getRecipeManager().byKey(id("productivebees:centrifuge/thaumicomniverse/" + name)).isPresent() == bees, "Bee centrifuge recipe " + name);
                    for (String type : java.util.List.of("produce", "conversion"))
                        require(server.getRecipeManager().byKey(id("collab/bees/" + name + "_" + type)).isPresent() == bees, "Bee " + type + " recipe " + name);
                }
            } catch (java.io.IOException error) { throw new AssertionError(error); }
        });
        check("compass mode cycle and displaced boss conquest", () -> {
            var level = server.overworld();
            var player = net.minecraftforge.common.util.FakePlayerFactory.getMinecraft(level);
            var stack = new ItemStack(ForgeRegistries.ITEMS.getValue(id("primal_compass")));
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, stack);
            for (int i = 0; i < 5; i++) {
                stack.getItem().use(level, player, net.minecraft.world.InteractionHand.MAIN_HAND);
                require(thaumicomniverse.content.ObeliskCompassItem.boss(stack) == (i == 4 ? -1 : i), "Boss selector cycle");
            }
            var entity = net.minecraft.world.entity.EntityType.ZOMBIE.create(level);
            entity.getPersistentData().putLong("thaumicomniverse:ObeliskHome", net.minecraft.world.level.ChunkPos.asLong(-765, 543));
            var drops = new java.util.ArrayList<net.minecraft.world.entity.item.ItemEntity>();
            drops.add(new net.minecraft.world.entity.item.ItemEntity(level, 0, 100, 0,
                    new ItemStack(tc4revival.content.TCItems.PRIMORDIAL_PEARL.get())));
            thaumicomniverse.content.ObeliskClaims.bossDefeated(new net.minecraftforge.event.entity.living.LivingDropsEvent(
                    entity, level.damageSources().generic(), drops, 0, false));
            require(thaumicomniverse.content.ObeliskClaims.get(server).conquered(-765, 543), "Lost original obelisk after boss moved");
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        });
    }
}
