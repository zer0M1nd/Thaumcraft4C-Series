package tc4verification;
import tc4revival.content.*;

import java.util.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.*;
import thaumcraft.api.wands.*;
import thaumcraft.api.crafting.*;
import thaumcraft.api.devices.*;
import static tc4verification.Checks.*;

/** Isolated migration contracts, executed in the transformed Forge runtime. */
@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public final class ApiMigrationServerTests {
    private static FakePlayer player(GameTestHelper t){
        var p=new FakePlayer(t.getLevel(),new GameProfile(UUID.randomUUID(),"ApiMigration"));
        p.setGameMode(GameType.SURVIVAL);return p;
    }
    @GameTest(template="wand_test_empty") public static void apiCastNestedExceptions(GameTestHelper t){new WandCastingChecks().nestedFailureRestoresOuterContextAndThenClearsIt();t.succeed();}
    @GameTest(template="wand_test_empty") public static void apiCastCosts(GameTestHelper t){new WandCastingChecks().costsStayLinearIndependentOfEffectsAndDoNotScaleTwice();t.succeed();}
    @GameTest(template="wand_test_empty") public static void apiCastPreview(GameTestHelper t){new WandCastingChecks().previewAndStaleHandleNeverWrite();t.succeed();}
    @GameTest(template="wand_test_empty") public static void apiCastPriorityAndBounds(GameTestHelper t){new WandCastingChecks().explicitPrioritySelectsOneEffectAndBoundsProtectTheHost();t.succeed();}

    private static boolean denyPayment,workbenchRules;
    private static int committedPayments;
    private static void workbenchRules(){
        if(workbenchRules)return;workbenchRules=true;
        MagicOperationRules.register("api_verification:payment",10000,c->{
            if(c.kind()!=MagicOperationRules.Kind.ARCANE||!c.wand().hasTag()||!c.wand().getTag().getBoolean("ApiPayment"))return MagicOperationRules.Verdict.pass();
            if(!c.simulate()){committedPayments++;if(denyPayment)return MagicOperationRules.Verdict.deny(null);}
            return MagicOperationRules.Verdict.pass();
        });
    }
    private static ArcaneWorkbenchBlockEntity workbench(GameTestHelper t,FakePlayer p){
        var pos=t.absolutePos(new BlockPos(2,2,2));t.getLevel().setBlock(pos,TCArcane.BLOCK.get().defaultBlockState(),3);
        p.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+.5);
        tc4revival.research.KnowledgeSavedData.get(p.server).update(p.getUUID(),b->0,k->{k.rememberResearch("ADVALCHEMYFURNACE");return null;});
        var table=(ArcaneWorkbenchBlockEntity)t.getLevel().getBlockEntity(pos);
        var ingot=net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(ResourceLocation.parse("thaumcraft:void_ingot"));
        for(int i=0;i<9;i++)table.setItem(i,new ItemStack(i==4?TCItems.PRIMORDIAL_PEARL.get():i%2==0?ingot:TCItems.ALCHEMICAL_CONSTRUCT.get()));
        var wand=TCItems.CASTING_WAND.get().create(WandParts.Rod.SILVERWOOD,WandParts.Cap.THAUMIUM,false,true);
        wand.getOrCreateTag().putBoolean("ApiPayment",true);table.setItem(10,wand);return table;
    }
    @GameTest(template="wand_test_empty") public static void apiWorkbenchActualDenialAndNormalTake(GameTestHelper t){
        workbenchRules();var p=player(t);var table=workbench(t,p);var menu=new ArcaneWorkbenchMenu(1,p.getInventory(),table);p.containerMenu=menu;
        var before=table.saveWithoutMetadata();committedPayments=0;denyPayment=true;
        try{
            assertTrue(!menu.getSlot(0).getItem().isEmpty(),"Simulation must approve the test recipe");
            menu.clicked(0,0,ClickType.PICKUP,p);
            assertTrue(menu.getCarried().isEmpty(),"Actual veto must not put an output on cursor");
            assertEquals(before,table.saveWithoutMetadata());assertEquals(1,committedPayments);
            denyPayment=false;menu.clicked(0,0,ClickType.PICKUP,p);
            assertTrue(menu.getCarried().is(TCItems.ADVANCED_ALCHEMICAL_CONSTRUCT.get()),"Authorized output delivered");
            assertEquals(4,menu.getCarried().getCount());assertEquals(2,committedPayments);
            assertTrue(table.getItem(0).isEmpty()&&!table.getItem(4).isEmpty(),"Ingredients debited and catalyst retained");
        }finally{denyPayment=false;p.containerMenu=p.inventoryMenu;}
        t.succeed();
    }
    @GameTest(template="wand_test_empty") public static void apiWorkbenchShiftDenialAndFullInventory(GameTestHelper t){
        workbenchRules();var p=player(t);var table=workbench(t,p);var menu=new ArcaneWorkbenchMenu(1,p.getInventory(),table);p.containerMenu=menu;
        var before=table.saveWithoutMetadata();committedPayments=0;denyPayment=true;
        try{
            menu.quickMoveStack(p,0);assertEquals(before,table.saveWithoutMetadata());
            assertEquals(0,p.getInventory().countItem(TCItems.ADVANCED_ALCHEMICAL_CONSTRUCT.get()));assertEquals(1,committedPayments);
            for(int i=0;i<36;i++)p.getInventory().setItem(i,new ItemStack(Items.STONE,64));
            denyPayment=false;menu.quickMoveStack(p,0);assertEquals(before,table.saveWithoutMetadata());assertEquals(1,committedPayments);
            p.getInventory().setItem(35,ItemStack.EMPTY);menu.quickMoveStack(p,0);
            assertEquals(4,p.getInventory().countItem(TCItems.ADVANCED_ALCHEMICAL_CONSTRUCT.get()));assertEquals(2,committedPayments);
        }finally{denyPayment=false;p.containerMenu=p.inventoryMenu;}
        t.succeed();
    }
    private static final class Storage implements FocusStorageRegistry.Storage {
        final List<ItemStack> values=new ArrayList<>(List.of(ItemStack.EMPTY,ItemStack.EMPTY));
        int preferred=1,provenance;boolean failWrite;
        public int slots(){return values.size();}
        public ItemStack get(int i){return values.get(i).copy();}
        public void set(int i,ItemStack v){values.set(i,v.copy());provenance++;if(failWrite){failWrite=false;throw new IllegalStateException("Injected partial storage write");}}
        public int preferredReturnSlot(ItemStack v){return preferred;}
        public CompoundTag checkpoint(){var n=new CompoundTag();n.putInt("provenance",provenance);return n;}
        public void restoreCheckpoint(CompoundTag n){provenance=n.getInt("provenance");}
    }
    @GameTest(template="wand_test_empty") public static void apiFocusStoragePartialWriteRollback(GameTestHelper t){
        var p=player(t);var focus=new ItemStack(TCItems.EXCAVATION_FOCUS.get());focus.getOrCreateTag().putString("marker","original");
        p.getInventory().setItem(9,focus.copy());var s=new Storage();s.values.set(0,focus.copy());s.failWrite=true;
        assertThrows(IllegalStateException.class,()->{
            try(var tx=new FocusStorageTransactions(p.getInventory(),List.of(s))){p.getInventory().setItem(9,ItemStack.EMPTY);s.set(0,ItemStack.EMPTY);tx.commit();}
        });
        assertTrue(ItemStack.matches(focus,p.getInventory().getItem(9))&&ItemStack.matches(focus,s.get(0)),"Rollback conserves inventory and external source");
        assertEquals(0,s.provenance);
        assertTrue(s.insert(1,focus,true)&&s.get(1).isEmpty()&&s.provenance==0,"Simulation must be pure");
        try(var tx=new FocusStorageTransactions(p.getInventory(),List.of(s))){assertTrue(FocusStorageTransactions.returnFocus(List.of(s),focus),"Preferred return succeeds");tx.commit();}
        assertTrue(ItemStack.matches(focus,s.get(1)),"Original preferred slot wins");t.succeed();
    }
    private static boolean infusionRules,transformEnabled,rejectParticipant;
    private static int transforms,completions,resource;
    private static void infusionRules(){
        if(infusionRules)return;infusionRules=true;
        InfusionLifecycle.registerResultTransform("api_verification:freeze",10000,"1",stack->{if(transformEnabled){transforms++;stack.getOrCreateTag().putInt("Roll",transforms);}return stack;});
        InfusionLifecycle.registerCommitParticipant("api_verification:resource",10000,c->{
            if(!c.playerName().equals("ApiMigration"))return Optional.empty();int before=resource;
            return Optional.of(new InfusionCommit.Operation(){public boolean validate(){return true;}public boolean commit(){resource--;return !rejectParticipant;}public void abort(){resource=before;}});
        });
        InfusionLifecycle.observeCompleted("api_verification:once",10000,c->{if(c.playerName().equals("ApiMigration"))completions++;});
    }
    @GameTest(template="wand_test_empty") public static void apiInfusionFrozenOutputReloadAndCommit(GameTestHelper t){
        infusionRules();var pos=t.absolutePos(new BlockPos(2,2,2));t.getLevel().setBlock(pos,TCArcane.ARCANE_PEDESTAL.get().defaultBlockState(),3);
        var center=(ArcanePedestalBlockEntity)t.getLevel().getBlockEntity(pos);center.setItem(0,new ItemStack(Items.DIAMOND));
        transforms=0;completions=0;resource=5;transformEnabled=true;
        try{
            var intent=InfusionOutput.item(new ItemStack(Items.EMERALD)).freeze("api_verification:recipe","",center.getItem(0));
            assertEquals(1,transforms);var loaded=InfusionOutput.load(intent.save());var output=loaded.apply(center.getItem(0));
            assertTrue(ItemStack.matches(output,loaded.apply(center.getItem(0))),"Saved intent must not reroll");assertEquals(1,transforms);
            var matrix=new InfusionMatrixBlockEntity(pos.above(2),TCArcane.INFUSION_MATRIX.get().defaultBlockState());matrix.setLevel(t.getLevel());
            var context=new InfusionLifecycle.Context(matrix,center,"ApiMigration",output,null);
            rejectParticipant=true;assertTrue(!InfusionLifecycle.commit(context),"Rejected participant aborts");
            assertEquals(5,resource);assertEquals(0,completions);assertTrue(center.getItem(0).is(Items.DIAMOND),"Rejected commit preserves center");
            rejectParticipant=false;assertTrue(InfusionLifecycle.commit(context),"Ready participants commit");
            assertEquals(4,resource);assertEquals(1,completions);assertTrue(ItemStack.matches(output,center.getItem(0)),"One output delivered");
        }finally{transformEnabled=false;rejectParticipant=false;}t.succeed();
    }
    public static final class Component implements DeviceExtensions.Component {
        int value,attached,invalidated,revived;final List<DeviceExtensions.Removal> removals=new ArrayList<>();
        public void extensionAttached(){attached++;}
        public void extensionInvalidate(){invalidated++;}
        public void extensionRevive(){revived++;}
        public void extensionRemoved(DeviceExtensions.Removal r){removals.add(r);}
        public CompoundTag extensionSave(){var n=new CompoundTag();n.putInt("value",value);return n;}
        public void extensionLoad(CompoundTag n){value=n.getInt("value");}
    }
    private static final DeviceExtensions.Key<Component> KEY=new DeviceExtensions.Key<>("api_verification:component",Component.class);
    private static boolean deviceRegistered;
    @GameTest(template="wand_test_empty") public static void apiDevicePayloadVersionsAndLifecycle(GameTestHelper t){
        if(!deviceRegistered){deviceRegistered=true;DeviceExtensions.register(KEY,10000,ArcanePedestalBlockEntity.class,1,"LegacyApiTest",h->new Component());}
        var pos=t.absolutePos(new BlockPos(2,2,2));t.getLevel().setBlock(pos,TCArcane.ARCANE_PEDESTAL.get().defaultBlockState(),3);
        var host=(ArcanePedestalBlockEntity)t.getLevel().getBlockEntity(pos);var component=host.extensions().get(KEY).orElseThrow();component.value=73;
        var saved=host.saveWithoutMetadata();var unknown=new CompoundTag();unknown.putInt("Version",37);unknown.putString("opaque","keep");saved.getCompound("DeviceExtensions").put("missing:component",unknown);
        host.load(saved);assertEquals(73,component.value);assertEquals(unknown,host.saveWithoutMetadata().getCompound("DeviceExtensions").getCompound("missing:component"));
        host.onChunkUnloaded();host.onChunkUnloaded();assertEquals(List.of(DeviceExtensions.Removal.UNLOADED),component.removals);
        assertTrue(host.extensions().get(KEY).isEmpty(),"Unloaded host cannot supply live component");host.reviveCaps();host.reviveCaps();
        assertEquals(1,component.revived);assertEquals(1,component.invalidated);assertEquals(73,host.extensions().get(KEY).orElseThrow().value);
        var future=host.saveWithoutMetadata();future.getCompound("DeviceExtensions").getCompound(KEY.id()).putInt("Version",100);
        host.load(future);assertTrue(host.extensions().get(KEY).isEmpty(),"Unknown component version stays opaque");
        assertEquals(100,host.saveWithoutMetadata().getCompound("DeviceExtensions").getCompound(KEY.id()).getInt("Version"));t.succeed();
    }
    @GameTest(template="wand_test_empty") public static void apiCrucibleOldCapabilityStaysInvalid(GameTestHelper t){
        assertTrue(net.minecraftforge.fml.ModList.get().isLoaded("create"),"Combined test requires Create");
        var pos=t.absolutePos(new BlockPos(2,2,2));t.getLevel().setBlock(pos,TCArcane.CRUCIBLE.get().defaultBlockState(),3);
        var host=(CrucibleBlockEntity)t.getLevel().getBlockEntity(pos);var old=host.getCapability(ForgeCapabilities.ITEM_HANDLER,null).resolve().orElseThrow();
        var input=new ItemStack(Items.COBBLESTONE,2);assertEquals(1,old.insertItem(0,input,true).getCount());assertTrue(old.getStackInSlot(0).isEmpty(),"Simulation leaves buffer empty");
        host.invalidateCaps();assertEquals(2,old.insertItem(0,input,false).getCount());host.reviveCaps();
        assertEquals(2,old.insertItem(0,input,false).getCount());var fresh=host.getCapability(ForgeCapabilities.ITEM_HANDLER,null).resolve().orElseThrow();
        assertTrue(fresh!=old,"Revive returns new handler");assertEquals(1,fresh.insertItem(0,input,false).getCount());
        var copy=fresh.getStackInSlot(0);copy.setCount(0);assertEquals(1,fresh.getStackInSlot(0).getCount());
        var saved=host.saveWithoutMetadata();host.load(saved);assertEquals(1,fresh.getStackInSlot(0).getCount());
        host.onChunkUnloaded();assertEquals(2,fresh.insertItem(0,input,false).getCount());t.succeed();
    }
    private static boolean modeRegistered,modeEnabled=true;
    private static final List<WandCasting.CancelReason> ended=new ArrayList<>();
    @GameTest(template="wand_test_empty") public static void apiUseModeCancellationLifecycle(GameTestHelper t){
        if(!modeRegistered){modeRegistered=true;WandCasting.registerUseMode("api_verification:mode",10000,new WandCasting.UseMode(){
            public boolean matches(WandCasting.Request r){return r.wand().hasTag()&&r.wand().getTag().getBoolean("ApiMode");}
            public boolean enabled(WandCasting.Request r){return modeEnabled;}
            public void finish(WandCasting.Request r,WandCasting.CancelReason reason){ended.add(reason);r.wand().getOrCreateTag().remove("ApiMode");}
        });}
        var p=player(t);ended.clear();
        for(var reason:List.of(WandCasting.CancelReason.REPLACED,WandCasting.CancelReason.STOPPED,WandCasting.CancelReason.DISABLED,WandCasting.CancelReason.DEATH,WandCasting.CancelReason.DISCONNECT,WandCasting.CancelReason.DIMENSION)){
            var wand=new ItemStack(TCItems.CASTING_WAND.get());wand.getOrCreateTag().putBoolean("ApiMode",true);p.setItemInHand(InteractionHand.MAIN_HAND,wand);
            assertTrue(WandCasting.beginUseMode(p,wand,InteractionHand.MAIN_HAND),"Mode selected");int before=ended.size();
            if(reason==WandCasting.CancelReason.STOPPED){WandCasting.releaseMode(p,wand);}
            else if(reason==WandCasting.CancelReason.DISABLED){modeEnabled=false;WandCasting.holdMode(p,wand,10);modeEnabled=true;}
            else if(reason==WandCasting.CancelReason.REPLACED){p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);WandCasting.tick(new net.minecraftforge.event.TickEvent.PlayerTickEvent(net.minecraftforge.event.TickEvent.Phase.END,p));}
            else if(reason==WandCasting.CancelReason.DEATH)WandCasting.death(new net.minecraftforge.event.entity.living.LivingDeathEvent(p,p.damageSources().generic()));
            else if(reason==WandCasting.CancelReason.DISCONNECT)WandCasting.logout(new net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent(p));
            else WandCasting.dimension(new net.minecraftforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent(p,net.minecraft.world.level.Level.OVERWORLD,net.minecraft.world.level.Level.NETHER));
            assertEquals(before+1,ended.size());assertEquals(reason,ended.get(before));WandCasting.cancel(p,reason);assertEquals(before+1,ended.size());
        }t.succeed();
    }
    private static boolean captureRegistered;
    @GameTest(template="wand_test_empty") public static void apiNodeCaptureExtensionNbtAndRollback(GameTestHelper t){
        if(!captureRegistered){captureRegistered=true;thaumcraft.api.nodes.NodeCaptureExtensions.register("api_verification:capture",10000,c->{
            if(!c.saved().getCompound("ForgeData").getBoolean("ApiCapture"))return Optional.empty();
            var extensions=new CompoundTag();extensions.put("ForgeData",c.saved().getCompound("ForgeData").copy());
            return Optional.of(new thaumcraft.api.nodes.NodeCaptureExtensions.Capture(TCArcane.NODE_JAR.get().defaultBlockState(),extensions));
        });}
        var origin=t.absolutePos(new BlockPos(2,2,2));var center=origin.offset(1,0,1);var level=t.getLevel();
        for(int row=0;row<4;row++)for(int x=0;x<3;x++)for(int z=0;z<3;z++)level.setBlock(origin.offset(x,2-row,z),Blocks.GLASS.defaultBlockState(),18);
        level.setBlock(center,TCArcane.AURA_NODE.get().defaultBlockState(),18);var node=(AuraNodeBlockEntity)level.getBlockEntity(center);
        node.configure(new tc4revival.aspect.AspectAmounts().add(tc4revival.aspect.TCAspects.catalog().require("aer"),37),AuraNodeType.NORMAL,AuraNodeModifier.BRIGHT,"api-capture",false);
        node.getPersistentData().putBoolean("ApiCapture",true);node.getPersistentData().putInt("payload",42);
        var before=node.saveWithoutMetadata();var result=NodeJarCapture.payload(level,center);
        assertTrue(!NodeJarCapture.convertValidated(level,origin,result,()->false,(p,s)->level.setBlock(p,s,18),()->true),"Payment failure rolls back");
        assertEquals(before,level.getBlockEntity(center).saveWithoutMetadata());
        assertTrue(NodeJarCapture.convertValidated(level,origin,result,()->true,(p,s)->level.setBlock(p,s,18),()->true),"Capture with extension commits");
        var jar=(NodeJarBlockEntity)level.getBlockEntity(center);assertEquals(42,jar.getPersistentData().getInt("payload"));
        var saved=jar.saveWithoutMetadata();jar.load(saved);assertEquals(42,jar.getPersistentData().getInt("payload"));assertEquals("api-capture",jar.nodeId());t.succeed();
    }
    private static boolean dropRegistered,dropEnabled;
    private static net.minecraft.world.entity.player.Inventory dropReceiver;
    @GameTest(template="wand_test_empty") public static void apiDropInsertionFailureAndRejectedSpawnConserve(GameTestHelper t){
        if(!dropRegistered){dropRegistered=true;thaumcraft.api.aspects.AspectDropHandlers.registerHandler("api_verification:drop",10000,c->dropEnabled
            ?Optional.of(new thaumcraft.api.aspects.AspectDropHandlers.Conversion(new ItemStack(Items.AMETHYST_SHARD),2,dropReceiver,new CompoundTag())):Optional.empty());}
        var p=player(t);var level=t.getLevel();var pos=t.absolutePos(new BlockPos(3,3,3));var box=new net.minecraft.world.phys.AABB(pos).inflate(3);
        var aspect=tc4revival.aspect.TCAspects.catalog().require("aer");dropEnabled=true;
        try{
            dropReceiver=new net.minecraft.world.entity.player.Inventory(p){@Override public boolean add(ItemStack stack){setItem(0,stack.copyWithCount(1));stack.shrink(1);throw new IllegalStateException("Injected inventory failure");}};
            var orb=new AspectOrbEntity(level,pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,aspect,5);
            thaumcraft.api.aspects.AspectDropHandlers.spawn(level,null,orb);
            assertTrue(dropReceiver.getItem(0).isEmpty(),"Partial inventory insertion rolled back");
            int items=level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,box).stream().filter(e->e.getItem().is(Items.AMETHYST_SHARD)).mapToInt(e->e.getItem().getCount()).sum();
            int essence=level.getEntitiesOfClass(AspectOrbEntity.class,box).stream().mapToInt(AspectOrbEntity::value).sum();
            assertEquals(2,items);assertEquals(1,essence);
            level.getEntitiesOfClass(net.minecraft.world.entity.Entity.class,box).stream().filter(e->e instanceof AspectOrbEntity||e instanceof net.minecraft.world.entity.item.ItemEntity).forEach(net.minecraft.world.entity.Entity::discard);
            dropReceiver=new net.minecraft.world.entity.player.Inventory(p){@Override public boolean add(ItemStack stack){setItem(0,stack.copyWithCount(1));stack.shrink(1);return false;}};
            java.util.function.Consumer<net.minecraftforge.event.entity.EntityJoinLevelEvent> reject=e->{if(e.getEntity() instanceof net.minecraft.world.entity.item.ItemEntity item&&item.getItem().is(Items.AMETHYST_SHARD))e.setCanceled(true);};
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(reject);
            try{thaumcraft.api.aspects.AspectDropHandlers.spawn(level,null,new AspectOrbEntity(level,pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,aspect,5));}
            finally{net.minecraftforge.common.MinecraftForge.EVENT_BUS.unregister(reject);}
            assertEquals(1,dropReceiver.getItem(0).getCount());assertEquals(3,level.getEntitiesOfClass(AspectOrbEntity.class,box).stream().mapToInt(AspectOrbEntity::value).sum());
            assertEquals(0,level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,box).size());
        }finally{dropEnabled=false;dropReceiver=null;}t.succeed();
    }
}
