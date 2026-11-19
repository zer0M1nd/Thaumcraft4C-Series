package automagy.logic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SignalGraphTest {
    @Test void amplifiedReaderSignalSurvivesCrystalsButNotVanillaWires(){
        var graph=new SignalGraph<String>();graph.node("reader",99,p->p);graph.node("crystal",0,p->p);graph.node("wire",0,p->Math.min(15,p));
        graph.edge("reader","crystal",1,99);graph.edge("crystal","wire",1,99);
        assertEquals(98,graph.solve().get("crystal"));assertEquals(15,graph.solve().get("wire"));
    }
    @Test void sourceRemovalExtinguishesEvenAmplifierLoops() {
        var powered = loop(1); assertEquals(49,powered.solve().get("wire"));
        var unpowered = loop(0); assertEquals(0,unpowered.solve().get("wire")); assertEquals(0,unpowered.solve().get("amp"));
    }
    private SignalGraph<String> loop(int source) {
        var graph = new SignalGraph<String>(); graph.node("amp",source,p->SignalGraph.transform("amp",0,p)); graph.node("wire",0,p->p);
        graph.edge("amp","wire",1,50);graph.edge("wire","amp",1,50);return graph;
    }
    @Test void capAndThresholdApplyAfterTransmissionLoss() {
        var graph = new SignalGraph<String>(); graph.node("source",15,p->p);
        graph.node("dim",0,p->SignalGraph.transform("dim",7,p));
        graph.node("dense",0,p->SignalGraph.transform("dense",7,p));
        graph.edge("source","dim",1,50);graph.edge("dim","dense",1,50);
        assertEquals(7,graph.solve().get("dim"));assertEquals(0,graph.solve().get("dense"));
    }
    @Test void ordinaryRedstoneClampsAmplifiedCrystalPower() {
        var graph = new SignalGraph<Integer>(); graph.node(0,50,p->p); graph.node(1,0,p->Math.min(15,p));graph.edge(0,1,1,50);
        assertEquals(15,graph.solve().get(1));
    }
    @Test void directionalEdgesNeverBackfeedAnAmplifierInput() {
        var graph = new SignalGraph<Integer>(); graph.node(0,0,p->SignalGraph.transform("amp",0,p)); graph.node(1,15,p->p);
        graph.edge(0,1,1,50);assertEquals(0,graph.solve().get(0));assertEquals(15,graph.solve().get(1));
    }
}
