package automagy.logic;
/** Integer XP storage shared by addon containers; transfer rates belong to the device. */
public final class ExperienceBuffer {
 private final int capacity;private int stored;
 public ExperienceBuffer(int capacity){if(capacity<1)throw new IllegalArgumentException("Positive capacity required");this.capacity=capacity;}
 public int stored(){return stored;}public int capacity(){return capacity;}
 public void restore(int value){stored=Math.max(0,Math.min(capacity,value));}
 public int insert(int requested,int limit,boolean simulate){int amount=Math.max(0,Math.min(Math.min(requested,limit),capacity-stored));if(!simulate)stored+=amount;return amount;}
 public int extract(int requested,int limit,boolean simulate){int amount=Math.max(0,Math.min(Math.min(requested,limit),stored));if(!simulate)stored-=amount;return amount;}
 public int comparator(){return stored==0?0:1+(int)(14L*stored/capacity);}
 /** Signed desired movement into the stone, including the old odd-unit correction. */
 public static int balanceWithBrain(int stone,int brain,int brainCapacity){if(stone==brain)return 0;long average=((long)stone+brain)/2;if(brain<average)return -(int)Math.min(Math.max(0,(long)brainCapacity-brain),average-brain);return (int)Math.min(brain,Math.max(1,average-stone));}
}
