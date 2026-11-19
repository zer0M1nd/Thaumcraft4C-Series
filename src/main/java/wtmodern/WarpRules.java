package wtmodern;

import java.util.*;
import java.util.function.IntUnaryOperator;

/** Pure rules keep the original author's severity/cost table separate from GTNH's pack balance. */
public class WarpRules {
    public static class Event {
        private final String id;private final int severity;private final boolean global;
        public Event(String id,int severity,boolean global){this.id=id;this.severity=severity;this.global=global;}
        public String id(){return id;}public int severity(){return severity;}public boolean global(){return global;}
        public int cost(){ return (severity+9)/10; }
    }
    public static final List<Event> EVENTS=List.of(
        new Event("bats",15,false),new Event("blink",30,false),new Event("poison",16,false),
        new Event("nausea",25,false),new Event("jump",18,false),new Event("blind",43,false),
        new Event("decay",50,true),new Event("ears",12,false),new Event("swamp",50,true),
        new Event("tongue",11,false),new Event("friend",26,false),new Event("livestock",32,false),
        new Event("wind",35,false),new Event("chests",35,true),new Event("blood",25,false),
        new Event("acceleration",27,true),new Event("lightning",60,false),new Event("fall",36,true),
        new Event("rain",12,true),new Event("wither",70,true),new Event("fakeexplosion",10,false),new Event("fakecreeper",10,false));
    public static Event find(String id){return EVENTS.stream().filter(e->e.id().equals(id)).findFirst().orElse(null);}
    public static Event choose(int severity,Collection<Event> enabled,IntUnaryOperator random){
        var possible=enabled.stream().filter(e->e.severity()<=severity).toList();
        return possible.isEmpty()?null:possible.get(random.applyAsInt(possible.size()));
    }
    /** Temporary, then normal, then permanent; the original integer division before Math.ceil intentionally keeps truncation. */
    public static int[] remove(int permanent,int sticky,int temporary,int amount,int multiplier,boolean allowPermanent){
        int n=Math.max(0,amount),t=Math.min(temporary,n); temporary-=t;n-=t;
        t=Math.min(sticky,n);sticky-=t;n-=t;
        if(allowPermanent&&n>0)permanent=Math.max(0,permanent-(int)Math.min(Integer.MAX_VALUE,((long)n)/Math.max(1,multiplier)));
        return new int[]{permanent,sticky,temporary};
    }
    public static List<String> purgeQueue(int amount,Collection<Event> enabled,IntUnaryOperator random){
        var queue=new ArrayList<String>();
        for(int remaining=Math.max(0,amount);remaining>0;){
            var e=choose(remaining,enabled,random);if(e==null)break;
            queue.add(e.id());remaining-=e.cost();
        }
        return queue;
    }
    public static String severityKey(int total){return total<=10?"minorwarp":total<=25?"averagewarp":total<=50?"majorwarp":"deadlywarp";}
}
