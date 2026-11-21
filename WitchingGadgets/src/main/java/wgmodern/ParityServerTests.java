package wgmodern;

import com.google.gson.JsonParser;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("witchinggadgets") @PrefixGameTestTemplate(false)
public class ParityServerTests {
    @GameTest(template="empty") public static void orePreferenceUsesFallbackOnlyWhenPrimaryTagIsEmpty(GameTestHelper h) {
        var fallback = new TagOrItemIngredient(new ResourceLocation("witchinggadgets", "test_absent_metal"), new ResourceLocation("minecraft", "iron_ingot"));
        h.assertTrue(fallback.test(new ItemStack(Items.IRON_INGOT)) && !fallback.test(new ItemStack(Items.DIAMOND)), "Absent silver falls back to iron only");
        var primary = new TagOrItemIngredient(new ResourceLocation("forge", "gems/diamond"), new ResourceLocation("minecraft", "iron_ingot"));
        h.assertTrue(primary.test(new ItemStack(Items.DIAMOND)) && !primary.test(new ItemStack(Items.IRON_INGOT)), "Installed primary ingredient disables the fallback");
        var json = new com.google.gson.JsonObject(); json.add("ingredient", primary.toJson());
        var infusion = new tc4revival.content.InfusionIngredient(json);
        h.assertTrue(infusion.matches(new ItemStack(Items.DIAMOND)) && !infusion.matches(new ItemStack(Items.IRON_INGOT)), "Actual infusion pedestal matching retains the same preference");
        var buffer = new FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        try { TagOrItemIngredient.SERIALIZER.write(buffer, primary); var remote = TagOrItemIngredient.SERIALIZER.parse(buffer); h.assertTrue(remote.test(new ItemStack(Items.DIAMOND)) && !remote.test(new ItemStack(Items.IRON_INGOT)), "Network ingredient retains preference"); } finally { buffer.release(); }
        pass(h);
    }
    @GameTest(template="empty") public static void gemDatapackAffinityRoundtripAndOriginalDefaults(GameTestHelper h) {
        var old = GemAffinities.find(new ItemStack(Items.DIAMOND), h.getLevel());
        h.assertTrue(old.affinities().equals(java.util.Set.of("ordo", "perditio")) && old.aversions().isEmpty(), "Original diamond affinities are supplied by recipes");
        var definition = JsonParser.parseString("{\"ingredient\":{\"item\":\"minecraft:diamond\"},\"affinities\":[\"aer\"],\"aversions\":[\"ignis\"],\"priority\":-2,\"enabled\":false}").getAsJsonObject();
        var recipe = new GemAffinityRecipe(new ResourceLocation("witchinggadgets", "test_affinity_override"), definition);
        var buffer = new FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        try { Content.GEM_AFFINITY_SERIALIZER.get().toNetwork(buffer, recipe); var remote = Content.GEM_AFFINITY_SERIALIZER.get().fromNetwork(recipe.id, buffer); h.assertTrue(!remote.enabled && remote.priority == -2 && remote.definition.affinities().equals(java.util.Set.of("aer")) && remote.definition.aversions().equals(java.util.Set.of("ignis")) && remote.ingredient.test(new ItemStack(Items.DIAMOND)), "Client receives complete custom affinity and removal rules"); } finally { buffer.release(); }
        pass(h);
    }
    @GameTest(template="empty") public static void creativeLocksAndBlockResistanceRetainLegacyRules(GameTestHelper h) {
        var lock = Content.LOCK.get().defaultBlockState(); var plate = Content.OBSIDIAN_PLATE.get().defaultBlockState();
        h.assertTrue(lock.getDestroySpeed(h.getLevel(), h.absolutePos(net.minecraft.core.BlockPos.ZERO)) < 0 && plate.getDestroySpeed(h.getLevel(), h.absolutePos(net.minecraft.core.BlockPos.ZERO)) < 0, "Puzzle locks and plates cannot be bypassed by survival mining");
        h.assertTrue(Content.LOCK.get().getExplosionResistance() == 999 && Content.OBSIDIAN_PLATE.get().getExplosionResistance() == 999, "Original lock explosion resistance is preserved");
        h.assertTrue(Content.SPINNING_WHEEL.get().getExplosionResistance() == 6 && Content.PUMP.get().getExplosionResistance() == 9, "Legacy resistance API conversion is applied to wood and metal devices");
        var aspects = tc4revival.aspect.item.ItemAspects.direct(new ItemStack(Content.GEAR.get("title_crimson_cult").get())).orElseThrow();
        h.assertTrue(aspects.amount(tc4revival.aspect.TCAspects.catalog().require("sensus")) == 2 && aspects.amount(tc4revival.aspect.TCAspects.catalog().require("humanus")) == 1, "Dropped crimson title retains explicit original aspects");
        pass(h);
    }
    private static void pass(GameTestHelper h) { com.mojang.logging.LogUtils.getLogger().info("WITCHINGGADGETS_TEST_PASS {}", Thread.currentThread().getStackTrace()[2].getMethodName()); h.succeed(); }
}
