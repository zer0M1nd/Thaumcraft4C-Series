package thaumcraft.api.research;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import tc4revival.aspect.*;
import tc4revival.research.*;

class ScanProgressRulesTest {
    @Test void selectedRuleOwnsTheLedgerWhileMatchingAndViewsCannotMutateIt(){
        var catalog=new AspectCatalog(Arrays.stream(LegacyAspect.values()).map(LegacyAspect::create).toList());
        var aspect=catalog.require("aer");var raw=new PlayerKnowledge(catalog);var amounts=new AspectAmounts().add(aspect,7);
        ScanProgressRules.register("scan_test:primary",100,ctx->{if(!ctx.identity().equals("api:test"))return false;ctx.target().add(aspect,99);return true;},(ledger,ctx)->{
            if(!ledger.canScan(ctx.kind(),ctx.identity(),ctx.mode()))return new ScanRules.Result(ScanRules.Status.ALREADY_SCANNED,Optional.empty(),List.of());
            var reward=ScanRules.grant(ledger,aspect,ctx.target().amount(aspect),100);ledger.rememberScan(ctx.kind(),ctx.mode().key(ctx.identity()));
            return new ScanRules.Result(ScanRules.Status.SUCCESS,Optional.empty(),List.of(reward));
        });
        ScanProgressRules.register("scan_test:fallback",0,ctx->ctx.identity().equals("api:test"),(ledger,ctx)->{fail("only one rule may grant rewards");return null;});
        assertTrue(ScanRules.apply(raw,catalog,PlayerKnowledge.ScanKind.OBJECT,"api:test",PlayerKnowledge.ScanMode.THAUMOMETER,amounts,100).succeeded());
        assertEquals(7,raw.poolAmount(aspect));assertEquals(7,amounts.amount(aspect));
        assertEquals(ScanRules.Status.ALREADY_SCANNED,ScanRules.apply(raw,catalog,PlayerKnowledge.ScanKind.OBJECT,"api:test",PlayerKnowledge.ScanMode.THAUMOMETER,amounts,100).status());
        assertEquals(7,raw.poolAmount(aspect));
        var view=new ScannerViews.Inventory(PlayerKnowledge.ScanKind.OBJECT,"api:test",amounts,raw);
        view.knowledge().setPool(aspect,999);view.amounts().add(aspect,999);
        assertEquals(7,raw.poolAmount(aspect));assertEquals(7,view.amounts().amount(aspect));
        assertTrue(ScanRules.apply(new PlayerKnowledge(catalog),catalog,PlayerKnowledge.ScanKind.OBJECT,"ordinary",PlayerKnowledge.ScanMode.THAUMOMETER,amounts,100).succeeded());
    }
}
