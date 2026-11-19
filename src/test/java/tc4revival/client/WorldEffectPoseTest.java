package tc4revival.client;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.*;

class WorldEffectPoseTest {
    @Test void billboardUsesViewExactlyOnceAndDoesNotAlterTheStage() {
        for(float yaw:new float[]{0,.5f,1.7f,3.1f})for(float pitch:new float[]{-.7f,0,.8f}) {
            var view=new PoseStack();var orientation=new Quaternionf().rotationYXZ(yaw,pitch,0);
            view.mulPose(new Quaternionf(orientation).conjugate());
            var original=new Matrix4f(view.last().pose());var pose=WorldEffectPose.copy(view);
            pose.translate(3,2,-8);pose.mulPose(orientation);
            var expected=original.transformPosition(new Vector3f(3,2,-8));
            var center=pose.last().pose().transformPosition(new Vector3f());
            assertEquals(0,expected.distance(center),.00001);
            // A billboard's right vector stays screen-horizontal at every camera yaw/pitch.
            var right=pose.last().pose().transformDirection(new Vector3f(1,0,0));
            assertEquals(0,right.distance(new Vector3f(1,0,0)),.00001);
            assertEquals(original,view.last().pose());
        }
    }
    @Test void mistFadesBackToOriginalRangeWithoutNegativeDensitySentinels() {
        assertEquals(new MistFogRange(200,256),MistFogRange.apply(200,256,0));
        float previous=0;
        for(int i=100;i>=1;i--) {
            var fog=MistFogRange.apply(200,256,i/1000f);
            assertTrue(fog.near()>=0&&fog.far()>fog.near()&&fog.far()<=256);
            assertTrue(fog.far()>=previous);previous=fog.far();
        }
        assertEquals(30,MistFogRange.apply(200,256,.1f).far(),.0001);
        assertEquals(new MistFogRange(-8,12),MistFogRange.apply(-8,12,.1f));
    }
}
