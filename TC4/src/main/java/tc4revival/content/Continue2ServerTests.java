package tc4revival.content;

import java.util.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.*;
import tc4revival.aspect.*;
import tc4revival.aspect.item.ItemStackAspects;
import tc4revival.research.*;
import tc4revival.network.*;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public class Continue2ServerTests {
    private static FakePlayer player(GameTestHelper t,String name) {
        var p=new FakePlayer(t.getLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),name));
        KnowledgeSavedData.get(p.server).ensure(p.getUUID(),bound->0);
        return p;
    }
    private static void passed(GameTestHelper t,String name){tc4revival.TC4Revival.LOGGER.info("Continue2 PASS: {}",name);t.succeed();}

    @GameTest(template="wand_test_empty",timeoutTicks=90)
    public static void inventoryHoverScansEmptyItemsAfterLegacyDuration(GameTestHelper t) {
        var p=player(t,"InventoryEmpty");var stack=AspectFreeTestItems.create();
        t.assertTrue(ItemStackAspects.resolve(stack).orElseGet(AspectAmounts::new).isEmpty(),"Modern empty item fixture");
        p.getInventory().setItem(0,stack);p.inventoryMenu.setCarried(new ItemStack(TCItems.THAUMOMETER.get()));
        var data=KnowledgeSavedData.get(p.server);String key=ScanIdentity.item(stack);
        t.runAtTickTime(1,()->{
            t.assertTrue(InventoryScanning.eligible(p,0,36),"Valid own inventory slot");
            t.assertTrue(!InventoryScanning.eligible(p,9,36)&&!InventoryScanning.eligible(p,0,999),"Reject forged menu and slot");
            InventoryScanning.request(p,0,36);
        });
        for(int elapsed=1;elapsed<=60;elapsed++){int tick=elapsed;
            t.runAtTickTime(tick+1,()->{
                if(tick%5==0)InventoryScanning.request(p,0,36);
                for(int repeat=0;repeat<5;repeat++)InventoryScanning.tick(p);
                boolean scanned=data.snapshot(p.getUUID()).orElseThrow().scanned(PlayerKnowledge.ScanKind.OBJECT,key);
                t.assertTrue(scanned==(tick>=InventoryScanning.DURATION),"Only the legacy 25 game-tick duration records an empty scan");
                if(tick==60){t.assertTrue(p.getInventory().getItem(0)==stack&&stack.getCount()==1,"Scanning never consumes inventory");InventoryScanning.cancel(p);passed(t,"empty item authoritative hover timer");}
            });
        }
    }

    @GameTest(template="wand_test_empty",timeoutTicks=90)
    public static void inventoryHoverCancelsOnMutationAndScansSelf(GameTestHelper t) {
        var p=player(t,"InventorySelf");var other=player(t,"InventoryCancel");
        for(var player:List.of(p,other)){
            player.inventoryMenu.setCarried(new ItemStack(TCItems.THAUMOMETER.get()));
            KnowledgeSavedData.get(player.server).update(player.getUUID(),bound->0,k->{TCAspects.catalog().all().forEach(k::discover);return null;});
        }
        var entity=ScanTargetResolver.entity(p).orElseThrow();
        other.getInventory().setItem(0,new ItemStack(Items.GLASS));
        t.runAtTickTime(1,()->{InventoryScanning.request(p,0,InventoryScanning.SELF);InventoryScanning.request(other,0,36);});
        for(int elapsed=1;elapsed<=55;elapsed++){int tick=elapsed;
            t.runAtTickTime(tick+1,()->{
                if(tick%5==0){InventoryScanning.request(p,0,InventoryScanning.SELF);if(tick<20)InventoryScanning.request(other,0,36);}
                if(tick==20)other.getInventory().getItem(0).getOrCreateTag().putInt("changed",1);
                InventoryScanning.tick(p);InventoryScanning.tick(other);
                if(tick==55){
                    var data=KnowledgeSavedData.get(p.server);
                    t.assertTrue(data.snapshot(p.getUUID()).orElseThrow().scanned(entity.kind(),entity.identity().orElseThrow()),"Self scans real player entity");
                    t.assertTrue(!data.snapshot(other.getUUID()).orElseThrow().scanned(PlayerKnowledge.ScanKind.OBJECT,ScanIdentity.item(new ItemStack(Items.GLASS))),"Changed NBT cancels reward");
                    other.inventoryMenu.setCarried(ItemStack.EMPTY);t.assertTrue(!InventoryScanning.eligible(other,0,36),"Removing cursor tool prevents requests");
                    InventoryScanning.cancel(p);InventoryScanning.cancel(other);passed(t,"self scan and slot mutation cancellation");
                }
            });
        }
    }

    @GameTest(template="wand_test_empty")
    public static void browserItemsRespectKnowledgeAndNbt(GameTestHelper t){
        var knowledge=new PlayerKnowledge(TCAspects.catalog());var sword=new ItemStack(Items.IRON_SWORD);
        var unscanned=BrowserRecipes.item(sword,knowledge,false).orElseThrow();
        t.assertTrue(unscanned.aspectOutputs().equals(BrowserRecipe.amounts(ItemStackAspects.resolve(sword).orElseThrow())),"Unscanned items expose complete JEI aspects");
        knowledge.rememberScan(PlayerKnowledge.ScanKind.OBJECT,"@"+ScanIdentity.item(sword));
        var partial=BrowserRecipes.item(sword,knowledge,false).orElseThrow();
        t.assertTrue(!partial.dynamic()&&partial.aspectOutputs().equals(unscanned.aspectOutputs()),"Scanning does not change JEI visibility");
        TCAspects.catalog().all().forEach(knowledge::discover);
        var plain=BrowserRecipes.item(sword,knowledge,false).orElseThrow();
        t.assertTrue(plain.aspectOutputs().equals(BrowserRecipe.amounts(ItemStackAspects.resolve(sword).orElseThrow())),"Exact resolved amounts");
        sword.enchant(Enchantments.SHARPNESS,3);
        var enchanted=BrowserRecipes.item(sword,knowledge,false).orElseThrow();
        t.assertTrue(!enchanted.aspectOutputs().equals(plain.aspectOutputs()),"Enchantment aspect bonuses included");
        t.assertTrue(ItemStack.isSameItemSameTags(enchanted.inputs().get(0).get(0),sword),"NBT retained in source recipe");
        t.assertTrue(BrowserRecipes.item(sword,null,false).orElseThrow().aspectOutputs().equals(enchanted.aspectOutputs()),"Missing knowledge must not gate JEI");
        passed(t,"public aspect lookup and NBT values");
    }

    @GameTest(template="wand_test_empty",timeoutTicks=2000)
    public static void browserCatalogGatesResearchAndUsesRealRecipes(GameTestHelper t){
        var p=player(t,"BrowserCatalog");p.setGameMode(GameType.SURVIVAL);
        var survival=BrowserRecipes.recipes(p);
        t.assertTrue(survival.stream().anyMatch(r->!r.research().isEmpty()),"Fresh survival can browse locked research recipes");
        p.setGameMode(GameType.CREATIVE);var cursor=BrowserRecipes.catalog(p);var all=new ArrayList<BrowserRecipe>();
        long[] timing={0,0,0};
        t.onEachTick(()->{
        long before=cursor.completedWork(),start=System.nanoTime();all.addAll(cursor.drain(4096,2_000_000L));
        long elapsed=System.nanoTime()-start;timing[0]+=elapsed;timing[1]=Math.max(timing[1],elapsed);timing[2]++;
        t.assertTrue(cursor.completedWork()-before<=4096,"Work-unit limit respected");
        if(!cursor.done())return;
        for(String type:List.of("arcane","crucible","infusion","aspect_combine"))t.assertTrue(all.stream().anyMatch(r->r.kind().equals(type)),"Creative includes "+type);
        t.assertTrue(all.size()==survival.size(),"All JEI recipe categories are independent of game mode and research");
        t.assertTrue(all.stream().anyMatch(r->r.kind().equals("arcane")&&r.output().hasTag()),"Dynamic wand NBT output");
        for(var row:all){
            if(row.kind().equals("infusion")){
                var id=net.minecraft.resources.ResourceLocation.parse(row.id().substring(0,row.id().lastIndexOf('/')));
                var actual=(InfusionRecipe)t.getLevel().getRecipeManager().byKey(id).orElseThrow();
                var center=row.inputs().get(0).get(0);
                var pedestalInputs=row.inputs().subList(1,row.inputs().size()).stream().map(options->options.get(0)).toList();
                t.assertTrue(actual.matches(new InfusionRecipe.Input(center,pedestalInputs,key->true),t.getLevel()),"Displayed infusion ingredients match the actual recipe: "+row.id());
                // Compare the matched preview; real crafting may intentionally sample random costs/NBT.
                var plan=actual.plan(center);
                t.assertTrue(row.aspectInputs().equals(BrowserRecipe.amounts(plan.aspects()))&&row.xp()==plan.xp(),"Infusion display agrees with server costs");
                var expectedOutput=plan.output().apply(center);
                t.assertTrue(ItemStack.matches(row.output(),expectedOutput),"Infusion output agrees with server NBT: "+row.id()+" center="+center.save(new net.minecraft.nbt.CompoundTag())+" displayed="+row.output().save(new net.minecraft.nbt.CompoundTag())+" expected="+expectedOutput.save(new net.minecraft.nbt.CompoundTag()));
            }
            if(row.kind().equals("aspect_combine"))t.assertTrue(row.aspectInputs().values().stream().mapToInt(Integer::intValue).sum()==2,"Compound consumes two components");
        }
        tc4revival.TC4Revival.LOGGER.info("Continue2 browser catalog: {} recipes, {} slices, {} ms generation total, {} ms max slice",all.size(),timing[2],timing[0]/1_000_000,timing[1]/1_000_000);
        passed(t,"server catalog research gates and actual recipe plans");
        });
    }

    @GameTest(template="wand_test_empty")
    public static void browserPacketsPreserveNbtAndRejectOversize(GameTestHelper t){
        var stack=new ItemStack(Items.DIAMOND_SWORD);stack.enchant(Enchantments.SHARPNESS,2);
        var b=new FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        try {
            new BrowserRecipesRequest(17,false,List.of(stack)).encode(b);var request=BrowserRecipesRequest.decode(b);
            t.assertTrue(request.request()==17&&!request.catalog()&&ItemStack.matches(stack,request.stacks().get(0)),"Request roundtrip");
            b.clear();var row=BrowserRecipes.item(stack,null,true).orElseThrow();new BrowserRecipesPacket(17,false,true,List.of(row)).encode(b);
            var reply=BrowserRecipesPacket.decode(b);var copy=reply.recipes().get(0);
            t.assertTrue(reply.last()&&copy.aspectOutputs().equals(row.aspectOutputs())&&ItemStack.matches(copy.inputs().get(0).get(0),stack),"Reply amount and NBT roundtrip");
            b.clear();b.writeVarInt(1);b.writeBoolean(false);b.writeVarInt(33);boolean rejected=false;
            try{BrowserRecipesRequest.decode(b);}catch(IllegalArgumentException e){rejected=true;}
            t.assertTrue(rejected,"Oversize client query rejected");
        }finally{b.release();}passed(t,"browser packet codecs and bounds");
    }
}
