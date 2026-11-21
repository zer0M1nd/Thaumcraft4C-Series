package tc4revival.content;

import java.util.*;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.gametest.*;
import tc4revival.aspect.*;
import tc4revival.research.*;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public  class InfusionAuditServerTests {
    private static ArcanePedestalBlockEntity pedestal(ServerLevel level,BlockPos pos,int count) {
        level.setBlock(pos,TCArcane.ARCANE_PEDESTAL.get().defaultBlockState(),18);
        var value=(ArcanePedestalBlockEntity)level.getBlockEntity(pos);var stack=new ItemStack(Items.DIAMOND,count);
        stack.getOrCreateTag().putString("Audit","whole-stack");value.setFromInfusion(stack);return value;
    }
    @GameTest(template="wand_test_empty")
    public static void infusionLayoutRetainsScanOrderBoundsAndFloatSymmetry(GameTestHelper test) {
        var ordered=tc4revival.config.TCConfig.SERVER.orderedInfusion;boolean previous=ordered.get();
        try {ordered.set(false);checkUnorderedLayout(test);} finally {ordered.set(previous);}
    }
    private static void checkUnorderedLayout(GameTestHelper test) {
        var level=test.getLevel();var origin=test.absolutePos(new BlockPos(3,20,3));
        for(var pos:BlockPos.betweenClosed(origin.offset(-13,-11,-13),origin.offset(13,6,13)))level.setBlock(pos,Blocks.AIR.defaultBlockState(),18);
        var offsets=List.of(new BlockPos(-8,-1,-8),new BlockPos(-8,-10,8),new BlockPos(8,-10,-8),new BlockPos(8,-1,8));
        var expected=offsets.stream().map(origin::offset).toList();
        for(var pos:expected)pedestal(level,pos,0);
        for(var offset:List.of(new BlockPos(-8,-2,-8),new BlockPos(-9,-1,0),new BlockPos(0,-2,0),new BlockPos(1,0,0),new BlockPos(2,-11,0)))pedestal(level,origin.offset(offset),0);
        var layout=InfusionLayout.scan(level,origin);
        test.assertTrue(layout.pedestals().equals(expected)&&layout.symmetry()==0,"Old x/z/depth ordering, one pedestal per column, strict below/bounds, central exclusion");
        pedestal(level,expected.get(0),1);test.assertTrue(InfusionLayout.scan(level,origin).symmetry()==1,"One occupied half adds one imbalance");
        level.setBlock(expected.get(2),Blocks.AIR.defaultBlockState(),18);
        test.assertTrue(InfusionLayout.scan(level,origin).symmetry()==3,"Unpaired empty pedestal adds two");
        level.setBlock(origin.offset(-12,5,-12),Blocks.SKELETON_SKULL.defaultBlockState(),18);
        level.setBlock(origin.offset(12,5,12),TCArcane.candle(0).defaultBlockState(),18);
        test.assertTrue(InfusionLayout.scan(level,origin).symmetry()==2,"Different stabilizer types pair and combined float score truncates toward zero");
        for(var pos:expected)level.setBlock(pos,Blocks.AIR.defaultBlockState(),18);
        level.setBlock(origin.offset(-8,-2,-8),Blocks.AIR.defaultBlockState(),18);
        for(int z=-5;z<=5;z++) {
            level.setBlock(origin.offset(-12,-10,z),Blocks.SKELETON_SKULL.defaultBlockState(),18);
            level.setBlock(origin.offset(12,-10,-z),TCArcane.candle(1).defaultBlockState(),18);
        }
        test.assertTrue(InfusionLayout.scan(level,origin).symmetry()==-2,"Twenty-four stabilizers retain old negative fractional accumulation");
        for(var offset:List.of(new BlockPos(0,-3,0),new BlockPos(13,0,0),new BlockPos(0,6,2),new BlockPos(0,-11,2)))level.setBlock(origin.offset(offset),Blocks.SKELETON_SKULL.defaultBlockState(),18);
        test.assertTrue(InfusionLayout.scan(level,origin).symmetry()==-2,"Out-of-scan and central column decorations have no effect");
        for(int i=0;i<16;i++)test.assertTrue(TCArcane.candle(i).defaultBlockState().is(InfusionLayout.STABILIZERS),"All old candle colors stabilize");
        for(var shard:ShardType.values())test.assertTrue(TCArcane.crystal(shard).defaultBlockState().is(InfusionLayout.STABILIZERS),"All old crystal clusters stabilize");
        test.assertTrue(TCEldritch.CRYSTAL.get().defaultBlockState().is(InfusionLayout.STABILIZERS)&&!Blocks.DRAGON_HEAD.defaultBlockState().is(InfusionLayout.STABILIZERS)&&!Blocks.PIGLIN_HEAD.defaultBlockState().is(InfusionLayout.STABILIZERS),"Old eighth crystal retained; newer heads are not added");
        tc4revival.TC4Revival.LOGGER.info("PASS infusionLayoutRetainsScanOrderBoundsAndFloatSymmetry");test.succeed();
    }

    @GameTest(template="wand_test_empty")
    public static void infusionInstabilityOutcomesRetainDropsFluidsDamageAndEffects(GameTestHelper test) {
        var level=test.getLevel();var origin=test.absolutePos(new BlockPos(3,10,3));var at=origin.offset(4,-2,0);
        var drops=new ArrayList<ItemStack>();var explosions=new ArrayList<Vec3>();
        Consumer<EntityJoinLevelEvent> capture=event->{if(event.getLevel()==level&&event.getEntity() instanceof ItemEntity item&&item.position().distanceToSqr(at.getCenter())<4){drops.add(item.getItem().copy());event.setCanceled(true);}};
        Consumer<ExplosionEvent.Start> blast=event->{if(event.getLevel()==level&&event.getExplosion().getPosition().distanceToSqr(origin.getCenter())<100){test.assertTrue(!event.getExplosion().interactsWithBlocks(),"Old instability explosions do not damage terrain");explosions.add(event.getExplosion().getPosition());}};
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST,capture);MinecraftForge.EVENT_BUS.addListener(blast);
        int[] ejectType={0,2,0,-1,5,-1,1,4,-1,-1,0,2,-1,0,-1,5,-1,1,-1,3};int cases=0;
        try {
            for(int event=0;event<20;event++) {
                var pedestal=pedestal(level,at,64);var before=pedestal.getItem(0).copy();drops.clear();explosions.clear();
                level.setBlock(at.above(),Blocks.STONE.defaultBlockState(),18);
                var first=EntityType.SHEEP.create(level);var second=EntityType.SHEEP.create(level);var outside=EntityType.SHEEP.create(level);
                first.moveTo(origin.getCenter().add(-3,0,0));second.moveTo(origin.getCenter().add(-4,0,0));outside.moveTo(origin.getCenter().add(-12,0,0));
                first.setNoAi(true);second.setNoAi(true);outside.setNoAi(true);level.addFreshEntity(first);level.addFreshEntity(second);level.addFreshEntity(outside);
                float h1=first.getHealth(),h2=second.getHealth(),h3=outside.getHealth();
                try {
                    InfusionInstability.apply(level,origin,List.of(at),event);int type=ejectType[event];
                    if(type>=0) {
                        boolean drop=type<3||type==5;
                        test.assertTrue(pedestal.isEmpty()&&drops.size()==(drop?1:0),"Weighted event ejects one whole stack or destroys it: "+event);
                        if(drop)test.assertTrue(ItemStack.matches(drops.get(0),before),"Ejection retains all 64 items and NBT: "+event);
                        var upper=level.getBlockState(at.above());
                        if(type==1||type==3)test.assertTrue(upper.is(TCArcane.FLUX_GOO.get())&&upper.getValue(FiniteFluidBlock.QUANTA)==8,"Goo overwrites above pedestal at legacy metadata seven");
                        else if(type==2||type==4)test.assertTrue(upper.is(TCArcane.FLUX_GAS.get())&&upper.getValue(FiniteFluidBlock.QUANTA)==8,"Gas overwrites above pedestal at legacy metadata seven");
                        else test.assertTrue(upper.is(Blocks.STONE),"Other ejection preserves above block");
                        test.assertTrue(explosions.size()==(type==5?1:0)&&(type!=5||explosions.get(0).equals(at.getCenter())),"Ejection explosion weight and pedestal origin");
                    } else {
                        test.assertTrue(ItemStack.matches(pedestal.getItem(0),before)&&drops.isEmpty(),"Non-ejection does not consume pedestal contents");
                        if(Set.of(3,8,12,14).contains(event)) {
                            var deltas=List.of(h1-first.getHealth(),h2-second.getHealth());int hit=0;
                            for(float damage:deltas){if(damage!=0){test.assertTrue(damage>=4&&damage<=7,"Magic zap old damage range");hit++;}}
                            test.assertTrue(hit==(event==12?2:1),"One/all zap target count: "+event);
                        } else if(Set.of(5,16,18).contains(event)) {
                            int affected=0;for(var sheep:List.of(first,second)) {
                                var taint=sheep.getEffect(TCEffects.FLUX_TAINT.get());var vis=sheep.getEffect(TCEffects.VIS_EXHAUST.get());
                                if(taint!=null){affected++;test.assertTrue(taint.getDuration()==120&&taint.getAmplifier()==0&&!taint.isAmbient(),"Taint poison duration and visibility");}
                                if(vis!=null){affected++;test.assertTrue(vis.getDuration()==2400&&vis.getAmplifier()==0&&vis.isAmbient()&&vis.getCurativeItems().isEmpty(),"Vis exhaustion ambient and uncurable");}
                            }
                            test.assertTrue(affected==(event==18?2:1),"One/all harm target count: "+event);
                        } else if(event==9)test.assertTrue(explosions.size()==1&&explosions.get(0).equals(origin.getCenter())&&level.getBlockState(at.above()).is(Blocks.STONE),"Matrix explosion origin and no terrain damage");
                    }
                    test.assertTrue(outside.getHealth()==h3&&outside.getActiveEffects().isEmpty(),"Outside player-sized query unaffected");cases++;
                }finally{first.discard();second.discard();outside.discard();}
            }
            InfusionInstability.apply(level,origin,List.of(),0);
        }finally{MinecraftForge.EVENT_BUS.unregister(capture);MinecraftForge.EVENT_BUS.unregister(blast);}
        tc4revival.TC4Revival.LOGGER.info("PASS infusionInstabilityOutcomesRetainDropsFluidsDamageAndEffects: {} weighted events",cases);test.succeed();
    }

    @GameTest(template="wand_test_empty")
    public static void infusionWarpResetsCounterAndHonorsConfiguration(GameTestHelper test) {
        var level=test.getLevel();var origin=test.absolutePos(new BlockPos(3,5,3));
        var player=new net.minecraftforge.common.util.FakePlayer(level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"InfusionWarpAudit"));player.moveTo(origin.getCenter());level.addNewPlayer(player);
        var store=KnowledgeSavedData.get(level.getServer());var config=tc4revival.config.TCConfig.SERVER.disableWarp;boolean disabled=config.get();int sticky=0,temporary=0;
        try {
            config.set(false);
            for(int i=0;i<64;i++) {
                store.update(player.getUUID(),bound->0,k->{k.setWarp(PlayerKnowledge.WarpKind.PERMANENT,3);k.setWarp(PlayerKnowledge.WarpKind.STICKY,0);k.setWarp(PlayerKnowledge.WarpKind.TEMPORARY,0);k.setWarpCounter(0);return null;});
                InfusionInstability.apply(level,origin,List.of(),20);var result=store.snapshot(player.getUUID()).orElseThrow();
                int s=result.warp(PlayerKnowledge.WarpKind.STICKY),t=result.warp(PlayerKnowledge.WarpKind.TEMPORARY);
                test.assertTrue(s==1&&t==0||s==0&&t>=1&&t<=5,"Sticky one or temporary one-to-five, never permanent");
                test.assertTrue(result.warp(PlayerKnowledge.WarpKind.PERMANENT)==3&&result.warpCounter()==3+s+t,"Both warp paths reset event counter to total");
                if(s>0)sticky++;else temporary++;
            }
            test.assertTrue(sticky>0&&temporary>0,"Both random warp branches exercised");
            config.set(true);long revision=store.revision(player.getUUID());InfusionInstability.apply(level,origin,List.of(),20);
            test.assertTrue(store.revision(player.getUUID())==revision,"Disabled warp leaves stored state unchanged");
            config.set(false);player.moveTo(origin.getCenter().add(12,0,0));InfusionInstability.apply(level,origin,List.of(),20);
            test.assertTrue(store.revision(player.getUUID())==revision,"Out-of-range player cannot receive instability warp");
        }finally{player.discard();config.set(disabled);}
        tc4revival.TC4Revival.LOGGER.info("PASS infusionWarpResetsCounterAndHonorsConfiguration: {} sticky, {} temporary",sticky,temporary);test.succeed();
    }

    @GameTest(template="wand_test_empty")
    public static void infusionFailureAndMissingCostsRetainLegacyOrdering(GameTestHelper test) {
        var air=TCAspects.catalog().require("aer");var fire=TCAspects.catalog().require("ignis");
        class Host implements InfusionProcess.Host {
            boolean valid=true,available;int refresh,finished;final List<String> calls=new ArrayList<>();
            public boolean centerMatches(ItemStack expected){return valid;}
            public InfusionProcess.Experience drainExperience(){throw new AssertionError("Ordinary transaction");}
            public boolean drainEssentia(Aspect aspect){calls.add(aspect.tag());return available;}
            public List<ArcanePedestalBlockEntity> pedestals(){return List.of();}
            public void ingredientStarted(BlockPos position){throw new AssertionError("All components missing");}
            public void instabilityEvent(int event){calls.add("event:"+event);}
            public void refreshSurroundings(){refresh++;}
            public void failed(){calls.add("failed");}
            public void finish(InfusionOutput output,String player){finished++;}
            public void changed(){}
        }
        for(int seed=0;seed<64;seed++) {
            var process=new InfusionProcess();var random=net.minecraft.util.RandomSource.create(seed);var expected=net.minecraft.util.RandomSource.create(seed);var host=new Host();
            process.start(new InfusionRecipe.Plan(List.of(),new AspectAmounts().add(air,1),500,0,false,InfusionOutput.item(new ItemStack(Items.DIAMOND))),new ItemStack(Items.IRON_INGOT),"owner",0);
            expected.nextInt(500);int first=expected.nextInt(21);process.cycle(random,host);
            test.assertTrue(host.calls.equals(List.of("event:"+first))&&process.crafting()&&process.remaining().amount(air)==1,"Instability uses weighted event and suspends this cycle without payment");
            host.valid=false;process.cycle(random,host);
            test.assertTrue(host.calls.equals(List.of("event:"+first,"failed"))&&!process.crafting()&&process.instability()==0&&process.remaining().isEmpty(),"Invalid center cancels without destroying other items through an instability event");
            process.cycle(random,host);test.assertTrue(host.calls.size()==2&&host.finished==0,"Cancelled recipe never pays or publishes again");
        }
        var ingredient=new InfusionIngredient(com.google.gson.JsonParser.parseString("{\"ingredient\":{\"item\":\"minecraft:diamond\"}}").getAsJsonObject());
        var process=new InfusionProcess();var host=new Host();var random=net.minecraft.util.RandomSource.create(93);
        process.start(new InfusionRecipe.Plan(List.of(ingredient),new AspectAmounts().add(air,1).add(fire,1),0,0,false,InfusionOutput.item(new ItemStack(Items.GOLD_INGOT))),new ItemStack(Items.IRON_INGOT),"owner",-1000);
        process.cycle(random,host);
        test.assertTrue(host.calls.equals(List.of("aer","ignis"))&&host.refresh==1&&process.remaining().total()==2,"Missing sources are all attempted in order, then layout discovery requested");
        host.available=true;process.cycle(random,host);process.cycle(random,host);
        test.assertTrue(process.remaining().total()==0&&process.remaining().size()==2,"Successful payment retains zero-valued old aspect entries");
        process.cycle(random,host);
        test.assertTrue(process.remaining().total()==1&&host.finished==0,"First missing ingredient always re-adds one already-paid aspect");
        process.cycle(random,host);test.assertTrue(process.remaining().total()==0,"Penalty is paid before retrying ingredient");
        for(int malformed:new int[]{Integer.MIN_VALUE,Integer.MAX_VALUE}) {
            var saved=process.save();saved.putInt("RecipeInstability",malformed);process.load(saved);process.cycle(random,host);
            test.assertTrue(process.remaining().total()==1&&process.crafting(),"Extreme saved recipe instability cannot overflow random bounds");
            process.cycle(random,host);
        }
        var packet=new tc4revival.network.FocusEffectPacket(test.getLevel().dimension().location(),tc4revival.network.FocusEffectPacket.Kind.NODE_ZAP,new Vec3(1,2,3),new Vec3(4,5,6),1,0);
        var buffer=new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        try{packet.encode(buffer);test.assertTrue(tc4revival.network.FocusEffectPacket.decode(buffer).equals(packet),"Node zap kind and endpoints round-trip independently of regular lightning");}finally{buffer.release();}
        tc4revival.TC4Revival.LOGGER.info("PASS infusionFailureAndMissingCostsRetainLegacyOrdering: 64 cancellation seeds and missing-cost recovery");test.succeed();
    }
}
