package thmodern.plane;
/** Lifecycle count follows the old warm-up, accumulated instability and collapse delay. */
public final class RiftLifetime {
 private int count;private boolean collapsing;
 public RiftLifetime(){}
 public RiftLifetime(int count,boolean collapsing){this.count=Math.max(0,Math.min(2401,count));this.collapsing=collapsing;}
 public int count(){return count;}public boolean collapsing(){return collapsing;}
 public boolean ready(){return !collapsing&&count>=50;}
 /** Returns true only when final destruction is due. */
 public boolean tick(int beams,boolean exempt){if(collapsing)return ++count>25;if(count<50){count++;return false;}if(!exempt)count+=Math.max(0,6-Math.max(0,beams));if(count>2400&&!exempt){collapsing=true;count=0;}return false;}
}
