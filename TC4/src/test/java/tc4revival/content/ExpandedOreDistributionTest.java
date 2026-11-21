package tc4revival.content;

import java.util.Arrays;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ExpandedOreDistributionTest {
    private ExpandedOreDistribution uniform(ExpandedOreDistribution.Band band,int count){
        var bands=new ExpandedOreDistribution.Band[256];Arrays.fill(bands,band);return new ExpandedOreDistribution(bands,count);
    }
    @Test void negativeHeightIncreasesAttemptsInProportionToVolume(){
        var cinnabar=uniform(ExpandedOreDistribution.cinnabar(-64,320),18);
        assertEquals(-64,cinnabar.band(0).minimum());assertEquals(51,cinnabar.band(0).maximumExclusive());
        assertEquals(18*115d/51,cinnabar.expectedAttempts(),1e-10);
        assertEquals(18d/51,cinnabar.expectedAttempts()/115,1e-10);
        var amber=uniform(ExpandedOreDistribution.amber(-64,320,64),40);
        assertEquals(16,amber.band(0).minimum());assertEquals(65,amber.band(0).maximumExclusive());
        assertEquals(40,amber.expectedAttempts(),1e-10);
        var infused=uniform(ExpandedOreDistribution.infused(-64,320,64),8);
        assertEquals(8*123d/59,infused.expectedAttempts(),1e-10);
        assertEquals(8d/59,infused.expectedAttempts()/123,1e-10);
    }
    @Test void fractionalAttemptIsNotAlwaysRoundedDown(){
        var distribution=uniform(ExpandedOreDistribution.cinnabar(-64,320),18);
        assertEquals(41,distribution.attempts(0));assertEquals(40,distribution.attempts(.999));
        var random=new java.util.Random(91);long total=0;
        for(int i=0;i<100000;i++)total+=distribution.attempts(random.nextDouble());
        assertEquals(distribution.expectedAttempts(),total/100000d,.01);
    }
    @Test void columnsAreWeightedByAddedVolumeAndOriginalLocalDensity(){
        var low=ExpandedOreDistribution.infused(-64,320,25);
        var high=ExpandedOreDistribution.infused(-64,320,205);
        var distribution=new ExpandedOreDistribution(new ExpandedOreDistribution.Band[]{low,high},8);
        double probability=low.weight()/(low.weight()+high.weight());
        assertEquals(0,distribution.column(Math.nextDown(probability)));
        assertEquals(1,distribution.column(Math.nextUp(probability)));
        assertEquals(8d/(2*low.originalSpan()),distribution.expectedAttempts()*probability/low.span(),1e-12);
        assertEquals(8d/(2*high.originalSpan()),distribution.expectedAttempts()*(1-probability)/high.span(),1e-12);
    }
    @Test void customWorldBoundsAndEmptyColumnsAreSafe(){
        var empty=ExpandedOreDistribution.cinnabar(100,200);
        assertEquals(0,uniform(empty,18).attempts(0));
        assertThrows(IllegalStateException.class,()->uniform(empty,18).column(0));
        var band=ExpandedOreDistribution.amber(-128,40,70);
        assertEquals(22,band.minimum());assertEquals(40,band.maximumExclusive());
        var distribution=new ExpandedOreDistribution(new ExpandedOreDistribution.Band[]{empty,band,empty},20);
        assertEquals(1,distribution.column(0));assertEquals(1,distribution.column(.999999));
    }
    @Test void amberIsAlwaysNearItsLocalSurfaceAndDoublesTheOldAttemptCount(){
        for(int surface:new int[]{0,64,150,250}){
            var band=ExpandedOreDistribution.amber(-64,320,surface);
            assertEquals(surface-48,band.minimum());assertEquals(surface+1,band.maximumExclusive());
            assertEquals(49,band.span());assertEquals(40,uniform(band,40).expectedAttempts(),1e-10);
        }
        var clipped=ExpandedOreDistribution.amber(-64,320,-32);
        assertEquals(-64,clipped.minimum());assertEquals(-31,clipped.maximumExclusive());
    }
    @Test void infusedMultiplierScalesCountWithoutChangingHeightOrColumnWeights(){
        var bands=new ExpandedOreDistribution.Band[]{ExpandedOreDistribution.infused(-64,320,25),ExpandedOreDistribution.infused(-64,320,205)};
        var baseline=new ExpandedOreDistribution(bands,8);
        for(double multiplier:new double[]{0,.01,.5,1,2,100}){
            var scaled=new ExpandedOreDistribution(bands,8,multiplier);
            assertEquals(baseline.expectedAttempts()*multiplier,scaled.expectedAttempts(),1e-12);
            for(double roll:new double[]{0,.2,.8,.999})assertEquals(baseline.column(roll),scaled.column(roll));
            for(int i=0;i<bands.length;i++)assertEquals(baseline.band(i),scaled.band(i));
        }
        assertEquals(0,new ExpandedOreDistribution(bands,8,0).attempts(0));
        assertThrows(IllegalArgumentException.class,()->new ExpandedOreDistribution(bands,8,Double.NaN));
        assertThrows(IllegalArgumentException.class,()->new ExpandedOreDistribution(bands,8,-1));
    }
}
