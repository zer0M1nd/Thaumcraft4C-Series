package automagy.logic;

import java.util.Map;

/** Immutable observation in whole aspect units; validity distinguishes an empty vessel from a missing target. */
public record AspectReading(Map<String,Integer> contents, Map<String,Integer> capacities,
                            long totalCapacity, int individualCapacity, boolean perAspectCapacity, boolean valid) {
    public static final AspectReading MISSING = new AspectReading(Map.of(),Map.of(),0,0,false,false);
    public AspectReading { contents=Map.copyOf(contents);capacities=Map.copyOf(capacities); }
}
