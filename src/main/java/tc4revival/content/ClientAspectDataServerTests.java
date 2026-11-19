package tc4revival.content;

import java.util.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraftforge.gametest.*;
import net.minecraftforge.registries.ForgeRegistries;
import tc4revival.aspect.*;
import tc4revival.aspect.item.*;
import tc4revival.aspect.sync.*;
import tc4revival.research.*;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public class ClientAspectDataServerTests {
    @GameTest(template="wand_test_empty",timeoutTicks=1200)
    public static void clientAspectTablePrewarmsAndCachesOnlyRelevantState(GameTestHelper t){
        var cache=new ItemAspectCache(isolated(ServerAspectData.snapshot()));
        var stacks=new ArrayList<ItemStack>();BrowserQueryStacks.enumerate(stacks::add);
        int[] at={0};
        t.onEachTick(()->{
            for(int i=0;i<32&&at[0]<stacks.size();i++){
                var stack=stacks.get(at[0]++);var actual=cache.resolve(stack);
                t.assertTrue(actual.amounts().view().equals(ItemStackAspects.resolve(stack).orElseGet(AspectAmounts::new).view()),"Cached prewarm parity: "+stack);
            }
            if(at[0]<stacks.size())return;
            int initial=cache.size();long start=System.nanoTime();
            var pearl=new ItemStack(Items.ENDER_PEARL);
            var expected=cache.resolve(pearl).amounts().view();
            // A large tag written by another mod must neither be copied nor hashed for aspect lookup.
            pearl.getOrCreateTag().putByteArray("unrelated_payload",new byte[1000000]);
            for(int i=0;i<10000;i++){
                pearl.getOrCreateTag().putInt("unrelated_energy",i);pearl.setCount(i%16+1);
                t.assertTrue(cache.resolve(pearl).amounts().view().equals(expected),"Irrelevant NBT must reuse result");
            }
            t.assertTrue(cache.size()==initial,"Inventory moves/irrelevant NBT never add or evict table entries");
            long elapsed=System.nanoTime()-start;
            var sword=new ItemStack(Items.DIAMOND_SWORD);sword.enchant(Enchantments.SHARPNESS,3);
            cache.resolve(sword);t.assertTrue(cache.size()==initial+1,"New enchantment adds only one entry");
            sword.setDamageValue(40);sword.setHoverName(net.minecraft.network.chat.Component.literal("Renamed"));
            cache.resolve(sword);t.assertTrue(cache.size()==initial+1,"Damage and name reuse cached enchanted result");
            var essence=new ItemStack(TCItems.WISP_ESSENCE.get());
            TCItems.WISP_ESSENCE.get().setStoredAspects(essence,new AspectAmounts().add(TCAspects.catalog().require("aer"),4));
            var before=cache.resolve(essence);
            TCItems.WISP_ESSENCE.get().setStoredAspects(essence,new AspectAmounts().add(TCAspects.catalog().require("aer"),12));
            var after=cache.resolve(essence);
            t.assertTrue(!before.amounts().view().equals(after.amounts().view()),"Stored-aspect callback sees changed NBT");
            after.amounts().add(TCAspects.catalog().require("aer"),100);
            t.assertTrue(!after.amounts().view().equals(cache.resolve(essence).amounts().view()),"Caller cannot mutate table");
            tc4revival.TC4Revival.LOGGER.info("CLIENT_ASPECT_CACHE PASS: {} prewarmed states; 10000 hits with 1MB unrelated NBT in {} ms; no new entries",initial,elapsed/1_000_000.0);
            t.succeed();
        });
    }
    private static AspectCalculationData isolated(AspectDataSet files){return new AspectCalculationData(files,TCAspects.catalog(),(id,tag)->ForgeRegistries.ITEMS.getValue(net.minecraft.resources.ResourceLocation.parse(id)).builtInRegistryHolder().is(net.minecraft.tags.ItemTags.create(net.minecraft.resources.ResourceLocation.parse(tag))));}
    @GameTest(template="wand_test_empty",timeoutTicks=1200)
    public static void clientAspectSnapshotMatchesServerForAllEnumeratedStacks(GameTestHelper t){
        var data=isolated(ServerAspectData.snapshot());var stacks=new ArrayList<ItemStack>();BrowserQueryStacks.enumerate(stacks::add);
        var sword=new ItemStack(Items.DIAMOND_SWORD);sword.enchant(Enchantments.SHARPNESS,3);stacks.add(sword);
        var wand=TCItems.CASTING_WAND.get().create(WandParts.Rod.GREATWOOD,WandParts.Cap.GOLD,false,true);stacks.add(wand);
        var essence=new ItemStack(TCItems.WISP_ESSENCE.get());TCItems.WISP_ESSENCE.get().setStoredAspects(essence,new AspectAmounts().add(TCAspects.catalog().require("aer"),4));stacks.add(essence);
        int[] at={0};t.onEachTick(()->{
            for(int i=0;i<32&&at[0]<stacks.size();i++){
                var stack=stacks.get(at[0]++);var server=ItemStackAspects.resolve(stack).orElseGet(AspectAmounts::new);var client=ItemStackAspects.resolve(stack,data).orElseGet(AspectAmounts::new);
                t.assertTrue(server.view().equals(client.view()),"Isolated evaluator differs: "+stack);
                t.assertTrue(ScanIdentity.item(stack).equals(ScanIdentity.item(stack,data)),"Scan identity differs: "+stack);
            }
            if(at[0]==stacks.size()){tc4revival.TC4Revival.LOGGER.info("CLIENT_ASPECTS PASS: {} item states match server",stacks.size());t.succeed();}
        });
    }
    @GameTest(template="wand_test_empty",timeoutTicks=300)
    public static void clientAspectDeltaOverrideDoesNotMutateServerData(GameTestHelper t){
        var baseline=ServerAspectData.snapshot();var files=new TreeMap<>(baseline.files());
        var rule=new ItemAspectRule(List.of("minecraft:ender_pearl"),null,List.of(new ItemAspectRule.Quantity("aer",19)),false);
        files.put("item_aspects/zz_sync:override",ItemAspectJson.encode(List.of(rule)).toString());
        var changed=new AspectDataSet(files);var missing=AspectDataSet.missing(changed.hashes(),baseline.files());
        t.assertTrue(missing.equals(List.of("item_aspects/zz_sync:override")),"Only server addition is transferred");
        var merged=AspectDataSet.merge(changed.hashes(),baseline.files(),changed.select(missing));
        var stack=new ItemStack(Items.ENDER_PEARL);var before=ItemStackAspects.resolve(stack).orElseThrow();
        var oldTable=new ItemAspectCache(isolated(baseline));var oldResult=oldTable.resolve(stack);
        var newTable=new ItemAspectCache(isolated(merged));var local=newTable.resolve(stack).amounts();
        t.assertTrue(local.view().equals(before.copy().merge(TCAspects.catalog().require("aer"),19).view()),"Additional base definition merges by maximum in the isolated client evaluator");
        t.assertTrue(before.view().equals(ItemStackAspects.resolve(stack).orElseThrow().view()),"Client snapshot cannot change integrated server");
        t.assertTrue(oldTable.resolve(stack).amounts().view().equals(oldResult.amounts().view()),"Reload replaces a generation rather than mutating cached results");
        t.assertTrue(newTable.size()==1,"Only queried item is initially added to new table");
        var restored=AspectDataSet.merge(baseline.hashes(),merged.files(),Map.of());
        t.assertTrue(restored.files().equals(baseline.files()),"Removed server-only file does not survive reload");
        var knowledge=new PlayerKnowledge(TCAspects.catalog());String identity=ScanIdentity.item(stack,isolated(merged));
        t.assertTrue(InventoryAspectDisplay.present(identity,local,knowledge).isEmpty(),"Local calculation preserves unscanned display gate");
        knowledge.rememberScan(PlayerKnowledge.ScanKind.OBJECT,PlayerKnowledge.ScanMode.THAUMOMETER.key(identity));
        var concealed=new AspectAmounts().add(TCAspects.catalog().require("praecantatio"),19);
        t.assertTrue(InventoryAspectDisplay.present(identity,concealed,knowledge).equals(List.of(new InventoryAspectDisplay.Icon("",0))),"Unknown aspect identity stays hidden");
        knowledge.discover(TCAspects.catalog().require("praecantatio"));
        t.assertTrue(InventoryAspectDisplay.present(identity,concealed,knowledge).equals(List.of(new InventoryAspectDisplay.Icon("praecantatio",19))),"Known scanned aspect is displayed locally");
        tc4revival.TC4Revival.LOGGER.info("CLIENT_ASPECTS PASS: delta override, removal and side isolation");t.succeed();
    }
    @GameTest(template="wand_test_empty",timeoutTicks=300)
    public static void clientAspectFramesRoundTripEffectiveServerSnapshot(GameTestHelper t){
        var server=ServerAspectData.snapshot();var delta=ServerAspectData.inputs();var frames=AspectDataTransfer.encode(delta);var receiver=new AspectDataTransfer.Receiver();Map<String,String> received=null;
        t.assertTrue(delta.keySet().stream().noneMatch(AspectInputDelta::generated),"No generated tables are sent");
        var bundled=BundledAspectData.all();
        delta.forEach((key,value)->t.assertTrue(!value.equals(bundled.get(key)),"Unchanged bundled definitions are never sent"));
        for(int i=0;i<frames.size();i++){
            var buffer=new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
            try{
                new tc4revival.network.AspectDataFrame(12,true,i,frames.size(),frames.get(i)).encode(buffer);
                var frame=tc4revival.network.AspectDataFrame.decode(buffer);received=receiver.accept(frame.index(),frame.count(),frame.bytes()).orElse(received);
            }finally{buffer.release();}
        }
        t.assertTrue(AspectInputDelta.between(AspectInputDelta.apply(bundled,received),server.files()).isEmpty(),"Custom inputs survive network codec");
        tc4revival.TC4Revival.LOGGER.info("CLIENT_ASPECTS PASS: {} custom inputs in {} sync frames; no generated tables",delta.size()-1,frames.size());t.succeed();
    }
}
