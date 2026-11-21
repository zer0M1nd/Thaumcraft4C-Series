package tc4revival.content;

import java.util.ArrayList;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import tc4revival.aspect.Aspect;
import tc4revival.aspect.AspectAmounts;
import tc4revival.aspect.TCAspects;

@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public  class AdvancedAlchemyServerTests {
    private AdvancedAlchemyServerTests() {}
    private static  class Host implements AdvancedAlchemyProcess.Host {
        final ArrayList<String> requests=new ArrayList<>();
        boolean supply=true;
        @Override public int drain(Aspect aspect,int requested) { requests.add(aspect.tag()+":"+requested); return supply ? requested : 0; }
        @Override public void changed(boolean sync) {}
    }
    @GameTest(template="wand_test_empty")
    public static void powerRequestsCostsAndPostPaymentDelay(GameTestHelper test) {
        var host=new Host(); var process=new AdvancedAlchemyProcess();
        for (int i=0;i<4;i++) process.tick(host);
        test.assertTrue(host.requests.isEmpty(),"Network charging happens only every fifth tick");
        process.tick(host);
        test.assertTrue(host.requests.equals(java.util.List.of("ignis:50","perditio:50","aqua:50"))
                && process.heat()==50 && process.entropy()==50 && process.water()==50,"Cold heat stays nonnegative before ordered charging");
        var aspect=TCAspects.catalog().require("aer");
        test.assertTrue(!process.process(new AspectAmounts().add(aspect,26),host),"Insufficient double heat must reject an input without spending power");
        test.assertTrue(process.process(new AspectAmounts().add(aspect,20),host)
                && process.heat()==10 && process.entropy()==30 && process.water()==30 && process.amount()==20,
                "One item spends twice its aspect total in heat and once in each other power");
        test.assertTrue(process.cooldown()==(int)(5f+(1f-10f/500f)*100f),"Delay uses remaining heat after payment");
        test.assertTrue(!process.process(new AspectAmounts().add(aspect,1),host),"Further collisions must wait for the processing delay");
        test.succeed();
    }
    @GameTest(template="wand_test_empty")
    public static void thresholdOvershootReloadAndSafeEmptyOutput(GameTestHelper test) {
        var tag=new CompoundTag(); tag.putShort("heat",(short)501); tag.putShort("power1",(short)500); tag.putShort("power2",(short)500);
        var process=new AdvancedAlchemyProcess(); process.load(tag); var host=new Host();
        for (int i=0;i<5;i++) process.tick(host);
        test.assertTrue(process.heat()==500 && process.entropy()==500 && process.water()==500 && host.requests.isEmpty(),"Full power rejects additional charge");
        var air=TCAspects.catalog().require("aer"); var fire=TCAspects.catalog().require("ignis");
        test.assertTrue(process.firstAspect()==null && !process.take(air,1,host),"Empty output is safe to inspect and drain");
        process.process(new AspectAmounts().add(air,2).add(fire,3),host);
        test.assertTrue(process.firstAspect().equals(air) && !process.take(air,3,host) && !process.take(air,-1,host),"Nozzles preserve insertion order and reject excess or negative removal");
        test.assertTrue(process.take(air,2,host) && process.firstAspect().equals(fire),"Draining the first aspect advances to the next stored type");
        var saved=new CompoundTag(); process.save(saved); var restored=new AdvancedAlchemyProcess(); restored.load(saved);
        test.assertTrue(restored.amount()==3 && restored.heat()==490 && restored.entropy()==495
                && restored.water()==495 && restored.cooldown()==process.cooldown(),"Reload preserves power, contents and pending cooldown");
        test.succeed();
    }
    @GameTest(template="wand_test_empty")
    public static void actualFormationNozzlesAndDismantling(GameTestHelper test) {
        var level=test.getLevel(); var origin=test.absolutePos(new net.minecraft.core.BlockPos(3,1,3));
        for (int x=-1;x<=1;x++) for (int y=0;y<=1;y++) for (int z=-1;z<=1;z++) {
            int part=AdvancedAlchemyStructure.part(x,y,z);
            if (part>=0) level.setBlock(origin.offset(x,y,z),AdvancedAlchemyStructure.restored(part),18);
        }
        level.setBlock(origin.above(),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),18);
        test.assertTrue(AdvancedAlchemyStructure.matches(level,origin),"The original blueprint does not require an empty top centre");
        var base=(AlchemyFurnaceBlockEntity)level.getBlockEntity(origin);
        base.setItem(0,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_INGOT,7));
        var player=net.minecraftforge.common.util.FakePlayerFactory.get(level,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"FurnaceFormation"));
        player.setPos(origin.getCenter());
        tc4revival.research.KnowledgeSavedData.get(level.getServer()).update(player.getUUID(),player.getRandom()::nextInt,k->k.rememberResearch("ADVALCHEMYFURNACE"));
        var wand=TCItems.CASTING_WAND.get().create(WandParts.Rod.SILVERWOOD,WandParts.Cap.GOLD,false,true);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,wand);
        var clicked=origin.east();
        var context=new net.minecraft.world.item.context.UseOnContext(player,net.minecraft.world.InteractionHand.MAIN_HAND,
                new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(clicked),net.minecraft.core.Direction.EAST,clicked,false));
        test.assertTrue(TCItems.CASTING_WAND.get().onItemUseFirst(wand,context).consumesAction(),"The actual wand must build the advanced furnace");
        test.assertTrue(level.getBlockState(origin.above()).is(net.minecraft.world.level.block.Blocks.STONE),"Formation leaves the unconstrained top centre untouched");
        test.assertTrue(TCItems.CASTING_WAND.get().vis(wand,TCAspects.catalog().require("ignis"))==5000
                && TCItems.CASTING_WAND.get().vis(wand,TCAspects.catalog().require("aqua"))==5000
                && TCItems.CASTING_WAND.get().vis(wand,TCAspects.catalog().require("ordo"))==5000,"Formation charges all three original 50-point costs");
        int dropped=level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(origin).inflate(2))
                .stream().map(net.minecraft.world.entity.item.ItemEntity::getItem).filter(stack->stack.is(net.minecraft.world.item.Items.IRON_INGOT)).mapToInt(net.minecraft.world.item.ItemStack::getCount).sum();
        test.assertTrue(dropped==7,"Successful formation returns the base furnace inventory exactly once");
        var core=(AdvancedAlchemyBlockEntity)level.getBlockEntity(origin); var data=new CompoundTag();
        data.putShort("heat",(short)500); data.putShort("power1",(short)500); data.putShort("power2",(short)500); core.process().load(data);
        var air=TCAspects.catalog().require("aer"); core.process().process(new AspectAmounts().add(air,4),core);
        for (var side:net.minecraft.core.Direction.Plane.HORIZONTAL) {
            var nozzle=(AdvancedAlchemyBlockEntity)level.getBlockEntity(origin.relative(side));
            test.assertTrue(nozzle.controller()==core && nozzle.canOutput(side) && !nozzle.canOutput(side.getOpposite())
                    && nozzle.takeEssentia(air,1,side)==1,"Each lower nozzle must drain the shared core only outward");
        }
        var nozzle=(AdvancedAlchemyBlockEntity)level.getBlockEntity(origin.east());
        test.assertTrue(nozzle.essentiaType(net.minecraft.core.Direction.EAST)==null && nozzle.essentiaAmount(net.minecraft.core.Direction.EAST)==0,
                "Draining the final unit leaves a safe empty output");
        level.destroyBlock(origin.offset(1,1,1),false); core.tick();
        test.assertTrue(level.getBlockState(origin).is(TCArcane.ALCHEMY_FURNACE.get())
                && level.getBlockState(origin.east()).is(TCArcane.ADVANCED_ALCHEMICAL_CONSTRUCT.get())
                && level.getBlockState(origin.above().east()).is(TCArcane.ALCHEMICAL_CONSTRUCT.get()),"Breaking an upper corner restores the surviving components on the next core tick");
        tc4revival.TC4Revival.LOGGER.info("actualFormationNozzlesAndDismantling PASS");
        test.succeed();
    }
    @GameTest(template="wand_test_empty")
    public static void failedConversionRestoresAllInventoriesWithoutDrops(GameTestHelper test) {
        var level=test.getLevel(); var origin=test.absolutePos(new net.minecraft.core.BlockPos(3,1,3));
        var states=new java.util.LinkedHashMap<net.minecraft.core.BlockPos,net.minecraft.world.level.block.state.BlockState>();
        var saved=new java.util.LinkedHashMap<net.minecraft.core.BlockPos,CompoundTag>();
        for (int x=-1;x<=1;x++) for (int y=0;y<=1;y++) for (int z=-1;z<=1;z++) {
            int part=AdvancedAlchemyStructure.part(x,y,z); if (part<0) continue;
            var pos=origin.offset(x,y,z); level.setBlock(pos,AdvancedAlchemyStructure.restored(part),18);
            if (level.getBlockEntity(pos) instanceof AlembicBlockEntity alembic) alembic.addToContainer(TCAspects.catalog().require("aqua"),13);
        }
        var base=(AlchemyFurnaceBlockEntity)level.getBlockEntity(origin);
        base.setItem(0,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_INGOT,7));
        base.setItem(1,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.COAL,3));
        for (int x=-1;x<=1;x++) for (int y=0;y<=1;y++) for (int z=-1;z<=1;z++) {
            if (AdvancedAlchemyStructure.part(x,y,z)<0) continue;
            var pos=origin.offset(x,y,z); states.put(pos,level.getBlockState(pos));
            var entity=level.getBlockEntity(pos); if (entity!=null) saved.put(pos,entity.saveWithFullMetadata());
        }
        for (int failure=0;failure<states.size();failure++) {
            int failAt=failure; int[] writes={0},charges={0};
            boolean converted=AdvancedAlchemyStructure.convert(level,origin,states,saved,()->{ charges[0]++; return true; },(pos,state)->{
                boolean written=level.setBlock(pos,state,18); return writes[0]++==failAt ? false : written;
            });
            test.assertTrue(!converted && charges[0]==0,"Every failed write must roll back before charging");
            assertRestored(test,origin,states,saved);
        }
        int[] charges={0};
        test.assertTrue(!AdvancedAlchemyStructure.convert(level,origin,states,saved,()->{ charges[0]++; return false; },
                (pos,state)->level.setBlock(pos,state,18)) && charges[0]==1,"A payment rejection must undo the complete structure");
        assertRestored(test,origin,states,saved);
        var interruption=new IllegalStateException("injected conversion interruption"); int[] writes={0};
        try {
            AdvancedAlchemyStructure.convert(level,origin,states,saved,()->true,(pos,state)->{
                boolean written=level.setBlock(pos,state,18); if (++writes[0]==12) throw interruption; return written;
            });
            test.assertTrue(false,"The injected exception must propagate");
        } catch (IllegalStateException expected) { test.assertTrue(expected==interruption,"Rollback must preserve the original exception"); }
        assertRestored(test,origin,states,saved);
        test.assertTrue(AdvancedAlchemyStructure.convert(level,origin,states,saved,()->true,(pos,state)->level.setBlock(pos,state,18)),
                "A successful retry must remain possible after all rollback paths");
        test.succeed();
    }
    @GameTest(template="wand_test_empty")
    public static void thrownItemConsumesOneAndExposesItsResolvedAspects(GameTestHelper test) {
        var level=test.getLevel(); var pos=test.absolutePos(new net.minecraft.core.BlockPos(3,1,3));
        var state=TCArcane.ADVANCED_ALCHEMY_FURNACE.get().defaultBlockState(); level.setBlock(pos,state,18);
        var core=(AdvancedAlchemyBlockEntity)level.getBlockEntity(pos);
        var power=new CompoundTag(); power.putShort("heat",(short)500); power.putShort("power1",(short)500); power.putShort("power2",(short)500); core.process().load(power);
        var stack=new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_INGOT,3);
        var aspects=AlchemyFurnaceBlockEntity.inputAspects(stack);
        test.assertTrue(!aspects.isEmpty() && aspects.total()>0 && aspects.total()<=250,"The real item-aspect definitions must be loaded for this scenario");
        var item=new net.minecraft.world.entity.item.ItemEntity(level,pos.getX()+.5,pos.getY()+.8,pos.getZ()+.5,stack);
        level.addFreshEntity(item);
        TCArcane.ADVANCED_ALCHEMY_FURNACE.get().entityInside(state,level,pos,item);
        test.assertTrue(item.isAlive() && item.getItem().getCount()==2 && core.process().contents().view().equals(aspects.view()),
                "A collision consumes one actual item and retains all its resolved aspects");
        test.assertTrue(core.process().heat()==500-2*aspects.total() && core.process().water()==500-aspects.total(),"Collision processing pays the actual item's total aspect cost");
        TCArcane.ADVANCED_ALCHEMY_FURNACE.get().entityInside(state,level,pos,item);
        test.assertTrue(item.getItem().getCount()==2,"Repeated collision in the same tick cannot bypass the processing delay");
        item.discard(); test.succeed();
    }
    private static void assertRestored(GameTestHelper test,net.minecraft.core.BlockPos origin,
            java.util.LinkedHashMap<net.minecraft.core.BlockPos,net.minecraft.world.level.block.state.BlockState> states,
            java.util.LinkedHashMap<net.minecraft.core.BlockPos,CompoundTag> saved) {
        var level=test.getLevel();
        for (var entry:states.entrySet()) test.assertTrue(level.getBlockState(entry.getKey()).equals(entry.getValue()),"Rollback restores every original block state");
        for (var entry:saved.entrySet()) {
            var entity=level.getBlockEntity(entry.getKey());
            test.assertTrue(entity!=null && entity.saveWithFullMetadata().equals(entry.getValue()),"Rollback restores exact furnace and alembic NBT");
        }
        test.assertTrue(level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(origin).inflate(2)).isEmpty(),
                "Failed conversion must not emit inventory or constituent item drops");
        test.assertTrue(!AdvancedAlchemyStructure.mutating(),"The conversion suppression scope must always be released");
    }
    @GameTest(template="wand_test_empty")
    public static void fullCapacityAndUnpoweredCooling(GameTestHelper test) {
        var process=new AdvancedAlchemyProcess(); var tag=new CompoundTag();
        var aspect=TCAspects.catalog().require("aer");
        tc4revival.aspect.AspectNbt.write(tag,new AspectAmounts().add(aspect,499));
        tag.putShort("heat",(short)50); tag.putShort("power1",(short)50); tag.putShort("power2",(short)50); process.load(tag);
        var host=new Host(); host.supply=false;
        test.assertTrue(!process.process(new AspectAmounts().add(aspect,2),host) && process.heat()==50,"Capacity rejection is atomic");
        test.assertTrue(process.process(new AspectAmounts().add(aspect,1),host) && process.amount()==500,"Exactly 500 units fit");
        var empty=new AdvancedAlchemyProcess(); for (int i=0;i<10;i++) empty.tick(host);
        test.assertTrue(empty.heat()==0,"Cooling cannot create negative heat"); test.succeed();
    }
}
