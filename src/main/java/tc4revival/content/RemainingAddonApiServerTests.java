package tc4revival.content;

import java.util.*;
import com.google.gson.JsonParser;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.*;
import tc4revival.aspect.*;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public class RemainingAddonApiServerTests {
    private static ResourceLocation id(String name) { return ResourceLocation.parse("remaining_api_test:" + name + UUID.randomUUID().toString().replace("-", "")); }
    private static BlockPos pos(GameTestHelper test) { return test.absolutePos(new BlockPos(2,2,2)); }
    @GameTest(template="wand_test_empty")
    public static void remainingBrainJarQuantitativeTransfers(GameTestHelper test) {
        var world=test.getLevel(); var pos=pos(test); world.setBlock(pos,TCArcane.BRAIN_JAR.get().defaultBlockState(),3);
        var jar=(BrainJarBlockEntity)world.getBlockEntity(pos); jar.eatDelay=19;
        test.assertTrue(jar.insertExperience(Integer.MAX_VALUE,true)==jar.experienceCapacity() && jar.experience()==0,"Simulation bounded and side effect free");
        test.assertTrue(jar.insertExperience(Integer.MAX_VALUE,false)==2000 && jar.comparator()==15,"Insert clamps to capacity and comparator reflects amount");
        test.assertTrue(jar.insertExperience(1,false)==0 && jar.extractExperience(1999,true)==1999 && jar.experience()==2000,"Full jar rejects input and simulated extract preserves state");
        test.assertTrue(jar.extractExperience(1999,false)==1999 && jar.experience()==1 && jar.eatDelay==19,"Can retain exactly one point without orb/cooldown effects");
        var loaded=new BrainJarBlockEntity(pos,jar.getBlockState()); loaded.load(jar.saveWithoutMetadata());
        test.assertTrue(loaded.experience()==1 && loaded.insertExperience(1,false)==0,"XP persists, detached jar rejects mutations");
        world.removeBlock(pos,false); test.assertTrue(jar.extractExperience(1,false)==0,"Stale jar rejects mutations"); test.succeed();
    }
    @GameTest(template="wand_test_empty",timeoutTicks=100)
    public static void remainingMirrorTargetQueue(GameTestHelper test) {
        var world=test.getLevel();var pos=pos(test);world.setBlock(pos,TCArcane.ITEM_MIRROR.get().defaultBlockState(),3);
        var mirror=(ItemMirrorBlockEntity)world.getBlockEntity(pos);var stack=new ItemStack(Items.DIAMOND,3);stack.getOrCreateTag().putString("delivery","original");
        test.assertTrue(!mirror.linked() && mirror.insertOutput(stack,true).isEmpty() && mirror.queuedItems().isEmpty(),"Unpaired target simulates without queue mutation");
        test.assertTrue(mirror.insertOutput(stack,false).isEmpty() && stack.getCount()==3,"Insertion accepts without taking input ownership");
        stack.getOrCreateTag().putString("delivery","changed");mirror.queuedItems().get(0).setCount(99);
        test.assertTrue(mirror.queuedItems().get(0).getCount()==3 && mirror.queuedItems().get(0).getTag().getString("delivery").equals("original"),"Queue copies both input and inspection output");
        var loaded=new ItemMirrorBlockEntity(pos,mirror.getBlockState());loaded.load(mirror.saveWithoutMetadata());
        test.assertTrue(loaded.queuedItems().get(0).getCount()==3 && loaded.insertOutput(stack,false).getCount()==3,"Queue persists and detached mirror rejects");
        test.runAfterDelay(40,()->{test.assertTrue(mirror.queuedItems().isEmpty(),"Original mirror tick gradually emits queued items without a partner");world.removeBlock(pos,false);test.succeed();});
    }
    @GameTest(template="wand_test_empty",timeoutTicks=60)
    public static void remainingAmplifierLeaseBudgetPersistenceAndExpiry(GameTestHelper test) {
        var world=test.getLevel();var pos=pos(test);world.setBlock(pos,TCArcane.ENERGIZED_NODE.get().defaultBlockState(),3);
        var node=(EnergizedNodeBlockEntity)world.getBlockEntity(pos);var air=TCAspects.catalog().require("aer");
        node.configure(new AspectAmounts().add(air,16),AuraNodeType.NORMAL,null,"permanent");node.tick();
        var a=UUID.randomUUID();var b=UUID.randomUUID();var bonus=new AspectAmounts().add(air,10);
        test.assertTrue(node.drain(air,4)==4,"Base source supplies four");node.amplify(a,bonus,30);node.amplify(b,bonus,5);
        test.assertTrue(node.aura().amount(air)==16 && node.effectiveAura().amount(air)==36 && node.activeAmplifierCount()==2,"Two device bonuses sum without changing baseline");
        test.assertTrue(node.drain(air,100)==2,"Increased output grants only additional budget in same tick");
        node.amplify(a,bonus,30);test.assertTrue(node.drain(air,1)==0,"Renewal cannot reset spent budget");
        var saved=node.saveWithoutMetadata();var loaded=new EnergizedNodeBlockEntity(pos,node.getBlockState());loaded.load(saved);
        test.assertTrue(loaded.aura().amount(air)==16 && loaded.supply().amount(air)==4 && loaded.activeAmplifierCount()==0,"Disk excludes temporary aura and temporary output");
        test.assertTrue(AspectNbt.read(node.getUpdateTag(),"AEB",TCAspects.catalog()).amounts().amount(air)==6,"Client packet carries effective output");
        bonus.add(air,999);test.assertTrue(node.effectiveAura().amount(air)==36,"Lease owns its input copy");
        test.runAfterDelay(7,()->{
            test.assertTrue(node.activeAmplifierCount()==1 && node.effectiveAura().amount(air)==26,"Absent device expires independently");
            test.assertTrue(node.removeAmplifier(a) && node.effectiveAura().amount(air)==16,"Explicit removal restores baseline");
            node.amplify(a,new AspectAmounts().add(air,10),100);node.onChunkUnloaded();
            test.assertTrue(node.activeAmplifierCount()==0,"Chunk unload discards leases");world.removeBlock(pos,false);test.succeed();
        });
    }
    @GameTest(template="wand_test_empty")
    public static void remainingDisposableWandRejectsSourceDebit(GameTestHelper test) {
        var world=test.getLevel();var pos=pos(test);var air=TCAspects.catalog().require("aer");
        String marker=id("disposable").toString();
        thaumcraft.api.wands.WandRechargePolicies.registerVeto(id("veto"),stack->stack.hasTag() && stack.getTag().getBoolean(marker));
        var disposable=TCItems.CASTING_WAND.get();
        var wand=disposable.create(WandParts.Rod.SILVERWOOD,WandParts.Cap.GOLD,false,true);
        wand.getOrCreateTag().putBoolean(marker,true);
        int initial=disposable.vis(wand,air);wand.getOrCreateTag().putInt("aer",initial-100);
        test.assertTrue(initial>0 && disposable.receiveVis(wand,air,50,true)==0 && disposable.addVis(wand,air,50,false,true)==50,"Initial charge is legal but all recharge rejects");
        var player=new FakePlayer(world,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"DisposableTest"));player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,wand);player.tickCount=5;
        var amulet=TCItems.LESSER_VIS_AMULET.get();var amuletStack=new ItemStack(amulet);amulet.addVis(amuletStack,air,100,false,true);
        amulet.curioTick(new top.theillusivec4.curios.api.SlotContext("charm",player,0,false,true),amuletStack);
        test.assertTrue(amulet.vis(amuletStack,air)==100 && disposable.vis(wand,air)==initial-100,"Amulet retains rejected vis");
        world.setBlock(pos,TCArcane.BLOCK.get().defaultBlockState(),3);var table=(ArcaneWorkbenchBlockEntity)world.getBlockEntity(pos);table.setItem(10,wand);
        var nodePos=pos.east(4);world.setBlock(nodePos,TCArcane.ENERGIZED_NODE.get().defaultBlockState(),3);var node=(EnergizedNodeBlockEntity)world.getBlockEntity(nodePos);node.configure(new AspectAmounts().add(air,100),AuraNodeType.NORMAL,null,"charge-source");node.tick();
        world.setBlock(pos.above(),TCArcane.WORKBENCH_CHARGER.get().defaultBlockState(),3);var charger=(WorkbenchChargerBlockEntity)world.getBlockEntity(pos.above());charger.tick();
        test.assertTrue(node.consumeVis(air,100)==10 && disposable.vis(wand,air)==initial-100,"Actual workbench charger does not drain source for rejected charge");
        table.clearContent();world.removeBlock(pos.above(),false);world.removeBlock(pos,false);world.removeBlock(nodePos,false);test.succeed();
    }
    @GameTest(template="wand_test_empty")
    public static void remainingPedestalOrbAndRegenerationRespectVeto(GameTestHelper test) {
        var world=test.getLevel();var pos=pos(test);var air=TCAspects.catalog().require("aer");String marker=id("portable_veto").toString();
        thaumcraft.api.wands.WandRechargePolicies.registerVeto(id("portable_policy"),stack->stack.hasTag() && stack.getTag().getBoolean(marker));
        var item=TCItems.CASTING_WAND.get();var wand=item.create(WandParts.Rod.REED,WandParts.Cap.GOLD,false,false);wand.getOrCreateTag().putBoolean(marker,true);
        world.setBlock(pos,TCArcane.WAND_PEDESTAL.get().defaultBlockState(),3);var pedestal=(WandPedestalBlockEntity)world.getBlockEntity(pos);pedestal.setItem(0,wand);
        var nodePos=pos.east(3);world.setBlock(nodePos,TCArcane.AURA_NODE.get().defaultBlockState(),3);var node=(AuraNodeBlockEntity)world.getBlockEntity(nodePos);
        node.configure(new AspectAmounts().add(air,10),AuraNodeType.NORMAL,null,"pedestal-policy",false);
        try {
            for(int i=0;i<5;i++)pedestal.tick();
            test.assertTrue(node.aspects().amount(air)==10 && item.vis(wand,air)==0,"Pedestal leaves node untouched for vetoed wand");
            wand.getOrCreateTag().putBoolean(marker,false);for(int i=0;i<5;i++)pedestal.tick();
            test.assertTrue(node.aspects().amount(air)==9 && item.vis(wand,air)==100,"Same pedestal/node charges normally once veto is removed");
            pedestal.removeItemNoUpdate(0);wand.getOrCreateTag().putBoolean(marker,true);wand.getOrCreateTag().putInt("aer",0);
            var player=new FakePlayer(world,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"PortableCharge"));player.getInventory().setItem(0,wand);player.getInventory().selected=0;player.tickCount=200;
            item.inventoryTick(wand,world,player,0,true);test.assertTrue(item.vis(wand,air)==0,"Natural regeneration respects per-stack veto");
            var orb=new AspectOrbEntity(world,pos.getX(),pos.getY(),pos.getZ(),air,1);orb.playerTouch(player);
            test.assertTrue(!orb.isRemoved() && item.vis(wand,air)==0,"Rejected orb remains available");
            wand.getOrCreateTag().putBoolean(marker,false);orb.playerTouch(player);
            test.assertTrue(orb.isRemoved() && item.vis(wand,air)==100,"Orb collection resumes for rechargeable stack");test.succeed();
        } finally {pedestal.clearContent();world.removeBlock(pos,false);world.removeBlock(nodePos,false);}
    }
    @GameTest(template="wand_test_empty")
    public static void remainingPechFixedValuePriorityAndFallback(GameTestHelper test) {
        var item=new ItemStack(Items.STRUCTURE_VOID);
        thaumcraft.api.entities.PechTradeValues.register(id("low"),1,Items.STRUCTURE_VOID,3);
        thaumcraft.api.entities.PechTradeValues.register(id("high"),2,net.minecraft.world.item.crafting.Ingredient.of(Items.STRUCTURE_VOID),7);
        test.assertTrue(PechEntity.fixedValue(item)==7 && PechEntity.value(item)==7,"Higher-priority registered fixed value drives both resolvers");
        var pech=TCEntities.PECH.get().create(test.getLevel());test.assertTrue(pech.canPickup(item),"Taming pickup sees fixed custom value");
        test.assertTrue(PechEntity.fixedValue(new ItemStack(Items.DIAMOND))==4 && PechEntity.fixedValue(ItemStack.EMPTY)==0,"Native fixed values and empty-stack fallback preserved");test.succeed();
    }
    @GameTest(template="wand_test_empty")
    public static void remainingArcaneDynamicOutputTransactions(GameTestHelper test) {
        var world=test.getLevel();var manager=world.getRecipeManager();var original=new ArrayList<net.minecraft.world.item.crafting.Recipe<?>>(manager.getRecipes());
        var transform=id("copy");thaumcraft.api.crafting.ArcaneResultTransforms.register(transform,(inputs,w,h,registries,base,config)->{
            base.setTag(inputs.stream().filter(s->!s.isEmpty()).findFirst().orElseThrow().getOrCreateTag().copy());return base;
        });
        var json=JsonParser.parseString("{\"mode\":\"shapeless\",\"research\":\"FOCUSPOUCH\",\"ingredients\":[{\"item\":\"minecraft:structure_void\"}],\"result\":{\"item\":\"minecraft:paper\",\"count\":2},\"vis\":{\"aer\":1},\"result_transform\":{\"type\":\""+transform+"\"}}").getAsJsonObject();
        var recipe=new ArcaneRecipe(id("recipe"),json);var recipes=new ArrayList<>(original);recipes.add(recipe);manager.replaceRecipes(recipes);
        var pos=pos(test);world.setBlock(pos,TCArcane.BLOCK.get().defaultBlockState(),3);var table=(ArcaneWorkbenchBlockEntity)world.getBlockEntity(pos);
        var player=new FakePlayer(world,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"DynamicRecipe"));player.moveTo(pos.getCenter());player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        var wand=TCItems.CASTING_WAND.get().create(WandParts.Rod.WOOD,WandParts.Cap.GOLD,false,true);table.setItem(10,wand);int initial=TCItems.CASTING_WAND.get().vis(wand,TCAspects.catalog().require("aer"));
        var input=new ItemStack(Items.STRUCTURE_VOID);input.getOrCreateTag().putString("filter","keep this configuration");table.setItem(0,input.copy());
        var menu=new ArcaneWorkbenchMenu(1,player.getInventory(),table);player.containerMenu=menu;
        try {
            test.assertTrue(menu.getSlot(0).getItem().isEmpty(),"Dynamic recipe retains research lock");
            tc4revival.research.KnowledgeSavedData.get(player.server).update(player.getUUID(),bound->0,k->{k.rememberResearch("FOCUSPOUCH");return null;});menu.refresh();
            test.assertTrue(menu.preview().getTag().equals(input.getTag()) && menu.getSlot(0).getItem().getCount()==2,"Preview includes dynamic NBT before extraction");
            menu.clicked(0,0,ClickType.PICKUP,player);test.assertTrue(menu.getCarried().getTag().equals(input.getTag()),"Normal pickup preserves NBT");menu.setCarried(ItemStack.EMPTY);
            table.setItem(0,input.copy());menu.refresh();menu.clicked(0,0,ClickType.QUICK_MOVE,player);
            var crafted=player.getInventory().items.stream().filter(s->s.is(Items.PAPER)).findFirst().orElseThrow();
            test.assertTrue(crafted.getCount()==2 && crafted.getTag().equals(input.getTag()),"Shift output is transformed before insertion in inventory");
            test.assertTrue(initial-TCItems.CASTING_WAND.get().vis(wand,TCAspects.catalog().require("aer"))==200,"Both transactions debit exact vis");
            var buffer=new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
            try {new ArcaneRecipe.Serializer().toNetwork(buffer,recipe);var decoded=new ArcaneRecipe.Serializer().fromNetwork(recipe.getId(),buffer);test.assertTrue(decoded.getId().equals(recipe.getId()),"Custom transform JSON survives recipe synchronization");} finally {buffer.release();}
            test.succeed();
        } finally {player.containerMenu=player.inventoryMenu;table.clearContent();world.removeBlock(pos,false);manager.replaceRecipes(original);}
    }
}
