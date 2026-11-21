package tc4revival.content;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import tc4revival.TC4Revival;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public  class AutomationPickupServerTests {
    @GameTest(template = "wand_test_empty")
    public static void automationPickupConservesGroundItems(GameTestHelper test) {
        var level = test.getLevel();
        var pos = test.absolutePos(new net.minecraft.core.BlockPos(2, 2, 2));
        for (String name : java.util.List.of("FakeThaumcraftGolem", "FakeThaumcraftBore", "FakeThaumcraft")) {
            var worker = new FakePlayer(level, new GameProfile(UUID.randomUUID(), name));
            worker.setGameMode(GameType.SURVIVAL);
            worker.setPos(pos.getX(), pos.getY(), pos.getZ());
            var carried = new ItemStack(Items.STICK, 2);
            carried.getOrCreateTag().putString("workerCargo", "unchanged");
            worker.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, carried.copy());
            var stack = new ItemStack(Items.DIAMOND, 4);
            stack.getOrCreateTag().putString("source", "ground");
            var dropped = new ItemEntity(level, pos.getX(), pos.getY(), pos.getZ(), stack.copy());
            dropped.setNoPickUpDelay();
            try {
                for (int attempt = 0; attempt < 3; attempt++) dropped.playerTouch(worker);
                test.assertTrue(dropped.isAlive() && ItemStack.matches(stack, dropped.getItem())
                                && worker.getInventory().countItem(Items.DIAMOND) == 0
                                && ItemStack.matches(carried, worker.getMainHandItem()),
                        "Actual pickup callback leaves ground and worker inventories unchanged for " + name);
                worker.setGameMode(GameType.CREATIVE);
                dropped.playerTouch(worker);
                test.assertTrue(dropped.isAlive() && worker.getInventory().countItem(Items.DIAMOND) == 0,
                        "Creative automation workers are also excluded");
            } finally { dropped.discard(); }
        }
        for (String name : java.util.List.of("PickupTester", "OtherModWorker", "fakeThaumcraft", "AFakeThaumcraft")) {
            var player = new FakePlayer(level, new GameProfile(UUID.randomUUID(), name));
            player.setGameMode(GameType.SURVIVAL);
            var dropped = new ItemEntity(level, pos.getX(), pos.getY(), pos.getZ(), new ItemStack(Items.DIAMOND, 4));
            dropped.setNoPickUpDelay();
            try {
                dropped.playerTouch(player);
                test.assertTrue(dropped.isRemoved() && player.getInventory().countItem(Items.DIAMOND) == 4,
                        "Other players and near-match names retain actual item pickup: " + name);
            } finally { dropped.discard(); }
        }
        TC4Revival.LOGGER.info("Automation pickup PASS: real-item-touch-prefix-boundaries-count-nbt-inventory-creative");
        test.succeed();
    }
}
