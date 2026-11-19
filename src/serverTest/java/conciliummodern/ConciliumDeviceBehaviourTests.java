package conciliummodern;

import com.mojang.authlib.GameProfile;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.gametest.*;
import tc4revival.aspect.*;
import tc4revival.content.*;

/** Real block/entity/menu integration, complementing the core resource and algorithm checks. */
@GameTestHolder("thaumicconcilium") @PrefixGameTestTemplate(false)
public final class ConciliumDeviceBehaviourTests {
    private static ServerPlayer player(GameTestHelper t,String name){var p=FakePlayerFactory.get(t.getLevel(),new GameProfile(UUID.randomUUID(),name));p.setGameMode(GameType.SURVIVAL);p.moveTo(t.absolutePos(new BlockPos(16,1,16)).getCenter());return p;}
    private static <T extends net.minecraft.world.level.block.entity.BlockEntity>T tile(GameTestHelper t,BlockPos local,Block block,Class<T> type){var pos=t.absolutePos(local);t.getLevel().setBlock(pos,block.defaultBlockState(),3);return type.cast(t.getLevel().getBlockEntity(pos));}
    private static ItemStack namedWand(String owner){var wand=new ItemStack(TCItems.CASTING_WAND.get());wand.getOrCreateTag().putString("Xylography",owner);wand.getTag().putString("fixture","preserve");return wand;}
    private static ItemStack monitor(String owner){var stack=new ItemStack(ConciliumItems.ASTRAL_MONITOR.get());stack.getOrCreateTag().put("wand",namedWand(owner).save(new CompoundTag()));return stack;}

    @GameTest(template="empty") public static void quicksilverUsesRealNearbyCrucibleAndRedstone(GameTestHelper t){
        var catalyst=tile(t,new BlockPos(10,2,10),ConciliumBlocks.QUICKSILVER_CRUCIBLE.get(),ConciliumCrucibleBlockEntity.class);catalyst.initialize(5);
        var ordinary=tile(t,new BlockPos(12,2,10),TCArcane.CRUCIBLE.get(),CrucibleBlockEntity.class);var contents=new CompoundTag();contents.putString("FluidName","water");contents.putInt("Amount",730);contents.putShort("Heat",(short)173);contents.putLong("DecayTimer",-73);contents.putBoolean("Decaying",true);
        AspectNbt.write(contents,new AspectAmounts().add(TCAspects.catalog().require("aer"),7).add(TCAspects.catalog().require("terra"),2).add(TCAspects.catalog().require("aqua"),3));ordinary.contents().load(contents);
        var power=catalyst.getBlockPos().west();t.getLevel().setBlock(power,Blocks.REDSTONE_BLOCK.defaultBlockState(),3);catalyst.tick();t.assertTrue(catalyst.amount()==5&&ordinary.contents().aspects().amount(TCAspects.catalog().require("victus"))==0,"Real redstone prevents nearby processing");
        t.getLevel().removeBlock(power,false);catalyst.tick();var after=new CompoundTag();ordinary.contents().save(after);
        t.assertTrue(catalyst.amount()==4&&ordinary.contents().aspects().amount(TCAspects.catalog().require("victus"))==1,"Actual block tick finds real nearby core crucible and spends one mercury");
        for(String key:List.of("FluidName","Amount","Heat","DecayTimer","Decaying"))t.assertTrue(Objects.equals(contents.get(key),after.get(key)),"Core crucible state preserved: "+key);
        catalyst.tick();t.assertTrue(catalyst.amount()==4,"Cooldown prevents processing every tick");t.succeed();
    }
    @GameTest(template="empty") public static void fleshBasinConsumesMobRestoresFleshAndHonorsPower(GameTestHelper t){
        var basin=tile(t,new BlockPos(16,1,16),ConciliumBlocks.FLESH_CRUCIBLE.get(),ConciliumCrucibleBlockEntity.class);var pos=basin.getBlockPos();var state=basin.getBlockState();basin.initialize(0);
        var mad=ConciliumEntities.MAD_THAUMATURGE.get().create(t.getLevel());mad.moveTo(pos.getCenter());mad.setNoAi(true);t.getLevel().addFreshEntity(mad);t.getLevel().setBlock(pos.west(),Blocks.REDSTONE_BLOCK.defaultBlockState(),3);
        state.entityInside(t.getLevel(),pos,mad);t.assertTrue(mad.isAlive()&&basin.amount()==0,"Powered flesh basin does not consume real mob");t.getLevel().removeBlock(pos.west(),false);state.entityInside(t.getLevel(),pos,mad);t.assertTrue(mad.isRemoved()&&basin.amount()==2,"Actual basin collision converts one mad thaumaturge to two flesh");
        var player=player(t,"FleshBasin");player.moveTo(pos.getCenter());var data=EtherealState.data(player);data.putInt("flesh",2);EtherealState.save(player,data);state.entityInside(t.getLevel(),pos,player);t.assertTrue(EtherealState.flesh(player)==1&&basin.amount()==1,"Actual player contact restores one flesh and consumes one");
        state.entityInside(t.getLevel(),pos,player);state.entityInside(t.getLevel(),pos,player);t.assertTrue(EtherealState.flesh(player)==0&&basin.amount()==0,"Repeated contact stops at zero without negative state");
        basin.initialize(64);state.entityInside(t.getLevel(),pos,player);t.assertTrue(basin.amount()==64,"Healthy player does not consume stored flesh");var second=ConciliumEntities.MAD_THAUMATURGE.get().create(t.getLevel());second.moveTo(pos.getCenter());state.entityInside(t.getLevel(),pos,second);t.assertTrue(second.isAlive()&&basin.amount()==64,"Full basin preserves offered creature");t.succeed();
    }
    @GameTest(template="empty") public static void astralMonitorMenuWritesThroughAndLocksCarrier(GameTestHelper t){
        var player=player(t,"MonitorMenu");var monitor=new ItemStack(ConciliumItems.ASTRAL_MONITOR.get());player.getInventory().setItem(0,monitor);player.getInventory().selected=0;player.getInventory().setItem(9,namedWand("KnownTarget"));var menu=new AstralMonitorMenu(31,player.getInventory(),0);
        t.assertTrue(!menu.quickMoveStack(player,1).isEmpty()&&player.getInventory().getItem(9).isEmpty(),"Real quick move removes source wand from inventory");t.assertTrue(AstralMonitor.stored(monitor).getTag().getString("fixture").equals("preserve"),"Carrier receives complete wand NBT immediately");
        menu.clicked(0,0,ClickType.SWAP,player);menu.clicked(28,0,ClickType.PICKUP,player);t.assertTrue(player.getInventory().getItem(0)==monitor&&menu.getCarried().isEmpty()&&!menu.getSlot(0).getItem().isEmpty(),"Number-key and pickup cannot move carrier or duplicate embedded wand");menu.removed(player);
        var reopened=new AstralMonitorMenu(32,player.getInventory(),0);t.assertTrue(reopened.getSlot(0).getItem().getTag().getString("Xylography").equals("KnownTarget"),"Close/reopen retains one embedded wand");
        var replacement=new ItemStack(ConciliumItems.ASTRAL_MONITOR.get());player.getInventory().setItem(0,replacement);t.assertTrue(!reopened.stillValid(player)&&reopened.quickMoveStack(player,0).isEmpty()&&!AstralMonitor.stored(monitor).isEmpty()&&AstralMonitor.stored(replacement).isEmpty(),"Replacing carrier invalidates old menu without copying contents to replacement");t.succeed();
    }
    @GameTest(template="empty") public static void monitorReadsRealRedstoneAndClearsMissingSources(GameTestHelper t){
        var player=player(t,"MonitorRead");var mind=tile(t,new BlockPos(14,2,14),ConciliumBlocks.RED_MIND.get(),RedPoweredMindBlockEntity.class);var pos=mind.getBlockPos();t.getLevel().setBlock(pos.west(),Blocks.REDSTONE_BLOCK.defaultBlockState(),3);mind.sample();var source=new CompoundTag();source.putLong("pos",pos.asLong());source.putString("dimension",t.getLevel().dimension().location().toString());
        t.assertTrue(mind.power()==15&&MonitorSource.power(player,source,false)==15,"Real block power enters monitor reading");t.getLevel().removeBlock(pos.west(),false);mind.sample();t.assertTrue(MonitorSource.power(player,source,false)==0,"Power removal clears reading");t.getLevel().removeBlock(pos,false);t.assertTrue(MonitorSource.power(player,source,false)==-1,"Missing mind clears stale reading");
        var monitor=monitor("OfflineMissingTarget");monitor.getOrCreateTag().putIntArray("vals",new int[]{10,10,10,9});monitor.getTag().putInt("mindPower",15);player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,monitor);player.tickCount=20;ConciliumItems.ASTRAL_MONITOR.get().inventoryTick(monitor,t.getLevel(),player,0,true);t.assertTrue(Arrays.equals(monitor.getTag().getIntArray("vals"),new int[]{-1,-1,-1,-1})&&monitor.getTag().getInt("mindPower")==-1,"Held monitor clears offline/invalid source values");
        player.setHealth(10);var sample=AstralMonitor.sample(player);t.assertTrue(sample[0]==5&&sample[3]==-1,"Health maps to ten steps and absent shield is hidden");t.succeed();
    }
    private record LithoFixture(LithographerBlockEntity device,DestabilizedCrystalBlockEntity crystal,ServerPlayer sleeper){}
    private static LithoFixture lithographer(GameTestHelper t,int amount,boolean sleep){
        var level=t.getLevel();var bed=t.absolutePos(new BlockPos(16,1,16));level.setBlock(bed.below(),Blocks.STONE.defaultBlockState(),3);level.setBlock(bed.south().below(),Blocks.STONE.defaultBlockState(),3);
        level.setBlock(bed.south(),Blocks.RED_BED.defaultBlockState().setValue(BedBlock.FACING,Direction.NORTH).setValue(BedBlock.PART,BedPart.FOOT),2);level.setBlock(bed,Blocks.RED_BED.defaultBlockState().setValue(BedBlock.FACING,Direction.NORTH).setValue(BedBlock.PART,BedPart.HEAD),2);
        var device=tile(t,new BlockPos(16,2,16),ConciliumBlocks.LITHOGRAPHER.get(),LithographerBlockEntity.class);tile(t,new BlockPos(16,3,16),ConciliumBlocks.CONDENSER.get(),VisCondenserBlockEntity.class);var crystal=tile(t,new BlockPos(16,4,16),ConciliumBlocks.CRYSTAL.get(),DestabilizedCrystalBlockEntity.class);var tag=new CompoundTag();tag.putString("aspect","sensus");tag.putInt("capacity",amount);crystal.load(tag);
        for(int i=0;i<4;i++)device.setItem(i,monitor("Source"+i));ServerPlayer player=null;
        if(sleep){player=player(t,"LithoSleep");level.addNewPlayer(player);player.startSleeping(bed);var registered=player;t.runAtTickTime(90,()->{registered.stopSleeping();registered.discard();});}
        return new LithoFixture(device,crystal,player);
    }
    @GameTest(template="empty",timeoutTicks=100) public static void lithographerSleepingProjectionConsumesExactly32AndPersists(GameTestHelper t){
        var f=lithographer(t,64,true);f.device.project();t.assertTrue(f.device.phase()==1&&f.device.timer()==20,"Projection begins with twenty-tick preparation");
        t.runAtTickTime(25,()->{t.assertTrue(Lithography.inscribed(f.sleeper)&&f.crystal.amount()==32,"Real sleeper receives inscription for exactly thirty-two sensus");var data=f.sleeper.getPersistentData().getCompound("PlayerPersisted").getCompound("thaumicconcilium:lithography");var sources=data.getList("sources",10);t.assertTrue(sources.size()==4&&sources.getCompound(2).getString("player").equals("Source2"),"Four actual monitor sources persisted");var clone=player(t,"LithoClone");Lithography.clone(new net.minecraftforge.event.entity.player.PlayerEvent.Clone(clone,f.sleeper,true));t.assertTrue(Lithography.inscribed(clone),"Death clone retains eye inscriptions");});
        t.runAtTickTime(70,()->{t.assertTrue(f.device.phase()==0&&f.crystal.amount()==32,"Sleeping continuously does not retrigger or double-charge");f.sleeper.stopSleeping();f.sleeper.discard();t.succeed();});
    }
    @GameTest(template="empty",timeoutTicks=100) public static void lithographerInsufficientSensusKeepsPriorInscription(GameTestHelper t){
        var f=lithographer(t,31,true);Lithography.inscribe(f.sleeper,List.of("Previous"));f.device.project();
        t.runAtTickTime(25,()->{var data=f.sleeper.getPersistentData().getCompound("PlayerPersisted").getCompound("thaumicconcilium:lithography");t.assertTrue(f.crystal.amount()==31&&data.getList("sources",10).getCompound(0).getString("player").equals("Previous"),"Insufficient resource leaves both crystal and old inscription intact");f.sleeper.stopSleeping();f.sleeper.discard();t.succeed();});
    }
    @GameTest(template="empty",timeoutTicks=60) public static void lithographerNoSleeperPreservesResourceAndSavedInventory(GameTestHelper t){
        var f=lithographer(t,64,false);f.device.project();var saved=f.device.saveWithoutMetadata();var restored=new LithographerBlockEntity(f.device.getBlockPos(),f.device.getBlockState());restored.load(saved);t.assertTrue(restored.phase()==1&&restored.timer()==20&&AstralMonitor.stored(restored.getItem(3)).getTag().getString("Xylography").equals("Source3"),"In-progress phase and all inventory survive block save");
        t.runAtTickTime(25,()->{t.assertTrue(f.crystal.amount()==64,"Unoccupied bed consumes no resource");var pos=f.device.getBlockPos();t.getLevel().removeBlock(pos,false);int count=t.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(2)).stream().filter(e->e.getItem().is(ConciliumItems.ASTRAL_MONITOR.get())).mapToInt(e->e.getItem().getCount()).sum();t.assertTrue(count==4,"Breaking device returns exactly four stored monitors");t.succeed();});
    }
    @GameTest(template="empty") public static void sixProtolimbsCollectOnceAndUnlockSolidVoid(GameTestHelper t){
        var player=player(t,"LimbCollector");for(int i=0;i<6;i++){var limb=ConciliumEntities.PROTOLIMB.get().create(t.getLevel());limb.setLimbType(i);limb.moveTo(player.position());t.getLevel().addFreshEntity(limb);limb.playerTouch(player);limb.playerTouch(player);t.assertTrue(EtherealState.limbs(player)==i+1&&limb.isRemoved(),"Real limb "+i+" counted once despite duplicate contact");}
        var missingHosts=List.of("taintedmagic","thaumichorizons").stream().filter(id->!net.minecraftforge.fml.ModList.get().isLoaded(id)).toList();
        boolean completed=thaumcraft.api.ThaumcraftApiHelper.isResearchComplete(player,"thaumicconcilium:SOLIDVOID");
        if(missingHosts.isEmpty())t.assertTrue(completed,"Six real collected limbs unlock the survival solid-void research with both required hosts");
        else{
            t.assertTrue(!completed,"Solid-void research remains unavailable while a required host is absent");
            System.out.println("CONCILIUM_BRANCH_UNAVAILABLE solid_void_research: positive unlock assertion skipped because required hosts are absent: "+missingHosts+"; six-limb collection and duplicate-contact assertions still run");
        }
        var last=ConciliumEntities.PROTOLIMB.get().create(t.getLevel());last.playerTouch(player);t.assertTrue(EtherealState.limbs(player)==6,"Additional limbs do not overflow six");t.succeed();
    }
    @GameTestGenerator public static Collection<TestFunction> optional(){
        boolean host=net.minecraftforge.fml.ModList.get().isLoaded("taintedmagic");return List.of(new TestFunction("concilium_optional","thaumicconcilium.protobody_"+(host?"real_six_limbs_retry_reload":"absent_host_no_split"),"thaumicconcilium:empty",100,0,true,host?ConciliumDeviceBehaviourTests::protobody:ConciliumDeviceBehaviourTests::protobodyAbsent));
    }
    public static final class RejectLimb {
        final AABB area;boolean rejected;RejectLimb(AABB area){this.area=area;}
        @SubscribeEvent public void spawn(EntityJoinLevelEvent event){if(event.getEntity() instanceof Protolimb limb&&area.contains(limb.position())){if(limb.limbType()==3&&!rejected){rejected=true;event.setCanceled(true);}else{limb.setNoAi(true);limb.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);}}}
    }
    private static ProtobodyDrop drop(GameTestHelper t){var result=ConciliumEntities.PROTOBODY_DROP.get().create(t.getLevel());var pos=t.absolutePos(new BlockPos(16,1,16));result.moveTo(pos.getX()+.5,t.getLevel().getMinBuildHeight()-18,pos.getZ()+.5);result.setItem(new ItemStack(ConciliumItems.PROTOBODY.get()));t.getLevel().addFreshEntity(result);return result;}
    static void protobody(GameTestHelper t){
        var drop=drop(t);var area=drop.getBoundingBox().inflate(8);var reject=new RejectLimb(area);MinecraftForge.EVENT_BUS.register(reject);
        try{drop.tick();var parts=t.getLevel().getEntitiesOfClass(Protolimb.class,area);t.assertTrue(reject.rejected&&parts.size()==5&&!drop.isRemoved(),"Five accepted real limbs and one rejected spawn keep original body");var saved=new CompoundTag();drop.saveWithoutId(saved);t.assertTrue(saved.getInt("ConciliumParts")==55,"Accepted five-type mask saved");drop.discard();var restored=ConciliumEntities.PROTOBODY_DROP.get().create(t.getLevel());restored.load(saved);t.getLevel().addFreshEntity(restored);for(int i=0;i<22&&!restored.isRemoved();i++)restored.tick();parts=t.getLevel().getEntitiesOfClass(Protolimb.class,area);t.assertTrue(restored.isRemoved()&&parts.size()==6&&parts.stream().map(Protolimb::limbType).distinct().count()==6,"Reload retries only the refused type and consumes body once");parts.forEach(Entity::discard);}finally{MinecraftForge.EVENT_BUS.unregister(reject);}t.succeed();
    }
    static void protobodyAbsent(GameTestHelper t){var drop=drop(t);drop.tick();t.assertTrue(!drop.isRemoved()&&t.getLevel().getEntitiesOfClass(Protolimb.class,drop.getBoundingBox().inflate(8)).isEmpty(),"Missing Tainted Magic leaves original body and never creates unavailable limbs");drop.discard();t.succeed();}
}
