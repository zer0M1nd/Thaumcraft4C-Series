package tc4revival.content;

import java.util.function.IntUnaryOperator;
import tc4revival.research.PlayerKnowledge;

/** The old event roll has intentional empty bands and uses only lasting warp for research gates. */
public  class WarpEventRules {
    public enum Event {
        NONE, INSIGHT, PARANOIA, EXHAUSTION, THAUMARHIA, HUNGER, WHISPERS,
        MIST, BLURRED, SUN, FATIGUE, INFECTIOUS, NIGHT_VISION, GAZE,
        SPIDERS, DREAD, MANY_GUARDIANS, BLINDNESS, RELIEF, LONG_HUNGER,
        GREAT_INSIGHT, REAL_SPIDERS, MOST_GUARDIANS
    }
    public record Roll(boolean triggered,int strength,int lastingWarp,Event event) {}
    private WarpEventRules() {}
    public static Roll roll(PlayerKnowledge knowledge,int gearWarp,boolean guardedMask,IntUnaryOperator random) {
        int lasting=(int)Math.min(Integer.MAX_VALUE,(long)knowledge.warp(PlayerKnowledge.WarpKind.PERMANENT)+knowledge.warp(PlayerKnowledge.WarpKind.STICKY));
        int chance=random.applyAsInt(100),counter=knowledge.warpCounter();
        long total=(long)knowledge.warp(PlayerKnowledge.WarpKind.PERMANENT)+knowledge.warp(PlayerKnowledge.WarpKind.STICKY)
                +knowledge.warp(PlayerKnowledge.WarpKind.TEMPORARY)+gearWarp;
        // A stale counter or warping equipment must not reactivate a fully cleansed player.
        if (knowledge.totalWarp()<=0 || counter<=0 || total<=0 || chance>Math.sqrt(counter)) return new Roll(false,0,lasting,Event.NONE);
        int strength=(int)Math.min(100,(total*2+counter)/3);
        knowledge.setWarpCounter((int)(counter-Math.max(5,Math.sqrt(counter)*2)));
        // Invalid edited data must not call nextInt(0).
        int choice=strength>0 ? random.applyAsInt(strength) : 0;
        if (guardedMask) choice-=2+random.applyAsInt(4);
        return new Roll(true,strength,lasting,event(choice));
    }
    public static Event event(int choice) {
        if (choice<=0) return Event.NONE;
        if (choice<=4) return Event.INSIGHT;
        if (choice<=8) return Event.NONE;
        if (choice<=12) return Event.PARANOIA;
        if (choice<=16) return Event.EXHAUSTION;
        if (choice<=20) return Event.THAUMARHIA;
        if (choice<=24) return Event.HUNGER;
        if (choice<=28) return Event.WHISPERS;
        if (choice<=32) return Event.MIST;
        if (choice<=36) return Event.BLURRED;
        if (choice<=40) return Event.SUN;
        if (choice<=44) return Event.FATIGUE;
        if (choice<=48) return Event.INFECTIOUS;
        if (choice<=52) return Event.NIGHT_VISION;
        if (choice<=56) return Event.GAZE;
        if (choice<=60) return Event.SPIDERS;
        if (choice<=64) return Event.DREAD;
        if (choice<=68) return Event.MANY_GUARDIANS;
        if (choice<=72) return Event.BLINDNESS;
        if (choice==76) return Event.RELIEF;
        if (choice<=80) return Event.LONG_HUNGER;
        if (choice<=84) return Event.GREAT_INSIGHT;
        if (choice<=88) return Event.NONE;
        if (choice<=92) return Event.REAL_SPIDERS;
        return Event.MOST_GUARDIANS;
    }
}
