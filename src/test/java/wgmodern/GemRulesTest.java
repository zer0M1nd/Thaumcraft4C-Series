package wgmodern;
import org.junit.jupiter.api.Test;import static org.junit.jupiter.api.Assertions.*;
public class GemRulesTest {
 @Test public void affinityAndCutTradeoff(){for(int n=1;n<=3;n++){assertEquals(n,GemRules.potency(n,false,true));assertEquals(n-1,GemRules.potency(n,true,true));assertEquals(0,GemRules.brittle(n,true,true));}}
 @Test public void aversionMakesMoreBrittleThanNeutral(){for(int n=1;n<=3;n++){assertEquals(0,GemRules.potency(n,false,false));assertEquals(n,GemRules.brittle(n,false,false));assertEquals(n+1,GemRules.brittle(n,false,true));}}
 @Test public void ovalCostsPreserveMirrorConfig(){assertEquals(16,GemRules.charge("perditio",true));assertEquals(2,GemRules.charge("perditio",false));assertEquals(2,GemRules.charge("ordo",true));assertEquals(1,GemRules.charge("terra",true));}
 @Test public void unawareBackstabAddsRatherThanMultipliesBonus(){assertEquals(16,GemRules.backstab(10,1,true),.0001);assertEquals(16,GemRules.backstab(10,3,false),.0001);assertEquals(20,GemRules.backstab(10,3,true),.0001);}
}
