package tc4revival.content;

import java.util.EnumMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import tc4revival.aspect.Aspect;
import tc4revival.aspect.TCAspects;

/** Integration and branch cases reserved for the complete wand-system server checkpoint. */
@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public  class EssentiaTubeServerTests {
    private EssentiaTubeServerTests() {}
    @GameTest(template="wand_test_empty")
    public static void alembicToJarNetworkConservesEveryUnit(GameTestHelper test) {
        var sourcePos=new BlockPos(2,2,2); var firstPos=sourcePos.east(); var secondPos=firstPos.east(); var jarPos=secondPos.below();
        test.setBlock(sourcePos,TCArcane.ALEMBIC.get()); test.setBlock(firstPos,TCArcane.ESSENTIA_TUBE.get());
        test.setBlock(secondPos,TCArcane.ESSENTIA_TUBE.get()); test.setBlock(jarPos,TCArcane.ESSENTIA_JAR.get());
        var source=(AlembicBlockEntity)test.getBlockEntity(sourcePos); var first=(EssentiaTubeBlockEntity)test.getBlockEntity(firstPos);
        var second=(EssentiaTubeBlockEntity)test.getBlockEntity(secondPos); var jar=(EssentiaJarBlockEntity)test.getBlockEntity(jarPos);
        var air=TCAspects.catalog().require("aer"); source.addToContainer(air,16);
        for (int tick=0;tick<180;tick++) {
            first.tick(); second.tick(); jar.tick();
            test.assertTrue(source.amount()+first.essentiaAmount(null)+second.essentiaAmount(null)+jar.amount()==16,"A pipe tick may move essentia but cannot create or discard it");
        }
        test.assertTrue(jar.amount()==16 && source.amount()==0,"Unfiltered jar suction must drain an alembic through the two-pipe bend");
        test.assertTrue(first.suction(null)==30 && second.suction(null)==31,"Suction loses one point at each ordinary pipe");
        test.succeed();
    }
    @GameTest(template="wand_test_empty")
    public static void filteringRestrictionAndOnewayUseDifferentSuctionRules(GameTestHelper test) {
        var air=TCAspects.catalog().require("aer"); var fire=TCAspects.catalog().require("ignis");
        var host=new Host(); host.neighbors.put(Direction.WEST,new Endpoint(air,31)); host.neighbors.put(Direction.EAST,new Endpoint(fire,64));
        var filtered=new EssentiaTubeFlow(EssentiaTubeFlow.Kind.FILTER,host); filtered.filter(air); filtered.calculateSuction();
        test.assertTrue(filtered.suction(null)==30 && filtered.suctionType(null).equals(air),"A stronger incompatible suction must not win through a labeled filter");
        var restricted=new EssentiaTubeFlow(EssentiaTubeFlow.Kind.RESTRICTED,host); restricted.calculateSuction();
        test.assertTrue(restricted.suction(null)==32 && restricted.suctionType(null).equals(fire),"Restriction halves incoming suction instead of subtracting one");
        var oneway=new EssentiaTubeFlow(EssentiaTubeFlow.Kind.ONEWAY,host); oneway.face(Direction.EAST); oneway.calculateSuction();
        test.assertTrue(oneway.suction(null)==30 && oneway.suctionType(null).equals(air),"One-way suction comes exclusively from the face opposite its orientation");
        host.neighbors.get(Direction.WEST).type=null; filtered.calculateSuction();
        test.assertTrue(filtered.suctionType(null).equals(air),"A labeled filter assigns its type to an untyped suction"); test.succeed();
    }
    @GameTest(template="wand_test_empty")
    public static void valvePowerEdgesAndManualOverridesSurviveSave(GameTestHelper test) {
        var host=new Host(); host.neighbors.put(Direction.WEST,new Endpoint(null,32));
        var valve=new EssentiaTubeFlow(EssentiaTubeFlow.Kind.VALVE,host); valve.face(Direction.EAST); valve.calculateSuction();
        test.assertTrue(valve.suction(null)==31 && !valve.connectable(Direction.EAST),"A valve handle occupies its facing port");
        host.powered=true; for (int tick=0;tick<20;tick++) valve.tick();
        test.assertTrue(!valve.allowFlow() && valve.suction(null)==0,"A redstone rising edge closes the valve and clears suction");
        valve.toggleValve(); for (int tick=0;tick<20;tick++) valve.tick();
        test.assertTrue(valve.allowFlow(),"Constant power must not undo a manual override");
        valve.toggleValve(); var restored=new EssentiaTubeFlow(EssentiaTubeFlow.Kind.VALVE,host); restored.load(valve.save(true),true);
        host.powered=false; for (int tick=0;tick<20;tick++) restored.tick();
        test.assertTrue(restored.allowFlow() && restored.facing()==Direction.EAST,"The saved powered state must detect a falling edge after reload"); test.succeed();
    }
    @GameTest(template="wand_test_empty")
    public static void bufferPriorityChokesAndClosedIntakeMatchLegacy(GameTestHelper test) {
        var air=TCAspects.catalog().require("aer"); var fire=TCAspects.catalog().require("ignis");
        var host=new Host(); host.bellows=2; var buffer=new EssentiaTubeFlow(EssentiaTubeFlow.Kind.BUFFER,host); buffer.tick();
        for (int unit=0;unit<8;unit++) test.assertTrue(buffer.addEssentia(unit%2==0 ? air : fire,1,Direction.UP)==1,"Buffer holds eight mixed units");
        test.assertTrue(buffer.addEssentia(air,1,Direction.UP)==0,"A full buffer must reject additional essentia");
        host.neighbors.put(Direction.EAST,new Endpoint(null,64)); host.neighbors.put(Direction.WEST,new Endpoint(air,128));
        test.assertTrue(buffer.takeEssentia(air,1,Direction.EAST)==0,"A lower-suction requester must yield to an eligible stronger branch");
        test.assertTrue(buffer.takeEssentia(fire,1,Direction.EAST)==1,"An air-only stronger branch cannot reserve fire");
        buffer.cycleChoke(Direction.NORTH); buffer.cycleChoke(Direction.SOUTH); buffer.cycleChoke(Direction.SOUTH);
        test.assertTrue(buffer.suction(Direction.UP)==64 && buffer.suction(Direction.NORTH)==1 && buffer.suction(Direction.SOUTH)==0,"Bellows and the three choke positions set independent face suction");
        var loaded=new EssentiaTubeFlow(EssentiaTubeFlow.Kind.BUFFER,host); loaded.load(buffer.save(true),true);
        test.assertTrue(loaded.contents().view().equals(buffer.contents().view()) && loaded.choke(Direction.SOUTH)==2,"Mixed contents and choke positions must survive serialization");
        var intakeHost=new Host(); intakeHost.bellows=1; var source=new Endpoint(air,0); source.amount=1; intakeHost.neighbors.put(Direction.WEST,source);
        var intake=new EssentiaTubeFlow(EssentiaTubeFlow.Kind.BUFFER,intakeHost); intake.setOpen(Direction.WEST,false);
        for (int tick=0;tick<5;tick++) intake.tick();
        test.assertTrue(intake.essentiaAmount(null)==0 && source.amount==1,"Closed intake preserves the source");
        intake.setOpen(Direction.WEST,true);for(int tick=0;tick<5;tick++)intake.tick();
        test.assertTrue(intake.essentiaAmount(null)==1 && source.amount==0,"Reopened intake transfers one unit"); test.succeed();
    }
    @GameTest(template="wand_test_empty")
    public static void incompatibleSuctionVentsWithoutDestroyingStoredEssentia(GameTestHelper test) {
        var air=TCAspects.catalog().require("aer"); var fire=TCAspects.catalog().require("ignis");
        var host=new Host(); host.neighbors.put(Direction.DOWN,new Endpoint(air,32)); host.neighbors.put(Direction.UP,new Endpoint(fire,31));
        var pipe=new EssentiaTubeFlow(EssentiaTubeFlow.Kind.NORMAL,host); pipe.addEssentia(air,1,Direction.NORTH);
        for (int tick=0;tick<4;tick++) pipe.tick();
        test.assertTrue(host.vents>0 && pipe.venting()>0,"Adjacent incompatible suction within one point must trigger venting");
        test.assertTrue(pipe.essentiaAmount(null)==1 && pipe.essentiaType(null).equals(air),"Venting pauses flow but does not discard the stored unit"); test.succeed();
    }
    @GameTest(template="wand_test_empty")
    public static void closedArmsRemainTraceableAndBufferEmitsComparatorStrength(GameTestHelper test) {
        var pos=new BlockPos(2,2,2); test.setBlock(pos,TCArcane.ESSENTIA_BUFFER.get()); test.setBlock(pos.east(),TCArcane.ESSENTIA_TUBE.get());
        var buffer=(EssentiaTubeBlockEntity)test.getBlockEntity(pos); buffer.flow().setOpen(Direction.EAST,false);
        test.assertTrue(EssentiaTubeBlock.parts(buffer).containsKey(Direction.EAST.get3DDataValue()),"Closed arms remain selectable for reopening with a wand");
        var air=TCAspects.catalog().require("aer"); for (int unit=0;unit<8;unit++) buffer.addEssentia(air,1,Direction.UP);
        var world=test.getLevel(); var absolute=test.absolutePos(pos);
        test.assertTrue(TCArcane.ESSENTIA_BUFFER.get().getAnalogOutputSignal(buffer.getBlockState(),world,absolute)==15,"Eight stored units produce comparator strength 15");
        buffer.load(buffer.saveWithoutMetadata()); test.assertTrue(!buffer.connectable(Direction.EAST) && buffer.essentiaAmount(null)==8,"World adapter persists closed ports and mixed storage"); test.succeed();
    }
    private static  class Host implements EssentiaTubeFlow.Host {
        final EnumMap<Direction,Endpoint> neighbors=new EnumMap<>(Direction.class); final RandomSource random=RandomSource.create(1234);
        boolean powered; int bellows,vents;
        @Override public EssentiaTransport neighbor(Direction side) { return neighbors.get(side); }
        @Override public RandomSource random() { return random; }
        @Override public boolean powered() { return powered; }
        @Override public int bellows() { return bellows; }
        @Override public void changed() {}
        @Override public void valveSound() {}
        @Override public void creak() {}
        @Override public void vent(Aspect type) { vents++; }
    }
    private static  class Endpoint implements EssentiaTransport {
        Aspect type; int suction,amount;
        Endpoint(Aspect type,int suction) { this.type=type; this.suction=suction; }
        @Override public boolean connectable(Direction side) { return true; }
        @Override public boolean canInput(Direction side) { return true; }
        @Override public boolean canOutput(Direction side) { return true; }
        @Override public Aspect suctionType(Direction side) { return type; }
        @Override public int suction(Direction side) { return suction; }
        @Override public int minimumSuction() { return 0; }
        @Override public Aspect essentiaType(Direction side) { return type; }
        @Override public int essentiaAmount(Direction side) { return amount; }
        @Override public int takeEssentia(Aspect selected,int requested,Direction side) { int taken=java.util.Objects.equals(selected,type) ? Math.min(requested,amount) : 0; amount-=taken; return taken; }
        @Override public int addEssentia(Aspect selected,int quantity,Direction side) { return 0; }
    }
}
