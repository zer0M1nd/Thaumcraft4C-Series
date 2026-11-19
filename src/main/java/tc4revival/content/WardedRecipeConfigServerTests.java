package tc4revival.content;

import com.mojang.authlib.GameProfile;
import io.netty.buffer.Unpooled;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;
import tc4revival.config.TCConfig;
import tc4revival.research.KnowledgeSavedData;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public  class WardedRecipeConfigServerTests {
    @GameTest(template="wand_test_empty")
    public static void wardedRecipeConfigurationPreservesLegacyExceptions(GameTestHelper test) {
        var level=test.getLevel(); var pos=test.absolutePos(new BlockPos(2,2,2));
        var previousBlock=level.getBlockState(pos); boolean previous=TCConfig.SERVER.wardedStone.get();
        var player=new FakePlayer(level,new GameProfile(UUID.randomUUID(),"WardedRecipeTest"));
        player.setGameMode(GameType.SURVIVAL); player.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+.5);
        KnowledgeSavedData.get(player.server).update(player.getUUID(),bound->0,knowledge->{
            knowledge.rememberResearch("WARDEDARCANA"); return null;
        });
        // The old config condition enclosed only gold keys and pressure plates.
        String[] outputs={"gold_key","arcane_pressure_plate","iron_key","arcane_door","warded_glass"};
        String[][] layouts={{"ggG","g  ","   "},{" B ","TDT","   "},{"iiI","i  ","   "},
                {"TDT","DBD","TDT"},{"XXX","DBD","XXX"}};
        try {
            level.setBlock(pos,TCArcane.BLOCK.get().defaultBlockState(),3);
            var table=(ArcaneWorkbenchBlockEntity)level.getBlockEntity(pos);
            var menu=new ArcaneWorkbenchMenu(1,player.getInventory(),table); player.containerMenu=menu;
            for (int index=0;index<outputs.length;index++) {
                var id=ResourceLocation.parse("thaumcraft:arcane/"+outputs[index]);
                var recipe=(ArcaneRecipe)level.getRecipeManager().byKey(id).orElseThrow();
                var buffer=new FriendlyByteBuf(Unpooled.buffer());
                ArcaneRecipe synced;
                try {
                    var serializer=new ArcaneRecipe.Serializer(); serializer.toNetwork(buffer,recipe);
                    synced=serializer.fromNetwork(id,buffer);
                    test.assertTrue(buffer.readableBytes()==0,"Recipe synchronization consumes full definition");
                } finally { buffer.release(); }
                var grid=new TransientCraftingContainer(menu,3,3);
                for (int slot=0;slot<9;slot++) {
                    var stack=ingredient(layouts[index][slot/3].charAt(slot%3));
                    grid.setItem(slot,stack.copy()); table.setItem(slot,stack.copy());
                }
                var wand=TCItems.CASTING_WAND.get().create(WandParts.Rod.SILVERWOOD,WandParts.Cap.THAUMIUM,false,true);
                table.setItem(10,wand); menu.setCarried(ItemStack.EMPTY);
                TCConfig.SERVER.wardedStone.set(true); menu.refresh();
                test.assertTrue(recipe.matches(grid,level) && synced.matches(grid,level) && !menu.getSlot(0).getItem().isEmpty(),
                        "Enabled recipe works in loaded, synchronized and menu paths: "+id);
                TCConfig.SERVER.wardedStone.set(false);
                boolean permitted=index>=2;
                test.assertTrue(recipe.matches(grid,level)==permitted && synced.matches(grid,level)==permitted,
                        "Disabled config preserves exact old conditional-registration boundary: "+id);
                var before=table.saveWithoutMetadata();
                // Click revalidates an output that was displayed before the setting changed.
                menu.clicked(0,0,ClickType.PICKUP,player);
                if (!permitted) {
                    test.assertTrue(menu.getCarried().isEmpty() && menu.getSlot(0).getItem().isEmpty()
                            && before.equals(table.saveWithoutMetadata()),"Blocked craft consumes no materials or vis: "+id);
                    TCConfig.SERVER.wardedStone.set(true); menu.refresh();
                    menu.clicked(0,0,ClickType.PICKUP,player);
                }
                var expected=ForgeRegistries.ITEMS.getValue(ResourceLocation.parse("thaumcraft:"+outputs[index]));
                int count=index==0 || index==2 ? 2 : index==4 ? 8 : 1;
                test.assertTrue(menu.getCarried().is(expected) && menu.getCarried().getCount()==count,
                        "Retained research crafts exact output when recipe is available: "+id);
                for (int slot=0;slot<9;slot++) test.assertTrue(table.getItem(slot).isEmpty(),"Craft consumes ingredient once");
                test.assertTrue(!ItemStack.matches(wand,TCItems.CASTING_WAND.get().create(
                        WandParts.Rod.SILVERWOOD,WandParts.Cap.THAUMIUM,false,true)),"Survival craft charges vis");
            }
            menu.setCarried(ItemStack.EMPTY);
            tc4revival.TC4Revival.LOGGER.info("PASS wardedRecipeConfigurationPreservesLegacyExceptions");
            test.succeed();
        } finally {
            player.containerMenu=player.inventoryMenu; TCConfig.SERVER.wardedStone.set(previous);
            level.setBlock(pos,previousBlock,3);
        }
    }
    private static ItemStack ingredient(char symbol) {
        String id=switch(symbol) {
            case 'g' -> "minecraft:gold_nugget"; case 'G' -> "minecraft:gold_ingot";
            case 'i' -> "minecraft:iron_nugget"; case 'I' -> "minecraft:iron_ingot";
            case 'B' -> "thaumcraft:zombie_brain"; case 'T' -> "thaumcraft:thaumium_ingot";
            case 'D' -> "thaumcraft:greatwood_planks"; case 'X' -> "minecraft:glass";
            default -> null;
        };
        return id==null ? ItemStack.EMPTY : new ItemStack(ForgeRegistries.ITEMS.getValue(ResourceLocation.parse(id)));
    }
}
