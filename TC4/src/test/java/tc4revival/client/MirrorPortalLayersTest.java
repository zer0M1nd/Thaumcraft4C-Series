package tc4revival.client;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MirrorPortalLayersTest {
    @Test void projectionKeepsFaceSignsAndRejectsSingularPlanes() {
        var layer=MirrorPortalLayers.layers().get(0);
        var positive=MirrorPortalLayers.projection(layer,4,1,2,3,10,20,true);
        var negative=MirrorPortalLayers.projection(layer,-4,-1,2,3,10,20,false);
        assertNotNull(positive); assertEquals(positive,negative);
        assertNull(MirrorPortalLayers.projection(layer,1,1,0,0,0,0,true));
        assertNull(MirrorPortalLayers.projection(layer,-65,0,0,0,0,0,true));
        assertNull(MirrorPortalLayers.projection(layer,Double.NaN,0,0,0,0,0,true));
    }
    @Test void homogeneousTextureMappingDoesNotChangeWhenAllCoordinatesScaleTogether() {
        for (var layer:MirrorPortalLayers.layers()) {
            var first=MirrorPortalLayers.textureCoordinates(layer,2.3,-1.7,.4,12,-8,123456);
            var scaled=MirrorPortalLayers.textureCoordinates(layer,2.3*7,-1.7*7,.4*7,12,-8,123456);
            assertEquals(first[0]/first[2],scaled[0]/scaled[2],1e-10);
            assertEquals(first[1]/first[2],scaled[1]/scaled[2],1e-10);
        }
    }
    @Test void scrollWrapsWithoutChangingLayerIdentityOrColour() {
        var layers=MirrorPortalLayers.layers(); assertEquals(16,layers.size());
        assertFalse(layers.get(0).additive()); assertEquals(.1f,layers.get(0).red());
        assertEquals(65,layers.get(0).depth()); assertEquals(.5f,layers.get(1).scale());
        assertTrue(layers.subList(1,16).stream().allMatch(MirrorPortalLayers.Layer::additive));
        assertEquals(MirrorPortalLayers.scroll(1234),MirrorPortalLayers.scroll(701234));
        assertSame(layers,MirrorPortalLayers.layers());
    }
}
