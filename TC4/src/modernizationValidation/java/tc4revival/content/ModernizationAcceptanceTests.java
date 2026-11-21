package tc4revival.content;

import java.util.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.*;
import tc4revival.aspect.*;
import tc4revival.research.*;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public class ModernizationAcceptanceTests {
    @GameTest(template="wand_test_empty")
    public static void modernNativeMushroomsAndSoapUse(GameTestHelper t) {
        var level=t.getLevel();var base=t.absolutePos(new BlockPos(4,4,4));int grown=0;
        for(int seed=0;seed<8;seed++) {
            for(var pos:BlockPos.betweenClosed(base.offset(-3,-1,-3),base.offset(3,14,3)))
                level.setBlock(pos,pos.getY()==base.getY()-1?Blocks.MYCELIUM.defaultBlockState():Blocks.AIR.defaultBlockState(),2);
            if(ForestMushroomFeature.grow(level,base,net.minecraft.util.RandomSource.create(seed))) {
                grown++;
                t.assertTrue(level.getBlockState(base).is(Blocks.MUSHROOM_STEM),"Native feature places a real mushroom stem");
            }
        }
        t.assertTrue(grown==8,"Native configured mushroom features generate in clear suitable terrain");
        var flowerBase=base.above(18);
        for(var pos:BlockPos.betweenClosed(flowerBase.offset(-8,-1,-8),flowerBase.offset(8,4,8)))
            level.setBlock(pos,pos.getY()==flowerBase.getY()-1?Blocks.MOSS_BLOCK.defaultBlockState():Blocks.AIR.defaultBlockState(),2);
        t.assertTrue(LegacyForestPlants.flowers(level,flowerBase,net.minecraft.util.RandomSource.create(3),Blocks.POPPY.defaultBlockState()),
                "Forest flowers use native survival on modern moss soil");
        var soap=new ItemStack(TCCleansing.SANITY_SOAP.get());
        t.assertTrue(soap.getUseAnimation()==UseAnim.BRUSH,"Soap uses modern brushing and grants no native blocking");
        var player=new FakePlayer(level,new GameProfile(UUID.randomUUID(),"SoapModernAudit"));
        player.gameMode.changeGameModeForPlayer(net.minecraft.world.level.GameType.CREATIVE);
        soap.getItem().releaseUsing(soap,level,player,4);
        t.assertTrue(soap.getCount()==1,"Creative cleansing does not consume soap");t.succeed();
    }
    @GameTest(template="wand_test_empty")
    public static void modernContainerCollisionAndCreativeBreak(GameTestHelper t) {
        var level=t.getLevel();var pos=t.absolutePos(new BlockPos(2,2,2));
        var player=new FakePlayer(level,new GameProfile(UUID.randomUUID(),"ContainerAudit"));
        player.gameMode.changeGameModeForPlayer(net.minecraft.world.level.GameType.CREATIVE);
        player.setPos(pos.getCenter());var area=new AABB(pos).inflate(2);
        for(var block:net.minecraft.core.registries.BuiltInRegistries.BLOCK) {
            if(!(block instanceof ArcanePedestalBlock || block instanceof EssentiaJarBlock
                    || block instanceof BrainJarBlock || block instanceof EldritchLootBlock))continue;
            var state=block.defaultBlockState();level.setBlock(pos,state,3);
            t.assertTrue(!net.minecraft.world.phys.shapes.Shapes.joinIsNotEmpty(state.getShape(level,pos),state.getCollisionShape(level,pos),
                    net.minecraft.world.phys.shapes.BooleanOp.NOT_SAME),"Collision follows actual container shape: "+block);
            if(block instanceof EssentiaJarBlock || block instanceof BrainJarBlock) {
                level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,area).forEach(Entity::discard);
                t.assertTrue(player.gameMode.destroyBlock(pos),"Creative player actually breaks container");
                t.assertTrue(level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,area).isEmpty(),"Creative breaking makes no forced extra item");
            } else level.setBlock(pos,Blocks.AIR.defaultBlockState(),3);
        }
        t.succeed();
    }
    @GameTest(template="wand_test_empty")
    public static void modernFrostRejectedDamageAndSavedBounces(GameTestHelper t) {
        var level=t.getLevel();var owner=new FakePlayer(level,new GameProfile(UUID.randomUUID(),"FrostAuditModern"));
        var cow=EntityType.COW.create(level);cow.setInvulnerable(true);
        var shard=new FrostShardEntity(level,owner,1,8,2,true,true);
        shard.onHit(new EntityHitResult(cow));
        t.assertTrue(!cow.hasEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN),"Rejected frost damage cannot apply slowing");
        var bouncing=new FrostShardEntity(level,owner,1,8,2,false,true);
        bouncing.setDeltaMovement(0,-.2,0);
        bouncing.onHit(new BlockHitResult(Vec3.ZERO,Direction.UP,t.absolutePos(BlockPos.ZERO),false));
        var saved=new CompoundTag();bouncing.saveWithoutId(saved);
        var restored=new FrostShardEntity(TCEntities.FROST_SHARD.get(),level);restored.load(saved);
        var again=new CompoundTag();restored.saveWithoutId(again);
        t.assertTrue(again.getInt("BouncesRemaining")==5&&again.getDouble("Bounce")==.8,"Reload retains consumed bounce count and damping");t.succeed();
    }
    @GameTest(template="wand_test_empty")
    public static void modernRunicPlayersHaveIndependentCooldowns(GameTestHelper t) {
        var level=t.getLevel();
        var a=new FakePlayer(level,new GameProfile(UUID.randomUUID(),"RunicFirst"));
        var b=new FakePlayer(level,new GameProfile(UUID.randomUUID(),"RunicSecond"));
        for(var p:List.of(a,b)) {
            p.getAttribute(TCAttributes.RUNIC_SHIELD.get()).setBaseValue(3);
            p.getInventory().setItem(0,TCItems.CASTING_WAND.get().create(WandParts.Rod.WOOD,WandParts.Cap.IRON,false,true));
            RunicShieldEvents.tick(new net.minecraftforge.event.entity.living.LivingEvent.LivingTickEvent(p));
        }
        t.assertTrue(RunicShieldEvents.state(a).charge()==1&&RunicShieldEvents.state(b).charge()==1,"Both players recharge with actual wand payment");
        RunicShieldEvents.hurt(new net.minecraftforge.event.entity.living.LivingHurtEvent(a,a.damageSources().generic(),1));
        var other=RunicShieldEvents.state(b);
        // Reset this player's transient charge only, so its first recharge can be observed immediately.
        var c=new FakePlayer(level,new GameProfile(UUID.randomUUID(),"RunicThird"));
        c.getAttribute(TCAttributes.RUNIC_SHIELD.get()).setBaseValue(4);
        c.getInventory().setItem(0,TCItems.CASTING_WAND.get().create(WandParts.Rod.WOOD,WandParts.Cap.IRON,false,true));
        RunicShieldEvents.tick(new net.minecraftforge.event.entity.living.LivingEvent.LivingTickEvent(c));
        t.assertTrue(RunicShieldEvents.state(c).charge()==1&&RunicShieldEvents.state(a).charge()==0,"Breaking one shield never pauses another player");
        a.getAttribute(TCAttributes.RUNIC_SHIELD.get()).setBaseValue(5);
        b.getAttribute(TCAttributes.RUNIC_SHIELD.get()).setBaseValue(6);
        a.tickCount=1;b.tickCount=1;
        RunicShieldEvents.equipmentChanged(a);RunicShieldEvents.equipmentChanged(b);
        RunicShieldEvents.tick(new net.minecraftforge.event.entity.living.LivingEvent.LivingTickEvent(a));
        RunicShieldEvents.tick(new net.minecraftforge.event.entity.living.LivingEvent.LivingTickEvent(b));
        t.assertTrue(RunicShieldEvents.state(a).maximum()==5&&other.maximum()==6,"Processing one equipment change does not consume another player's change");
        var shield=new RunicShieldState();shield.equipment(new RunicShieldState.Equipment(2,10,0,0,0));
        t.assertTrue(shield.recharge(100,500,()->true)&&!shield.recharge(100,500,()->true),"Charged upgrades cannot recharge twice at the same game time");
        shield.hurt(.25f,101);shield.recharge(102,500,()->true);shield.recharge(103,500,()->true);
        t.assertTrue(shield.exactCharge()==2,"Fractional recharge caps at actual equipment capacity");t.succeed();
    }
    @GameTest(template="wand_test_empty")
    public static void modernScanRewardFullIntegerRange(GameTestHelper t) {
        var buffer=new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        try {
            var packet=new tc4revival.network.ScanFeedbackPacket(ScanRules.Status.SUCCESS,"",
                    List.of(new tc4revival.network.ScanFeedbackPacket.Reward("aer",Integer.MAX_VALUE,false)));
            packet.encode(buffer);
            t.assertTrue(tc4revival.network.ScanFeedbackPacket.decode(buffer).equals(packet),"Scan feedback roundtrips full int without truncation");
            t.assertTrue(packet.rewards().get(0).gainedAmount()==Integer.MAX_VALUE
                    &&packet.rewards().get(0).gained()==Short.MAX_VALUE,"Legacy reward access remains a saturated binary-compatible view");
        } finally {buffer.release();}
        var aspects=TCAspects.catalog();var k=new PlayerKnowledge(aspects);var air=aspects.require("aer");
        var result=ScanRules.apply(k,aspects,PlayerKnowledge.ScanKind.OBJECT,"modern-max-value",PlayerKnowledge.ScanMode.ENHANCED,
                new AspectAmounts().add(air,Integer.MAX_VALUE),Integer.MAX_VALUE);
        t.assertTrue(result.succeeded()&&k.poolAmount(air)==Integer.MAX_VALUE,"Enhanced extreme aspect values saturate rather than crash");
        var rewards=KnowledgeFragments.consume(k,aspects,bound->0);
        t.assertTrue(rewards.stream().filter(r->r.aspect().equals(air)).allMatch(r->r.gained()==0),"Fragment feedback reports actual saturated growth");
        t.succeed();
    }
    private static GolemEntity golem(GameTestHelper t,BlockPos local,GolemCoreType core) {
        var g=TCEntities.GOLEM.get().create(t.getLevel());var pos=t.absolutePos(local);
        g.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+.5);
        var c=new GolemConfiguration(GolemMaterial.THAUMIUM,true);c.installCore(core);
        g.state().configure(c);g.state().home(pos,Direction.UP);g.state().owner("ModernAudit");g.refreshConfiguration();g.setHealth(g.getMaxHealth());return g;
    }
    private static void floor(GameTestHelper t) { for(int x=0;x<8;x++)for(int z=0;z<8;z++)t.setBlock(new BlockPos(x,0,z),Blocks.STONE); }
    @GameTest(template="wand_test_empty")
    public static void modernWorldHeightAndDevicePhysics(GameTestHelper t) {
        var level=t.getLevel();var p=t.absolutePos(new BlockPos(2,2,2));
        var env=new ResearchTableBonuses.Environment() {
            public int tableY(){return -40;} public int height(){return level.getHeight();}
            public int minimumHeight(){return level.getMinBuildHeight();} public boolean darkCoveredNight(){return false;}
            public ResearchTableBonuses.Source at(int x,int y,int z){return x==0&&y==0&&z==0?ResearchTableBonuses.Source.primal("terra",1):ResearchTableBonuses.Source.NONE;}
        };
        t.assertTrue(ResearchTableBonuses.recalculate(Set.of(),env,List.of("terra"),bound->0).contains("terra"),"Environment scan works below Y zero");
        t.assertTrue(ResearchTableBonuses.nextCounter(598)==599&&ResearchTableBonuses.nextCounter(599)==0,"Bonus timer is exactly 600 ticks");
        var tag=new CompoundTag();var fire=TCAspects.catalog().require("ignis");tag.putInt("ignis",999);
        WandRegeneration.tick(tag,0,TCAspects.catalog(),bound->0,WandParts.Rod.BLAZE,10000);
        t.assertTrue(tag.getInt("ignis")==1000,"Partial-vis regeneration stops at ten percent");
        level.setBlock(p,TCArcane.WARDED.get().defaultBlockState(),3);
        var ward=(WardedBlockEntity)level.getBlockEntity(p);var data=new CompoundTag();
        data.put("Original",net.minecraft.nbt.NbtUtils.writeBlockState(Blocks.PACKED_ICE.defaultBlockState()));ward.load(data);
        t.assertTrue(level.getBlockState(p).getFriction(level,p,null)==Blocks.PACKED_ICE.defaultBlockState().getFriction(level,p,null),"Warding keeps native ice friction");
        data.put("Original",net.minecraft.nbt.NbtUtils.writeBlockState(Blocks.OAK_PLANKS.defaultBlockState()));ward.load(data);
        t.assertTrue(level.getBlockState(p).getSoundType(level,p,null)==Blocks.OAK_PLANKS.defaultBlockState().getSoundType(level,p,null),"Warding keeps native material sounds");
        for(var block:List.of(TCArcane.WORKBENCH_CHARGER.get(),TCArcane.ALCHEMY_CENTRIFUGE.get())) {
            level.setBlock(p,block.defaultBlockState(),3);
            t.assertTrue(level.getBlockState(p).getCollisionShape(level,p).bounds().equals(level.getBlockState(p).getShape(level,p).bounds()),"Device collision follows its visible bounds");
        }
        t.succeed();
    }
    @GameTest(template="wand_test_empty",timeoutTicks=300)
    public static void modernGolemOpensWoodDoor(GameTestHelper t) { corridor(t,false); }
    @GameTest(template="wand_test_empty",timeoutTicks=300)
    public static void modernGolemOpensGate(GameTestHelper t) { corridor(t,true); }
    private static void corridor(GameTestHelper t,boolean gate) {
        var level=t.getLevel();var start=t.absolutePos(new BlockPos(2,1,1));
        for(int z=-1;z<=9;z++)for(int x=-1;x<=1;x++)for(int y=-1;y<=2;y++)
            level.setBlock(start.offset(x,y,z),y==-1||y==2||x!=0?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),3);
        var at=start.south(4);var home=start.south(8);
        var state=gate?Blocks.OAK_FENCE_GATE.defaultBlockState().setValue(FenceGateBlock.FACING,Direction.SOUTH):
            Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.FACING,Direction.SOUTH);
        level.setBlock(at,state,3);
        if(!gate)level.setBlock(at.above(),state.setValue(DoorBlock.HALF,net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER),3);
        var g=golem(t,new BlockPos(2,1,1),GolemCoreType.GATHER);g.state().home(home,Direction.UP);level.addFreshEntity(g);
        boolean[] opened={false};
        t.succeedWhen(()->{
            opened[0]|=level.getBlockState(at).getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.OPEN);
            t.assertTrue(g.position().distanceToSqr(home.getCenter())<2.25,"Actual goal selector traverses the closed "+(gate?"gate":"door"));
            t.assertTrue(opened[0],"Obstacle was opened during traversal");g.discard();
        });
    }
    @GameTest(template="wand_test_empty")
    public static void modernGolemRespectsLockedDoors(GameTestHelper t) {
        var level=t.getLevel();var start=t.absolutePos(new BlockPos(2,1,1));
        for(int z=-1;z<=9;z++)for(int x=-1;x<=1;x++)for(int y=-1;y<=2;y++)
            level.setBlock(start.offset(x,y,z),y==-1||y==2||x!=0?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),3);
        var g=golem(t,new BlockPos(2,1,1),GolemCoreType.GATHER);
        for(var block:List.of(Blocks.IRON_DOOR,TCArcane.ARCANE_DOOR.get())) {
            var at=start.south(4);var state=block.defaultBlockState().setValue(DoorBlock.FACING,Direction.SOUTH);
            level.setBlock(at.above(),Blocks.AIR.defaultBlockState(),18);level.setBlock(at,Blocks.AIR.defaultBlockState(),18);
            level.setBlock(at,state,18);level.setBlock(at.above(),state.setValue(DoorBlock.HALF,net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER),18);
            var path=g.getNavigation().createPath(start.south(8),0);
            t.assertTrue(path==null||!path.canReach(),"Closed locked door cannot be planned as openable: "+block);
            t.assertTrue(!level.getBlockState(at).getValue(DoorBlock.OPEN),"Navigation never changes locked door state");
        }
        g.discard();t.succeed();
    }
    @GameTest(template="wand_test_empty",timeoutTicks=240)
    public static void modernGolemPauseAndResume(GameTestHelper t) {
        floor(t);var g=golem(t,new BlockPos(1,1,1),GolemCoreType.GATHER);var home=t.absolutePos(new BlockPos(6,1,6));
        g.state().home(home,Direction.UP);g.state().paused(true);t.getLevel().addFreshEntity(g);var before=g.position();
        t.startSequence().thenIdle(30).thenExecute(()->{
            t.assertTrue(g.position().distanceToSqr(before)<.05 && g.getNavigation().isDone(),"Pause prevents native task navigation");
            g.state().paused(false);
        }).thenWaitUntil(()->t.assertTrue(g.position().distanceToSqr(home.getCenter())<2.25,"Resume restarts ordinary work navigation"))
        .thenExecute(g::discard).thenSucceed();
    }
    @GameTest(template="wand_test_empty",timeoutTicks=400)
    public static void modernGolemEscapesThenResumes(GameTestHelper t) {
        for(int x=-18;x<=18;x++)for(int z=-18;z<=18;z++)t.setBlock(new BlockPos(x,0,z),Blocks.STONE);
        var g=golem(t,new BlockPos(1,1,1),GolemCoreType.GATHER);var home=t.absolutePos(new BlockPos(6,1,6));g.state().home(home,Direction.UP);
        var creeper=EntityType.CREEPER.create(t.getLevel());creeper.setPos(g.position().add(-1,0,0));creeper.setNoAi(true);
        var saved=creeper.saveWithoutId(new CompoundTag());saved.putShort("Fuse",(short)2000);creeper.load(saved);creeper.setSwellDir(1);
        t.getLevel().addFreshEntity(creeper);t.getLevel().addFreshEntity(g);
        t.startSequence().thenWaitUntil(()->t.assertTrue(g.distanceToSqr(creeper)>36,"Native avoidance escapes an armed creeper"))
        .thenExecute(creeper::discard).thenWaitUntil(()->t.assertTrue(g.position().distanceToSqr(home.getCenter())<2.25,"Work resumes after danger is removed"))
        .thenExecute(g::discard).thenSucceed();
    }
    @GameTest(template="wand_test_empty")
    public static void modernLootAmuletsRollPerSelection(GameTestHelper t) {
        var pool=LegacyLootBags.decode(com.google.gson.JsonParser.parseString("{\"entries\":[{\"stack\":{\"precharged_amulet\":true},\"weights\":[1,1,1]}]}").getAsJsonObject());
        var chest=LegacyChestContents.decode(com.google.gson.JsonParser.parseString("{\"entries\":[{\"stack\":{\"precharged_amulet\":true},\"weight\":1}]}").getAsJsonObject());
        var pech=new PechTrades.Entry(5,new ItemStack(TCItems.LESSER_VIS_AMULET.get()),true);
        var bags=new HashSet<String>();var chests=new HashSet<String>();var trades=new HashSet<String>();
        for(int seed=0;seed<16;seed++) {
            bags.add(pool.generate(0,net.minecraft.util.RandomSource.create(seed)).getTag().toString());
            var inventory=new net.minecraft.world.SimpleContainer(1);chest.fill(inventory,1,net.minecraft.util.RandomSource.create(seed));
            chests.add(inventory.getItem(0).getTag().toString());trades.add(pech.generate(net.minecraft.util.RandomSource.create(seed)).getTag().toString());
        }
        t.assertTrue(bags.size()>1&&chests.size()>1&&trades.size()>1,"Bag, chest and Pech rewards roll charge for every selection");t.succeed();
    }
    @GameTest(template="wand_test_empty",timeoutTicks=240)
    public static void modernGolemWalksHome(GameTestHelper t) {
        floor(t);var g=golem(t,new BlockPos(1,1,1),GolemCoreType.GATHER);var home=t.absolutePos(new BlockPos(6,1,6));g.state().home(home,Direction.UP);
        t.getLevel().addFreshEntity(g);
        t.succeedWhen(()->{t.assertTrue(g.position().distanceToSqr(home.getCenter())<2.25,"Native navigation reaches home");g.discard();});
    }
    @GameTest(template="wand_test_empty",timeoutTicks=240)
    public static void modernGolemFightsAndSwitches(GameTestHelper t) {
        floor(t);var g=golem(t,new BlockPos(1,1,1),GolemCoreType.GUARD);
        var enemy=EntityType.HUSK.create(t.getLevel());enemy.setPos(t.absolutePos(new BlockPos(5,1,5)).getCenter());enemy.setNoAi(true);
        t.getLevel().addFreshEntity(g);t.getLevel().addFreshEntity(enemy);
        t.succeedWhen(()->{
            t.assertTrue(enemy.getHealth()<enemy.getMaxHealth(),"Native targeting, pathing and melee damage occurred");
            var c=new GolemConfiguration(GolemMaterial.THAUMIUM,true);c.installCore(GolemCoreType.GATHER);g.state().configure(c);g.refreshConfiguration();
            t.assertTrue(g.getTarget()==null,"Switching core clears combat target");g.discard();enemy.discard();
        });
    }
    @GameTest(template="wand_test_empty")
    public static void modernGolemTasksAreIdempotent(GameTestHelper t) {
        var g=golem(t,new BlockPos(2,2,2),GolemCoreType.GUARD);
        var work=new GoalSelector(()->t.getLevel().getProfiler());var targets=new GoalSelector(()->t.getLevel().getProfiler());
        GolemTaskSet.install(g,work,targets);int w=work.getAvailableGoals().size(),a=targets.getAvailableGoals().size();
        for(int i=0;i<20;i++)GolemTaskSet.install(g,work,targets);
        t.assertTrue(work.getAvailableGoals().size()==w && targets.getAvailableGoals().size()==a,"Repeated installs do not duplicate goals");
        var c=new GolemConfiguration(GolemMaterial.THAUMIUM,true);c.installCore(GolemCoreType.FILL);g.state().configure(c);GolemTaskSet.install(g,work,targets);
        t.assertTrue(targets.getAvailableGoals().isEmpty(),"Work core has no stale target selectors");t.succeed();
    }
    @GameTest(template="wand_test_empty")
    public static void modernDoubleChestUsesCombinedHandler(GameTestHelper t) {
        var p=t.absolutePos(new BlockPos(2,1,2));var level=t.getLevel();
        level.setBlock(p,Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING,Direction.NORTH).setValue(ChestBlock.TYPE,net.minecraft.world.level.block.state.properties.ChestType.LEFT),3);
        level.setBlock(p.east(),Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING,Direction.NORTH).setValue(ChestBlock.TYPE,net.minecraft.world.level.block.state.properties.ChestType.RIGHT),3);
        var view=GolemInventoryViews.single(level,p,Direction.UP);
        t.assertTrue(view!=null && view.getSlots()==54 && GolemInventoryViews.same(level,p,p.east()),"Double chest exposes 54 slots exactly once");
        var input=new ItemStack(Items.DIAMOND,64);var result=net.minecraftforge.items.ItemHandlerHelper.insertItemStacked(view,input,false);
        int count=0;for(int i=0;i<view.getSlots();i++)count+=view.getStackInSlot(i).getCount();
        t.assertTrue(result.isEmpty() && count==64 && input.getCount()==64,"Native insertion conserves input and inventory");t.succeed();
    }
    @GameTest(template="wand_test_empty")
    public static void modernBrainJarMergedOrbConservation(GameTestHelper t) {
        var level=t.getLevel();var p=t.absolutePos(new BlockPos(2,2,2));level.setBlock(p,TCArcane.BRAIN_JAR.get().defaultBlockState(),3);
        var jar=(BrainJarBlockEntity)level.getBlockEntity(p);jar.insertExperience(1995,false);
        var orb=new ExperienceOrb(level,p.getX()+.5,p.getY()+.5,p.getZ()+.5,7);var data=new CompoundTag();orb.addAdditionalSaveData(data);data.putInt("Count",3);orb.readAdditionalSaveData(data);level.addFreshEntity(orb);
        jar.tick();int remainder=level.getEntitiesOfClass(ExperienceOrb.class,new AABB(p).inflate(1),Entity::isAlive).stream().mapToInt(BrainJarBlockEntity::orbExperience).sum();
        t.assertTrue(jar.experience()==2000 && remainder==16,"Partial merged XP orb is conserved at capacity");
        level.getEntitiesOfClass(ExperienceOrb.class,new AABB(p).inflate(1)).forEach(Entity::discard);t.succeed();
    }
    @GameTest(template="wand_test_empty")
    public static void modernKnowledgeRangeAndLegacySave(GameTestHelper t) {
        var catalog=TCAspects.catalog();var a=catalog.require("aer");var k=new PlayerKnowledge(catalog);
        k.setPool(a,100000);t.assertTrue(k.poolAmount(a)==100000 && k.pool(a)==32767,"Large points retain int value and compatible short saturation");
        t.assertTrue(!k.changePool(a,-100001) && k.poolAmount(a)==100000,"Insufficient debit is atomic");
        k.changePool(a,Integer.MAX_VALUE);t.assertTrue(k.poolAmount(a)==Integer.MAX_VALUE,"Addition saturates instead of wrapping");
        var saved=KnowledgeNbt.write(k);t.assertTrue(KnowledgeNbt.read(saved,catalog).knowledge().poolAmount(a)==Integer.MAX_VALUE,"NBT int survives");
        saved.getList("aspects",10).getCompound(0).putShort("amount",(short)123);t.assertTrue(KnowledgeNbt.read(saved,catalog).knowledge().poolAmount(a)==123,"Old short NBT is accepted");t.succeed();
    }
    @GameTest(template="wand_test_empty")
    public static void modernNativeLootAndHoeEnchantments(GameTestHelper t) {
        var inventory=new net.minecraft.world.SimpleContainer(3);inventory.setItem(0,new ItemStack(Items.DIAMOND,5));
        var pool=new LegacyChestContents(List.of(new LegacyChestContents.Entry(new ItemStack(Items.IRON_INGOT),64,64,1)));
        pool.fill(inventory,6,net.minecraft.util.RandomSource.create(2));
        t.assertTrue(inventory.getItem(0).is(Items.DIAMOND) && inventory.getItem(0).getCount()==5 && inventory.getItem(1).getCount()+inventory.getItem(2).getCount()==128,"Generated loot never replaces inventory and respects capacity");
        boolean modern=false;for(int i=0;i<100;i++) {
            var enchanted=LegacyRandomEnchantments.apply(new ItemStack(Items.DIAMOND_HOE),30,net.minecraft.util.RandomSource.create(i));
            if(enchanted.getEnchantmentLevel(Enchantments.BLOCK_EFFICIENCY)>0)modern=true;
        }
        t.assertTrue(modern,"Native enchanting permits modern hoe efficiency");
        var json=com.google.gson.JsonParser.parseString("{\"entries\":[{\"stack\":{\"item\":\"minecraft:book\"},\"weights\":[1,1,1]}]}").getAsJsonObject();
        var bag=LegacyLootBags.decode(json);var variants=new HashSet<String>();
        for(int i=0;i<100;i++)variants.add(bag.generate(0,net.minecraft.util.RandomSource.create(i)).getTag().toString());
        t.assertTrue(variants.size()>1,"Books are enchanted per draw rather than mutating one shared template");t.succeed();
    }
    @GameTest(template="wand_test_empty")
    public static void modernInfusionCompleteAssignment(GameTestHelper t) {
        var broad=new InfusionIngredient(com.google.gson.JsonParser.parseString("{\"ingredient\":[{\"item\":\"minecraft:diamond\"},{\"item\":\"minecraft:emerald\"}]}").getAsJsonObject());
        var narrow=new InfusionIngredient(com.google.gson.JsonParser.parseString("{\"ingredient\":{\"item\":\"minecraft:diamond\"}}").getAsJsonObject());
        var inputs=List.of(new ItemStack(Items.DIAMOND),new ItemStack(Items.EMERALD));var match=InfusionMatching.assignment(inputs,List.of(broad,narrow),false);
        t.assertTrue(match!=null && match[0]==1 && match[1]==0,"Complete matching reserves unique diamond for narrow ingredient");t.succeed();
    }
    private static class HitProbe extends PrimalArrowEntity {
        HitProbe(ServerLevel level) {super(TCEntities.PRIMAL_ARROW.get(),level);}
        void impact(Entity target) {super.onHitEntity(new EntityHitResult(target));}
    }
    @GameTest(template="wand_test_empty")
    public static void modernArrowNativeImpactAndDamageMixin(GameTestHelper t) {
        var level=t.getLevel();boolean[] tagged={false};
        var target=new Cow(EntityType.COW,level) {
            @Override public boolean hurt(net.minecraft.world.damagesource.DamageSource source,float amount) {
                tagged[0]=source.is(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DAMAGE_TYPE,net.minecraft.resources.ResourceLocation.parse("thaumcraft:air_arrow")));
                return super.hurt(source,amount);
            }
        };
        target.setPos(t.absolutePos(new BlockPos(3,3,3)).getCenter());var arrow=new HitProbe(level);arrow.setBaseDamage(5);arrow.setDeltaMovement(1,0,0);arrow.impact(target);
        t.assertTrue(tagged[0] && target.getHealth()==5,"Native hit uses elemental DamageSource and inherited base damage");
        var immune=EntityType.COW.create(level);immune.setInvulnerable(true);var order=new HitProbe(level);var data=new CompoundTag();order.saveWithoutId(data);data.putByte("type",(byte)4);order.load(data);order.setDeltaMovement(1,0,0);order.impact(immune);
        t.assertTrue(!immune.hasEffect(net.minecraft.world.effect.MobEffects.WEAKNESS),"Native rejected hit never applies elemental status");t.succeed();
    }
    @GameTest(template="wand_test_empty")
    public static void modernNodeJarPreservesCapacity(GameTestHelper t) {
        var a=TCAspects.catalog().require("aer");var payload=new NodeJarData(new AspectAmounts().add(a,3),AuraNodeType.NORMAL,null,"modern",new AspectAmounts().add(a,20));
        var restored=NodeJarData.read(payload.write());t.assertTrue(restored.aspects().amount(a)==3 && restored.capacity().amount(a)==20,"Portable payload distinguishes charge and capacity");
        var p=t.absolutePos(new BlockPos(2,2,2));t.getLevel().setBlock(p,TCArcane.NODE_JAR.get().defaultBlockState(),3);var jar=(NodeJarBlockEntity)t.getLevel().getBlockEntity(p);jar.configure(restored);
        jar.takeFromContainer(a,2);var saved=jar.asItem();var placed=new NodeJarBlockEntity(p,jar.getBlockState());placed.configure(NodeJarData.read(saved.getTag()));
        t.assertTrue(placed.base().amount(a)==20 && placed.aspects().amount(a)==1,"Extract, itemize and replace preserve permanent capacity");t.succeed();
    }
    @GameTest(template="wand_test_empty")
    public static void modernEarDoesNotRewriteInstruments(GameTestHelper t) {
        t.assertTrue(ArcaneEarListening.tone(Blocks.EMERALD_BLOCK.defaultBlockState())==ArcaneEarListening.instrument(net.minecraft.world.level.block.state.properties.NoteBlockInstrument.BIT),"Emerald instrument follows native BIT");
        t.assertTrue(ArcaneEarListening.tone(Blocks.GLOWSTONE.defaultBlockState())==ArcaneEarListening.instrument(net.minecraft.world.level.block.state.properties.NoteBlockInstrument.PLING),"Glowstone instrument follows native PLING");t.succeed();
    }
}
