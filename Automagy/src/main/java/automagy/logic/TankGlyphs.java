package automagy.logic;
/** Legacy glyph ids are persisted per face; this is not a closed extension enum. */
public record TankGlyphs(int capacity,int drinkInterval,int consumption,boolean voiding,boolean controlled,boolean preserving,boolean milking){
 public static TankGlyphs from(int[] glyphs){if(glyphs.length!=6)throw new IllegalArgumentException("Six faces required");int capacity=16000,consume=0;float interval=30;boolean v=false,c=false,p=false,m=false;
  for(int glyph:glyphs)switch(glyph){case 1->v=true;case 2->consume+=100;case 5->c=true;case 6->p=true;case 7->interval*=.65f;case 8->capacity+=16000;case 9->m=true;default->{}}
  return new TankGlyphs(capacity,Math.round(interval),consume,v,c,p,m);
 }
 public int drainLimit(int requested,int stored){return Math.max(0,Math.min(requested,stored-(preserving?1:0)));}
 public boolean canRemoveCapacity(int stored){return stored<=capacity-16000;}
 public int siphonShare(int stored,int destinations){return destinations<=0?0:Math.min(150,drainLimit(Integer.MAX_VALUE,stored))/destinations;}
}
