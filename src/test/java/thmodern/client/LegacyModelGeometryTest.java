package thmodern.client;

import com.mojang.blaze3d.vertex.*;
import java.util.*;
import net.minecraft.client.model.geom.ModelPart;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** CPU-only mesh tests. These bake/inspect vertices; they never create a window or a GL context. */
class LegacyModelGeometryTest {
    record Box(ModelPart.Cube cube,float minU,float minV,float maxU,float maxV){}
    static Map<String,Box> boxes(ModelPart root){
        var result=new HashMap<String,Box>();
        root.visit(new PoseStack(),(pose,path,index,cube)->{
            var uv=new UV();cube.compile(new PoseStack().last(),uv,0,0,1,1,1,1);
            result.put(path+":"+index,new Box(cube,uv.minU,uv.minV,uv.maxU,uv.maxV));
        });return result;
    }
    static void uv(Box box,int u,int v,int w,int h,int d){
        assertNotNull(box);assertEquals(u/128f,box.minU,1e-6);assertEquals(v/128f,box.minV,1e-6);
        assertEquals((u+2*w+2*d)/128f,box.maxU,1e-6);assertEquals((v+h+d)/128f,box.maxV,1e-6);
    }
    @Test void horseLegsUseLegacySkinRegionsAndSeparateKnees(){
        var root=LegacySteedModel.layer().bakeRoot();var data=boxes(root);
        String[] names={"front_left","front_right","back_left","back_right"};int[] offsets={44,60,78,96};
        for(int i=0;i<4;i++){
            boolean front=i<2;var upper=data.get("/"+names[i]+":0");
            uv(upper,offsets[i],29,front?3:4,front?8:9,front?4:5);
            uv(data.get("/"+names[i]+"/shin:0"),offsets[i],front?41:43,3,5,3);
            uv(data.get("/"+names[i]+"/shin:1"),offsets[i],51,4,3,4);
            assertEquals(7,root.getChild(names[i]).getChild("shin").y,1e-6);
            assertEquals(front?3:4,upper.cube.maxX-upper.cube.minX,1e-6);
        }
    }
    @Test void saddleAndBridleHaveTheirOwnUvRegions(){
        var data=boxes(LegacySteedModel.layer().bakeRoot());
        uv(data.get("/saddle:0"),80,0,10,1,8);uv(data.get("/saddle:1"),106,9,3,1,2);uv(data.get("/saddle:2"),80,9,8,1,2);
        uv(data.get("/head/bridle:0"),80,12,5,5,12);
        uv(data.get("/reins:0"),44,10,0,3,16);uv(data.get("/reins:1"),44,5,0,3,16);
        uv(data.get("/left_stirrup:1"),74,0,1,2,2);uv(data.get("/right_stirrup:1"),74,4,1,2,2);
    }
    @Test void effigyIsACompleteBipedAndFamiliarHasAllFiveHatSections(){
        var body=boxes(CorpseEffigyModel.layer().bakeRoot());
        assertEquals(7,body.size());for(var part:List.of("head","body","left_arm","right_arm","left_leg","right_leg"))assertNotNull(body.get("/"+part+":0"));
        assertEquals(8,body.get("/head:0").cube.maxX-body.get("/head:0").cube.minX);
        var hat=boxes(FamiliarModel.layer().bakeRoot());
        assertEquals(5,hat.keySet().stream().filter(k->k.startsWith("/head/hat:")).count());
    }
    @Test void finiteLightningScalesDeviationWithoutThinningItsGlow(){
        var shortBolt=new Vertices();var longBolt=new Vertices();
        FiniteLightningRenderer.renderBolt(new PoseStack(),shortBolt,12345,16);
        FiniteLightningRenderer.renderBolt(new PoseStack(),longBolt,12345,128);
        assertEquals(896,shortBolt.positions.size());assertEquals(896,longBolt.positions.size());
        assertEquals(16,shortBolt.positions.stream().mapToDouble(v->v[1]).max().orElseThrow(),1e-6);
        assertEquals(128,longBolt.positions.stream().mapToDouble(v->v[1]).max().orElseThrow(),1e-6);
        // Vertices 1 and 2 are opposite X corners of the first upper edge: equal glow widths,
        // while their midpoint (the random path) must be eight times closer for the short bolt.
        var s1=shortBolt.positions.get(1);var s2=shortBolt.positions.get(2);
        var l1=longBolt.positions.get(1);var l2=longBolt.positions.get(2);
        assertEquals(l2[0]-l1[0],s2[0]-s1[0],1e-5);
        assertEquals((l1[0]+l2[0])/8,s1[0]+s2[0],1e-5);
    }
    static class Vertices extends UV {
        final List<double[]> positions=new ArrayList<>();
        @Override public VertexConsumer vertex(double x,double y,double z){super.vertex(x,y,z);positions.add(new double[]{x,y,z});return this;}
    }
    static class UV implements VertexConsumer {
        float minU=Float.POSITIVE_INFINITY,minV=Float.POSITIVE_INFINITY,maxU=Float.NEGATIVE_INFINITY,maxV=Float.NEGATIVE_INFINITY;
        public VertexConsumer vertex(double x,double y,double z){assertTrue(Double.isFinite(x)&&Double.isFinite(y)&&Double.isFinite(z));return this;}
        public VertexConsumer color(int r,int g,int b,int a){return this;}
        public VertexConsumer uv(float u,float v){minU=Math.min(minU,u);minV=Math.min(minV,v);maxU=Math.max(maxU,u);maxV=Math.max(maxV,v);return this;}
        public VertexConsumer overlayCoords(int u,int v){return this;}public VertexConsumer uv2(int u,int v){return this;}
        public VertexConsumer normal(float x,float y,float z){return this;}public void endVertex(){}
        public void defaultColor(int r,int g,int b,int a){}public void unsetDefaultColor(){}
    }
}
