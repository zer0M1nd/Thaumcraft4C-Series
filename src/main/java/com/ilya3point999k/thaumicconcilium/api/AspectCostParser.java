package com.ilya3point999k.thaumicconcilium.api;

import java.util.*;
import java.util.function.Predicate;

/** Strict modern equivalent of MineTweaker's comma-separated aspect-cost notation. */
public final class AspectCostParser {
    public static Map<String,Integer> parse(String source,Predicate<String> known){
        Objects.requireNonNull(source,"source");Objects.requireNonNull(known,"known");
        var costs=new LinkedHashMap<String,Integer>();
        for(String clause:source.split(",",-1)){
            var parts=clause.trim().split("\\s+");
            if(parts.length!=2||!parts[0].matches("[a-z0-9_./-]+(?::[a-z0-9_./-]+)?")||!known.test(parts[0]))
                throw new IllegalArgumentException("Expected a known aspect and positive amount: "+clause);
            final int amount;
            try{amount=Integer.parseInt(parts[1]);}catch(NumberFormatException invalid){throw new IllegalArgumentException("Invalid aspect amount: "+clause,invalid);}
            if(amount<=0)throw new IllegalArgumentException("Aspect amount must be positive: "+clause);
            try{costs.merge(parts[0],amount,Math::addExact);}catch(ArithmeticException overflow){throw new IllegalArgumentException("Aspect amount overflows: "+parts[0],overflow);}
        }
        return Collections.unmodifiableMap(costs);
    }
    private AspectCostParser(){}
}
