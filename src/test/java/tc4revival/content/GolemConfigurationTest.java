package tc4revival.content;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class GolemConfigurationTest {
    @Test void absentAddonFieldsDoNotOverrideNativeCore() {
        var tag=new CompoundTag();tag.putByte("GolemType",(byte)7);tag.putByte("Core",(byte)5);
        var config=GolemConfiguration.load(tag);
        assertNull(config.customTask());assertNull(config.customMaterial());assertEquals(GolemCoreType.LIQUID,config.core());
    }
    @Test void savedEmptyPathIdsRecoverOnLoad() {
        for(String id:new String[]{"","minecraft:","thaumcraft:"," ","not valid"}) {
            var tag=new CompoundTag();tag.putString("AddonTask",id);tag.putString("AddonMaterial",id);
            var config=GolemConfiguration.load(tag);assertNull(config.customTask());assertNull(config.customMaterial());
        }
    }
    @Test void validAddonDefinitionsStillRoundTrip() {
        var tag=new CompoundTag();tag.putString("AddonTask","example:alchemy");tag.putString("AddonMaterial","example:stone");
        var config=GolemConfiguration.load(tag);var saved=new CompoundTag();config.save(saved);
        assertEquals("example:alchemy",saved.getString("AddonTask"));assertEquals("example:stone",saved.getString("AddonMaterial"));
    }
    @Test void boxDistanceUtilityStillMeasuresSurfaceGaps() {
        var golem=new net.minecraft.world.phys.AABB(.3,2,.3,.7,2.8,.7);
        assertTrue(GolemWorkRange.distanceSquared(golem,new net.minecraft.world.phys.AABB(2,1,0,3,2,1))<=4);
        assertTrue(GolemWorkRange.distanceSquared(golem,new net.minecraft.world.phys.AABB(4,1,0,5,2,1))>4);
    }
}
