package tc4revival.aspect.recipe;

import java.util.*;
import tc4revival.aspect.*;

/** Immutable settings captured per calculation, including the nonlinear quantity scale. */
public record InferenceLimits(int amount,int types,boolean scaling,int softLimit,int hardLimit,int hardLimitBeforeScaling) {
    public static final InferenceLimits DEFAULT=new InferenceLimits(64,10,true,32,256,1048576);
    /** Explicit legacy-cap constructor, retained for callers selecting linear inference. */
    public InferenceLimits(int amount,int types){this(amount,types,false,32,256,1048576);}
    public InferenceLimits {
        if(amount<1||amount>10000||types<1||types>1024||softLimit<1||hardLimit<=softLimit
                ||hardLimit>10000||hardLimitBeforeScaling<=softLimit)
            throw new IllegalArgumentException("Inference limits require 1 <= soft_limit < hard_limit <= 10000 and hard_limit_before_scaling > soft_limit");
    }
    public int maximum(){return scaling?hardLimit:amount;}
    /** f: raw quantity to displayed quantity; rounding occurs exactly once at this boundary. */
    public int compress(double x){
        if(x<=0)return 0;
        if(!scaling)return (int)Math.min(amount,Math.round(x));
        if(x<=softLimit)return (int)Math.round(x);
        if(x>=hardLimitBeforeScaling)return hardLimit;
        double value=softLimit+(hardLimit-softLimit)*Math.log(x/softLimit)/Math.log((double)hardLimitBeforeScaling/softLimit);
        return (int)Math.min(hardLimit,Math.round(value));
    }
    /** g: expand each source separately before multiplying counts or summing. */
    public long expand(double x){
        if(x<=0)return 0;
        if(!scaling)return Math.round(x);
        if(x<=softLimit)return Math.round(x);
        if(x>=hardLimit)return hardLimitBeforeScaling;
        double value=softLimit*Math.pow((double)hardLimitBeforeScaling/softLimit,(x-softLimit)/(hardLimit-softLimit));
        return Math.min(hardLimitBeforeScaling,Math.round(value));
    }
    public static InferenceLimits server(){
        if(!tc4revival.config.TCConfig.SERVER_SPEC.isLoaded())return DEFAULT;
        var c=tc4revival.config.TCConfig.SERVER;
        return new InferenceLimits(c.inferredAspectAmountLimit.get(),c.inferredAspectTypeLimit.get(),
                c.inferredAspectScaling.get(),c.inferredAspectSoftLimit.get(),c.inferredAspectHardLimit.get(),c.inferredAspectBeforeScaling.get());
    }
    public Map<String,Integer> apply(Map<String,Integer> values,AspectCatalog catalog){
        var source=new AspectAmounts();values.forEach((tag,n)->catalog.find(tag).ifPresent(a->source.add(a,n)));
        var result=new TreeMap<String,Integer>();
        GeneratedAspectLimit.apply(source,catalog,this).view().forEach((a,n)->result.put(a.tag(),n));return result;
    }
}
