package tc4revival.aspect.recipe;

import tc4revival.aspect.*;

/** The old cullTags weighting applies only to recipe-derived base lists, not explicit definitions. */
public class GeneratedAspectLimit {
    public static AspectAmounts apply(AspectAmounts source,AspectCatalog catalog) {
        return apply(source,catalog,InferenceLimits.server());
    }
    public static AspectAmounts apply(AspectAmounts source,AspectCatalog catalog,InferenceLimits limits) {
        var result=source.copy();
        while(result.size()>limits.types()) {
            Aspect lowest=null;float weight=Float.POSITIVE_INFINITY;
            for(var aspect:result.aspects()) {
                float candidate=result.amount(aspect);
                if(aspect.isPrimal())candidate*=.9f;
                else for(var component:aspect.components()) {
                    var child=catalog.require(component);
                    if(!child.isPrimal()) {
                        candidate*=1.1f;
                        for(var grandchild:child.components())if(!catalog.require(grandchild).isPrimal())candidate*=1.05f;
                    }
                }
                if(candidate<weight){weight=candidate;lowest=aspect;}
            }
            result.remove(lowest);
        }
        var capped=new AspectAmounts();
        result.view().forEach((a,n)->capped.add(a,Math.min(n,limits.maximum())));
        return capped;
    }
}
