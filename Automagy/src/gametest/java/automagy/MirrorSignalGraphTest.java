package automagy;

import automagy.logic.SignalGraph;
import automagy.content.MirrorSignalLinks;
import net.minecraft.core.*;
import net.minecraft.world.level.Level;

import static automagy.gametest.Checks.*;

@net.minecraftforge.gametest.GameTestHolder("automagy")
@net.minecraftforge.gametest.PrefixGameTestTemplate(false)
public class MirrorSignalGraphTest {
    private static final GlobalPos LOCAL=GlobalPos.of(Level.OVERWORLD,BlockPos.ZERO),REMOTE=GlobalPos.of(Level.NETHER,BlockPos.ZERO);
    private static SignalGraph<GlobalPos> pair(int input){
        var graph=new SignalGraph<GlobalPos>();graph.node(LOCAL,input,n->n);graph.node(REMOTE,0,n->n);
        graph.edge(LOCAL,REMOTE,1,32767);graph.edge(REMOTE,LOCAL,1,32767);return graph;
    }
    @net.minecraft.gametest.framework.GameTest(template="empty",templateNamespace="automagy") public static void sameCoordinatesInDifferentDimensionsRemainSeparateAndLoseOneLevel(net.minecraft.gametest.framework.GameTestHelper h) {var power=pair(50).solve();assertEquals(50,power.get(LOCAL));assertEquals(49,power.get(REMOTE));h.succeed();}
    @net.minecraft.gametest.framework.GameTest(template="empty",templateNamespace="automagy") public static void removingTheExternalSourceClearsBothSidesWithoutSeedingOldPower(net.minecraft.gametest.framework.GameTestHelper h) {assertEquals(49,pair(50).solve().get(REMOTE));assertTrue(pair(0).solve().values().stream().allMatch(n->n==0));h.succeed();}
    @net.minecraft.gametest.framework.GameTest(template="empty",templateNamespace="automagy") public static void disconnectedMirrorsCannotCarryTheLastSolvedSignal(net.minecraft.gametest.framework.GameTestHelper h) {
        var graph=new SignalGraph<GlobalPos>();graph.node(LOCAL,15,n->n);graph.node(REMOTE,0,n->n);assertEquals(0,graph.solve().get(REMOTE));
    h.succeed();}
    @net.minecraft.gametest.framework.GameTest(template="empty",templateNamespace="automagy") public static void linkNbtPreservesDimensionAndNegativeHeight(net.minecraft.gametest.framework.GameTestHelper h) {
        var target=GlobalPos.of(Level.NETHER,new BlockPos(-99,-20,2048));assertEquals(target,MirrorSignalLinks.read(MirrorSignalLinks.write(target)));
        assertNull(MirrorSignalLinks.read(new net.minecraft.nbt.CompoundTag()));
    h.succeed();}
}
