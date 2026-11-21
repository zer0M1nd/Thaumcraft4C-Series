package tc4revival.content;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.resources.ResourceLocation;
import tc4revival.TC4Revival;
import tc4revival.aspect.AspectAmounts;
import tc4revival.aspect.TCAspects;
import tc4revival.research.KnowledgeSavedData;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public  class WorkbenchCatalystServerTests {
    @GameTest(template = "wand_test_empty")
    public static void workbenchRetainsPearlAndPhial(GameTestHelper test) {
        var level = test.getLevel();
        var pos = test.absolutePos(new BlockPos(2, 2, 2));
        var original = level.getBlockState(pos);
        var player = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "CatalystTest"));
        player.setGameMode(GameType.SURVIVAL);
        player.setPos(pos.getX() + .5, pos.getY(), pos.getZ() + .5);
        KnowledgeSavedData.get(player.server).update(player.getUUID(), bound -> 0, knowledge -> {
            knowledge.rememberResearch("ADVALCHEMYFURNACE"); return null;
        });
        level.setBlock(pos, TCArcane.BLOCK.get().defaultBlockState(), 3);
        var table = (ArcaneWorkbenchBlockEntity) level.getBlockEntity(pos);
        try {
            var pearl = new ItemStack(TCItems.PRIMORDIAL_PEARL.get());
            pearl.getOrCreateTag().putString("custom", "preserve-me");
            var voidIngot = ForgeRegistries.ITEMS.getValue(ResourceLocation.parse("thaumcraft:void_ingot"));
            for (int slot = 0; slot < 9; slot++) table.setItem(slot, slot == 4 ? pearl.copy()
                    : new ItemStack(slot % 2 == 0 ? voidIngot : TCItems.ALCHEMICAL_CONSTRUCT.get(), 2));
            var wand = TCItems.CASTING_WAND.get().create(WandParts.Rod.SILVERWOOD, WandParts.Cap.THAUMIUM, false, true);
            table.setItem(10, wand);
            var menu = new ArcaneWorkbenchMenu(1, player.getInventory(), table);
            player.containerMenu = menu;
            var wandBefore = wand.copy();
            for (int craft = 0; craft < 2; craft++) {
                menu.refresh();
                test.assertTrue(menu.getSlot(0).getItem().is(TCItems.ADVANCED_ALCHEMICAL_CONSTRUCT.get()),
                        "Real loaded recipe remains available for repeated crafting");
                menu.clicked(0, 0, ClickType.PICKUP, player);
                test.assertTrue(ItemStack.matches(table.getItem(4), pearl), "Pearl stays unchanged in center after craft");
            }
            test.assertTrue(menu.getCarried().is(TCItems.ADVANCED_ALCHEMICAL_CONSTRUCT.get()) && menu.getCarried().getCount() == 8,
                    "Two crafts produce exactly eight advanced constructs");
            for (int slot = 0; slot < 9; slot++) if (slot != 4)
                test.assertTrue(table.getItem(slot).isEmpty(), "Other ingredients are consumed twice");
            test.assertTrue(!ItemStack.matches(wandBefore, wand) && player.getInventory().countItem(TCItems.PRIMORDIAL_PEARL.get()) == 0,
                    "Survival crafting pays vis without moving or duplicating the pearl");
            for (var aspect : TCAspects.catalog().primals()) {
                int spent = switch (aspect.tag()) { case "aqua", "terra" -> 1800; case "ordo" -> 5400; default -> 0; };
                test.assertTrue(TCItems.CASTING_WAND.get().vis(wandBefore, aspect) - TCItems.CASTING_WAND.get().vis(wand, aspect) == spent,
                        "Two crafts debit the exact thaumium-cap vis cost for " + aspect.tag());
            }
            var stored = table.saveWithoutMetadata();
            table.load(stored);
            test.assertTrue(ItemStack.matches(pearl, table.getItem(4)), "Retained catalyst survives table serialization");
            menu.setCarried(ItemStack.EMPTY);
            for (int slot = 0; slot < 9; slot++) table.setItem(slot, ItemStack.EMPTY);
            var phial = new ItemStack(TCItems.ESSENTIA_PHIAL.get());
            TCItems.ESSENTIA_PHIAL.get().setStoredAspects(phial, new AspectAmounts().add(TCAspects.catalog().require("aer"), 8));
            phial.getOrCreateTag().putString("custom", "keep-phial");
            table.setItem(0, new ItemStack(TCItems.JAR_LABEL.get(), 2));
            table.setItem(1, phial.copy());
            for (int craft = 0; craft < 2; craft++) {
                menu.refresh();
                test.assertTrue(menu.getSlot(0).getItem().is(TCItems.JAR_LABEL.get()), "Loaded label recipe matches");
                menu.clicked(0, 0, ClickType.PICKUP, player);
                test.assertTrue(ItemStack.matches(phial, table.getItem(1)), "Label catalyst stays in its original cell with NBT");
            }
            test.assertTrue(menu.getCarried().getCount() == 2 && table.getItem(0).isEmpty()
                            && player.getInventory().countItem(TCItems.ESSENTIA_PHIAL.get()) == 0,
                    "Repeated labels consume only labels and never duplicate phial remainders");
            test.assertTrue(TCItems.JAR_LABEL.get().storedAspects(menu.getCarried()).aspects().contains(TCAspects.catalog().require("aer")),
                    "Produced labels keep selected aspect");
            menu.setCarried(ItemStack.EMPTY);
            player.containerMenu = player.inventoryMenu;
        } finally { level.setBlock(pos, original, 3); }
        TC4Revival.LOGGER.info("Workbench catalysts PASS: repeated-pearl-phial-crafting-material-vis-nbt-storage");
        test.succeed();
    }
}
