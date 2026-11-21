package automagy;

import automagy.logic.EssentiaLocusStructure;
import static automagy.logic.EssentiaLocusStructure.Cell.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class EssentiaLocusStructureTest {
    private EssentiaLocusStructure.Cell pool(int x,int y,int z){
        if(y==-1&&x==0&&z==0)return WATER_CLUSTER;
        if(y==-2){if(x==0&&z==0)return LAPIS;if(Math.abs(x)<=1&&Math.abs(z)<=1)return SOURCE_WATER;
            if(Math.abs(x)==2&&Math.abs(z)<=1||Math.abs(z)==2&&Math.abs(x)<=1)return SILVERWOOD;}
        return AIR;
    }
    @Test void placementConsumesCrystalAndRuntimeAllowsAggregator(){
        assertEquals(EssentiaLocusStructure.Validity.VALID,EssentiaLocusStructure.validate(this::pool,true));
        assertEquals(EssentiaLocusStructure.Validity.INVALID,EssentiaLocusStructure.validate(this::pool,false));
        assertEquals(EssentiaLocusStructure.Validity.VALID,EssentiaLocusStructure.validate((x,y,z)->x==0&&y==-1&&z==0?AGGREGATOR:pool(x,y,z),false));
    }
    @Test void missingChunksDoNotCountAsDestroyedAndPlacementNeedsSourceWater(){
        assertEquals(EssentiaLocusStructure.Validity.UNLOADED,EssentiaLocusStructure.validate((x,y,z)->x==-2?UNKNOWN:pool(x,y,z),true));
        assertEquals(EssentiaLocusStructure.Validity.INVALID,EssentiaLocusStructure.validate((x,y,z)->x==1&&y==-2&&z==1?FLOWING_WATER:pool(x,y,z),true));
        assertEquals(EssentiaLocusStructure.Validity.INVALID,EssentiaLocusStructure.validate((x,y,z)->x==1&&y==0&&z==1?OTHER:pool(x,y,z),true));
    }
    @Test void sharedCornerClusterPreservesTwoUpgradeContacts(){
        var base=EssentiaLocusStructure.capacity(this::pool);assertEquals(12,base.jars());assertEquals(1,base.eyes());assertEquals(20,base.distance());
        var upgraded=EssentiaLocusStructure.capacity((x,y,z)->x==2&&y==-2&&z==2?WATER_CLUSTER:pool(x,y,z));
        assertEquals(28,upgraded.jars());assertEquals(3,upgraded.eyes());assertEquals(36,upgraded.distance());
    }
}
