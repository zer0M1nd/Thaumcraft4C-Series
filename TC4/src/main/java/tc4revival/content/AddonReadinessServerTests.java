package tc4revival.content;

import java.util.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.*;
import tc4revival.aspect.*;
import thaumcraft.api.visnet.VisEndpoint;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public class AddonReadinessServerTests {
    private static ResourceLocation id(String name){return ResourceLocation.parse("readiness_test:"+name);}
    private static FakePlayer player(GameTestHelper test){return new FakePlayer(test.getLevel(),new GameProfile(UUID.randomUUID(),"ReadinessTest"));}
    private static boolean storageRegistered,cropRegistered,researchRegistered;
    @GameTest(template="wand_test_empty")
    public static void readinessForeignVisEndpointsShareNativeNetwork(GameTestHelper test) {
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(2,3,2));var air=TCAspects.catalog().require("aer");
        class Part implements VisEndpoint {
            final Key key;final boolean source;int remaining=80;boolean alive=true;VisEndpoint parent;
            Part(BlockPos p,String face,boolean source){key=new Key(p,id(face));this.source=source;}
            public net.minecraft.world.level.Level level(){return level;}
            public Key key(){return key;}public boolean valid(){return alive;}public int range(){return 8;}public boolean source(){return source;}
            public int drain(Aspect a,int requested){int n=Math.min(remaining,requested);remaining-=n;return n;}
            public void parentChanged(VisEndpoint value){parent=value;}
        }
        var source=new Part(pos,"north",true);var sameHost=new Part(pos,"south",false);var far=new Part(pos.east(12),"west",false);
        var relayPos=pos.east(6);level.setBlock(relayPos,TCArcane.VIS_RELAY.get().defaultBlockState(),2);
        var relay=(VisRelayBlockEntity)level.getBlockEntity(relayPos);
        try {
            VisNetwork.register(source);VisNetwork.register(sameHost);relay.tickNetwork();VisNetwork.register(far);
            test.assertTrue(sameHost.parent==source && relay.networkRoot()==source && far.parent==relay,"Face IDs coexist; native and foreign relays form one tree");
            test.assertTrue(relay.getUpdateTag().contains("Parent")&&relay.getBlockState().getValue(VisRelayBlock.CONNECTED),"Foreign parents synchronize native connection state");
            test.assertTrue(VisNetwork.drain(level,pos.east(18),air,30)==30&&source.remaining==50,"Normal consumers drain through a foreign relay and native bridge");
            source.alive=false;
            test.assertTrue(VisNetwork.consume(far,air,100)==0,"Host invalidation rejects stale source before periodic cleanup");
            VisNetwork.unregister(source);
            test.assertTrue(far.parent==null && relay.networkParent()==null && sameHost.parent==null,"Removal invalidates every descendant immediately");
            source.alive=true;VisNetwork.register(source);relay.refreshNetwork();relay.tickNetwork();VisNetwork.register(far);
            test.assertTrue(VisNetwork.consume(far,air,80)==50,"Reconnect retains source accounting");
        }finally{VisNetwork.unregister(far);VisNetwork.unregister(sameHost);VisNetwork.unregister(source);level.removeBlock(relayPos,false);}
        test.succeed();
    }
    @GameTest(template="wand_test_empty")
    public static void readinessCustomFocusStorageRoundTrip(GameTestHelper test) {
        if(!storageRegistered){
            thaumcraft.api.wands.FocusStorageRegistry.register(id("large_pouch"),stack->!stack.is(Items.BUNDLE)?null:new thaumcraft.api.wands.FocusStorageRegistry.Storage(){
                public int slots(){return 117;}
                public ItemStack get(int slot){return ItemStack.of(stack.getOrCreateTag().getCompound("focus_"+slot));}
                public void set(int slot,ItemStack value){stack.getOrCreateTag().put("focus_"+slot,value.save(new net.minecraft.nbt.CompoundTag()));}
            });storageRegistered=true;
        }
        var player=player(test);var bag=new ItemStack(Items.BUNDLE);var storage=thaumcraft.api.wands.FocusStorageRegistry.find(bag).orElseThrow();
        var fire=new ItemStack(TCItems.FIRE_FOCUS.get());storage.set(116,fire);
        var wand=TCItems.CASTING_WAND.get().create(WandParts.Rod.GREATWOOD,WandParts.Cap.GOLD,false,true);
        WandFocusItem.attach(wand,new ItemStack(TCItems.FROST_FOCUS.get()));
        player.getInventory().items.set(0,wand);player.getInventory().items.set(1,bag);player.getInventory().selected=0;
        var key=TCItems.FIRE_FOCUS.get().sortingKey(fire);
        test.assertTrue(FocusSelection.available(player).foci().containsKey(key),"Selection enumerates slot 116");
        FocusSelection.change(player,net.minecraft.world.InteractionHand.MAIN_HAND,key);
        test.assertTrue(WandFocusItem.attached(wand).is(TCItems.FIRE_FOCUS.get()) && storage.get(116).isEmpty() && storage.get(0).is(TCItems.FROST_FOCUS.get()),"Incoming focus removed exactly once and old focus returned to custom bag");
        FocusSelection.change(player,net.minecraft.world.InteractionHand.MAIN_HAND,"REMOVE");
        test.assertTrue(WandFocusItem.attached(wand).isEmpty() && storage.get(1).is(TCItems.FIRE_FOCUS.get()),"Removal returns focus through the same provider");test.succeed();
    }
    @GameTest(template="wand_test_empty")
    public static void readinessTubeVentAndHungryCreative(GameTestHelper test) {
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(2,3,2));
        level.setBlock(pos,TCArcane.ESSENTIA_TUBE.get().defaultBlockState(),2);
        var tube=(EssentiaTubeBlockEntity)level.getBlockEntity(pos);tube.vent(null);tube.vent(TCAspects.catalog().require("aer"));
        level.setBlock(pos,TCArcane.AURA_NODE.get().defaultBlockState(),2);var node=(AuraNodeBlockEntity)level.getBlockEntity(pos);
        node.configure(new AspectAmounts().add(TCAspects.catalog().require("aer"),20),AuraNodeType.HUNGRY,null,"readiness",false);
        var creative=player(test);creative.setGameMode(net.minecraft.world.level.GameType.CREATIVE);creative.setPos(pos.getX()+3.5,pos.getY()+.5,pos.getZ()+.5);
        level.addNewPlayer(creative);creative.setDeltaMovement(.2,.1,0);
        var item=new net.minecraft.world.entity.item.ItemEntity(level,pos.getX()+5.5,pos.getY()+.5,pos.getZ()+.5,new ItemStack(Items.DIRT));level.addFreshEntity(item);item.setDeltaMovement(0,0,0);
        try {
            for(int i=0;i<5;i++)node.tick();
            test.assertTrue(creative.getDeltaMovement().equals(new net.minecraft.world.phys.Vec3(.2,.1,0)),"Creative players receive no hungry-node acceleration");
            test.assertTrue(Math.abs(item.getDeltaMovement().x-(-5*.15*Math.pow(1-5d/15,2)))<1e-6,"Nonplayer pull preserves the legacy per-tick formula");
        }finally{creative.discard();item.discard();level.removeBlock(pos,false);}
        test.succeed();
    }
    @GameTest(template="wand_test_empty")
    public static void readinessCropAndModernEnchantment(GameTestHelper test) {
        if(!cropRegistered){thaumcraft.api.golems.GolemCropRegistry.register(id("crop"),10,new thaumcraft.api.golems.GolemCropRegistry.Rule(){
            public boolean matches(net.minecraft.world.level.block.state.BlockState state){return state.is(Blocks.DEAD_BUSH);}
            public boolean mature(net.minecraft.server.level.ServerLevel level,BlockPos pos,net.minecraft.world.level.block.state.BlockState state){return true;}
            public boolean harvest(thaumcraft.api.golems.GolemCropRegistry.Context context){return context.breakBlock(context.pos(),false)&&(!context.replant()||context.place(context.pos(),Blocks.DEAD_BUSH.defaultBlockState()));}
        });cropRegistered=true;}
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(2,3,2));level.setBlock(pos.below(),Blocks.SAND.defaultBlockState(),2);level.setBlock(pos,Blocks.DEAD_BUSH.defaultBlockState(),2);
        var golem=TCEntities.GOLEM.get().create(level);golem.moveTo(pos.getCenter());
        test.assertTrue(GolemCropHarvest.mature(level,pos),"Native harvest scanner recognizes a registered crop");
        GolemCropHarvest.harvest(golem,pos,level.getBlockState(pos));test.assertTrue(level.isEmptyBlock(pos),"Registered crop action is executed by the native harvester");
        var sword=new ItemStack(Items.DIAMOND_SWORD);var before=tc4revival.aspect.item.ItemStackAspects.resolve(sword).orElseThrow();
        sword.enchant(net.minecraft.world.item.enchantment.Enchantments.SWIFT_SNEAK,3);
        var after=tc4revival.aspect.item.ItemStackAspects.resolve(sword).orElseThrow();
        test.assertTrue(after.amount(TCAspects.catalog().require("praecantatio"))==Math.max(3,before.amount(TCAspects.catalog().require("praecantatio"))),"Modern enchantments contribute general magic through the common stack resolver");
        level.removeBlock(pos.below(),false);test.succeed();
    }
    @GameTest(template="wand_test_empty")
    public static void readinessBossReservationPersistsBeforeActivation(GameTestHelper test) {
        var data=new EldritchMazeSavedData();var layout=EldritchMazeLayout.generate(11,11,42L);
        test.assertTrue(data.reserve(new EldritchMazeSavedData.Reservation(200,200,11,11,42L)),"Reserve maze");data.assignBoss(200,200,3);
        var pending=EldritchMazeSavedData.load(data.save(new net.minecraft.nbt.CompoundTag()));
        test.assertTrue(pending.publish(200,200,layout),"Plan publishes after reservation reload");
        var restored=EldritchMazeSavedData.load(pending.save(new net.minecraft.nbt.CompoundTag()));
        test.assertTrue(restored.bossAt(200,200)==3,"Selected boss survives reservation, layout generation and world reload");
        var registry=test.getLevel().registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.STRUCTURE);
        for(var name:List.of("golem","warden","crimson","taint"))test.assertTrue(registry.containsKey(ResourceLocation.parse("thaumcraft:eldritch_obelisk_"+name)),"Named vanilla locate target exists: "+name);
        test.succeed();
    }
    @GameTest(template="wand_test_empty")
    public static void readinessCreativeBookAuthorization(GameTestHelper test) {
        var player=player(test);player.getInventory().items.set(0,new ItemStack(Items.BOOK));player.getInventory().selected=0;
        var data=tc4revival.research.KnowledgeSavedData.get(test.getLevel().getServer());
        var book=new tc4revival.research.ServerResearchBook(player,net.minecraft.world.InteractionHand.MAIN_HAND,s->s.is(Items.BOOK),s->false,data,()->new tc4revival.research.ServerResearchBook.Settings(0,false,false));
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        test.assertTrue(book.request(player,book.token(),0,"HUNGRYCHEST",true) instanceof tc4revival.research.ServerResearchBook.Denied,"Survival cannot forge creative intent");
        player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
        test.assertTrue(book.request(player,book.token(),1,"HUNGRYCHEST",true) instanceof tc4revival.research.ServerResearchBook.Purchase,"Creative intent uses valid book authorization");
        var knowledge=data.snapshot(player.getUUID()).orElseThrow();
        test.assertTrue(knowledge.researchMarkers().contains("HUNGRYCHEST")&&tc4revival.research.ResearchCatalogs.rules().prerequisites(knowledge,"HUNGRYCHEST"),"Completion and all prerequisites committed");
        test.assertTrue(book.request(player,book.token(),1,"ELDRITCHMINOR",true) instanceof tc4revival.research.ServerResearchBook.Denied,"Sequence replay rejected");book.close();test.succeed();
    }
    @GameTest(template="wand_test_empty")
    public static void readinessConditionalResearchSnapshot(GameTestHelper test) {
        if(!researchRegistered) {
            thaumcraft.api.research.ResearchCategories.registerEnablement("readiness:fire",()->false);
            thaumcraft.api.research.ResearchPrerequisites.conditional(id("conditional"),"readiness:crop","readiness:fire",()->false);
            thaumcraft.api.research.ResearchPrerequisites.allEnabled(id("all"),"readiness:kami",entry->entry.key().startsWith("readiness:"));
            researchRegistered=true;
        }
        var root=new com.google.gson.JsonObject();var categories=new com.google.gson.JsonArray();var category=new com.google.gson.JsonObject();
        category.addProperty("key","readiness");category.addProperty("icon","test:icon.png");category.addProperty("background","test:background.png");
        var entries=new com.google.gson.JsonArray();var pages=new com.google.gson.JsonArray();
        for(String key:List.of("fire","crop","new_research","kami","after_kami")) {
            var row=new com.google.gson.JsonObject();row.addProperty("key","readiness:"+key);row.addProperty("category","readiness");row.addProperty("displayColumn",0);row.addProperty("displayRow",0);row.addProperty("pageCount",0);
            var parents=new com.google.gson.JsonArray();if(key.equals("crop"))parents.add("readiness:fire");if(key.equals("after_kami"))parents.add("readiness:kami");row.add("parents",parents);entries.add(row);
            var page=new com.google.gson.JsonObject();page.addProperty("key","readiness:"+key);page.add("pages",new com.google.gson.JsonArray());pages.add(page);
        }
        category.add("research",entries);categories.add(category);root.add("categories",categories);root.add("pages",pages);root.add("recipes",new com.google.gson.JsonArray());
        var materialized=thaumcraft.api.research.ResearchPrerequisites.materialize(root);
        var snapshot=tc4revival.research.ResearchDefinitions.decode(materialized);
        test.assertTrue(snapshot.catalog().require("readiness:crop").parents().isEmpty(),"Disabled optional parent removed");
        var required=snapshot.catalog().require("readiness:kami").parents();
        test.assertTrue(required.containsAll(List.of("readiness:crop","readiness:new_research"))&&!required.contains("readiness:fire")&&!required.contains("readiness:after_kami"),"Dynamic gate includes newly loaded research, excludes disabled research and descendants");
        test.assertTrue(materialized.getAsJsonArray("disabledResearch").toString().contains("readiness:fire"),"Server enablement is carried in the synchronized snapshot");
        test.assertTrue(tc4revival.research.ResearchDefinitions.decode(materialized.deepCopy()).catalog().require("readiness:kami").parents().equals(required),"Decoded client snapshot preserves exact effective prerequisite list");
        test.succeed();
    }
}
