package tc4revival.content;

import java.util.*;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;
import tc4revival.aspect.TCAspects;
import tc4revival.config.TCConfig;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public  class CreativeInventoryServerTests {
    @GameTest(template="wand_test_empty")
    public static void creativeContentsCoverRegistryAndLegacyVariants(GameTestHelper test) {
        var tab=TCCreativeTabs.THAUMCRAFT.get(); var level=test.getLevel();
        var parameters=new CreativeModeTab.ItemDisplayParameters(level.enabledFeatures(),true,level.registryAccess());
        boolean previous=TCConfig.SERVER.allowCheatSheet.get(); int registered=0, displayed=0;
        try {
            for (boolean cheats:new boolean[]{false,true}) {
                TCConfig.SERVER.allowCheatSheet.set(cheats); tab.buildContents(parameters);
                var entries=List.copyOf(tab.getDisplayItems()); displayed=entries.size(); registered=0;
                for (var stack:entries) test.assertTrue(!stack.isEmpty() && stack.getCount()==1,"Every real tab entry is a single nonempty stack");
                for (var entry:ForgeRegistries.ITEMS.getEntries()) if (entry.getKey().location().getNamespace().equals("thaumcraft")) {
                    registered++;
                    test.assertTrue(entries.stream().anyMatch(stack->stack.is(entry.getValue())),"User-requested test access includes registered item "+entry.getKey().location());
                }
                test.assertTrue(entries.stream().anyMatch(stack->stack.is(Items.IRON_NUGGET))
                        && !ForgeRegistries.ITEMS.containsKey(ResourceLocation.parse("thaumcraft:iron_nugget")),"Iron nuggets reuse vanilla identity in the actual tab");
                for (var stack:entries) test.assertTrue(tab.getSearchTabDisplayItems().stream().anyMatch(other->ItemStack.isSameItemSameTags(stack,other)),
                        "Every displayed NBT variant is searchable");
                var allWands=entries.stream().filter(stack->stack.is(TCItems.CASTING_WAND.get())).toList();
                var wands=allWands.stream().filter(stack->!stack.getOrCreateTag().getString("rod").contains(":")
                        &&!stack.getOrCreateTag().getString("cap").contains(":")).toList();
                test.assertTrue(wands.size()==4,"Exactly the four original creative wand variants");
                for(var rod:WandParts.Rod.values())if(rod.tag.contains(":"))
                    test.assertTrue(allWands.stream().anyMatch(stack->stack.getOrCreateTag().getString("rod").equals(rod.tag)),"Registered addon rod is available: "+rod.tag);
                for(var cap:WandParts.Cap.values())if(cap.tag.contains(":"))
                    test.assertTrue(allWands.stream().anyMatch(stack->stack.getOrCreateTag().getString("cap").equals(cap.tag)),"Registered addon cap is available: "+cap.tag);
                for(var stack:allWands)for(var aspect:TCAspects.catalog().primals())
                    test.assertTrue(TCItems.CASTING_WAND.get().vis(stack,aspect)==TCItems.CASTING_WAND.get().capacity(stack),"Base and addon creative wands start full");
                String[] rods={"wood","greatwood","silverwood","silverwood"},caps={"iron","gold","thaumium","thaumium"};
                int[] metadata={0,9,54,128};
                for (int i=0;i<4;i++) {
                    var stack=wands.get(i); var tag=stack.getTag();
                    test.assertTrue(tag.getString("rod").equals(rods[i]) && tag.getString("cap").equals(caps[i])
                            && tag.getInt("tc4_metadata")==metadata[i] && WandParts.sceptre(tag)==(i==3),"Original creative wand data and order");
                    for (var aspect:TCAspects.catalog().primals()) test.assertTrue(TCItems.CASTING_WAND.get().vis(stack,aspect)==TCItems.CASTING_WAND.get().capacity(stack),"Creative wands start full for every primal");
                }
                for (var aspect:TCAspects.catalog().all()) {
                    test.assertTrue(entries.stream().filter(stack->stack.is(TCItems.ESSENTIA_PHIAL.get())).anyMatch(stack->{
                        var amounts=TCItems.ESSENTIA_PHIAL.get().storedAspects(stack); return amounts.size()==1&&amounts.amount(aspect)==8;
                    }),"Creative phial contains eight units: "+aspect.tag());
                    test.assertTrue(entries.stream().filter(stack->stack.is(TCItems.WISP_ESSENCE.get())).anyMatch(stack->{
                        var amounts=TCItems.WISP_ESSENCE.get().storedAspects(stack); return amounts.size()==1&&amounts.amount(aspect)==2;
                    }),"Creative essence contains two units: "+aspect.tag());
                }
                test.assertTrue(entries.stream().anyMatch(stack->ItemStack.isSameItemSameTags(stack,CrimsonBanner.item())),"Native crimson banner is available");
                var before=entries.stream().map(ItemStack::copy).toList();
                tab.buildContents(parameters);
                test.assertTrue(before.size()==tab.getDisplayItems().size(),"Repeated build does not append duplicate entries");
                int index=0; for (var stack:tab.getDisplayItems()) test.assertTrue(ItemStack.matches(before.get(index++),stack),"Repeated build preserves ordering and NBT");
            }
            tc4revival.TC4Revival.LOGGER.info("PASS creativeContentsCoverRegistryAndLegacyVariants: {} registered items, {} displayed variants",registered,displayed);
            test.succeed();
        } finally { TCConfig.SERVER.allowCheatSheet.set(previous); tab.buildContents(parameters); }
    }
}
