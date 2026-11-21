package tc4revival.content;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import tc4revival.TC4Revival;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public  class GolemOwnershipServerTests {
    @GameTest(template = "wand_test_empty")
    public static void golemOwnerRightClickBoundaries(GameTestHelper test) {
        var player = new FakePlayer(test.getLevel(), new GameProfile(UUID.randomUUID(), "GolemOwnerTest"));
        player.setGameMode(GameType.SURVIVAL);
        for (var material : GolemMaterial.values()) {
            var golem = TCEntities.GOLEM.get().create(test.getLevel());
            var config = new GolemConfiguration(material, true);
            config.installCore(GolemCoreType.FILL);
            golem.state().configure(config);
            golem.state().owner("SomeoneElse");
            golem.refreshConfiguration();
            golem.setHealth(golem.getMaxHealth() - 2);
            for (var hand : InteractionHand.values()) {
                for (var stack : java.util.List.of(new ItemStack(TCItems.GOLEM_BELL.get()),
                        new ItemStack(TCItems.golemUpgrade(0)), new ItemStack(Items.COOKIE, 3), ItemStack.EMPTY)) {
                    player.setItemInHand(hand, stack.copy());
                    var before = player.getItemInHand(hand).copy();
                    var state = golem.state().visualData();
                    var menu = player.containerMenu;
                    float health = golem.getHealth();
                    // Player.interactOn includes the real Forge hook before entity/item callbacks.
                    test.assertTrue(player.interactOn(golem, hand) == InteractionResult.PASS,
                            "Non-owner interaction is canceled with the old default result");
                    test.assertTrue(ItemStack.matches(before, player.getItemInHand(hand))
                            && state.equals(golem.state().visualData()) && golem.getHealth() == health
                            && menu == player.containerMenu, "Denied bell/component/cookie/menu leaves all state unchanged");
                }
            }
            player.setGameMode(GameType.CREATIVE);
            test.assertTrue(ForgeHooks.onInteractEntity(player, golem, InteractionHand.MAIN_HAND) != null,
                    "Creative does not bypass old ownership guard");
            player.setGameMode(GameType.SURVIVAL);
            golem.state().owner("golemownertest");
            test.assertTrue(ForgeHooks.onInteractEntity(player, golem, InteractionHand.MAIN_HAND) != null,
                    "Old owner names are case-sensitive");
            golem.state().owner(player.getGameProfile().getName());
            var saved = new CompoundTag();
            golem.saveWithoutId(saved);
            var restored = TCEntities.GOLEM.get().create(test.getLevel());
            restored.load(saved);
            test.assertTrue(ForgeHooks.onInteractEntity(player, restored, InteractionHand.MAIN_HAND) == null,
                    "Saved owner retains permission");
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(TCItems.GOLEM_BELL.get()));
            test.assertTrue(player.interactOn(restored, InteractionHand.MAIN_HAND).consumesAction()
                            && player.getMainHandItem().getOrCreateTag().getInt("golemid") == restored.getId(),
                    "Owner can select golem with actual bell callback");
            restored.state().owner("");
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(TCItems.golemUpgrade(0)));
            test.assertTrue(player.interactOn(restored, InteractionHand.MAIN_HAND).consumesAction()
                            && player.getMainHandItem().isEmpty() && restored.configuration().upgrades(0) == 1,
                    "Unowned golem remains configurable through actual component callback");
            restored.discard();
            golem.discard();
        }
        var trunk = TCEntities.TRAVELING_TRUNK.get().create(test.getLevel());
        test.assertTrue(ForgeHooks.onInteractEntity(player, trunk, InteractionHand.MAIN_HAND) == null,
                "Traveling trunk retains its separate ownership policy");
        trunk.discard();
        TC4Revival.LOGGER.info("Golem ownership PASS: all-materials-both-hands-real-interaction-owner-unowned-creative-persistence");
        test.succeed();
    }
}
