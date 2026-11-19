package tc4revival.content;

import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.*;
import net.minecraftforge.gametest.*;
import org.joml.Vector3f;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public  class WardParticleGeometryServerTests {
    @GameTest(template="wand_test_empty")
    public static void wardParticleSixFacesMatchLegacyVertexTransforms(GameTestHelper test) {
        int count=0;
        for(var face:Direction.values())for(int degrees:new int[]{0,37,90,183,359})for(float x:new float[]{-.7f,.7f})for(float y:new float[]{-.7f,.7f}) {
            double angle=Math.toRadians(degrees),px=face==Direction.SOUTH?-x:x;
            double a=px*Math.cos(angle)-y*Math.sin(angle),b=px*Math.sin(angle)+y*Math.cos(angle);
            // Closed-form legacy axis transforms, with the south-only final 180-degree flip.
            double[] expected=switch(face){case UP->new double[]{a,.505,b};case DOWN->new double[]{a,-.505,-b};case EAST->new double[]{.505,b,a};case WEST->new double[]{-.505,b,-a};case NORTH->new double[]{b,-a,-.505};case SOUTH->new double[]{-b,a,.505};};
            var actual=WardParticleGeometry.transform(face,degrees).transformPosition(new Vector3f(x,y,0));
            test.assertTrue(Math.abs(actual.x-expected[0])<.000002&&Math.abs(actual.y-expected[1])<.000002&&Math.abs(actual.z-expected[2])<.000002,"Legacy transformed corner: "+face+" "+degrees+" "+x+","+y);count++;
        }
        tc4revival.TC4Revival.LOGGER.info("PASS wardParticleSixFacesMatchLegacyVertexTransforms: {} corners across six faces and five rotations",count);test.succeed();
    }
}
