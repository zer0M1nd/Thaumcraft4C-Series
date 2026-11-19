package automagy.logic;
/** Four upgrade sockets; unknown extension ids are retained by the caller, never coerced to an enum. */
public record WorkbenchUpgrades(int speed,int strength,int recipes,boolean fuzzy,int heatThreshold){
 public static WorkbenchUpgrades from(int[] upgrades){if(upgrades.length!=4)throw new IllegalArgumentException("Four upgrade sockets required");int speed=0,strength=0,recipes=1,adjustment=0;boolean fuzzy=false;
  for(int id:upgrades)switch(id){case 0->{speed++;adjustment++;}case 1->recipes=Math.min(3,recipes+1);case 3->{strength++;adjustment-=2;}case 5->{if(!fuzzy){fuzzy=true;adjustment+=3;}}default->{}}
  return new WorkbenchUpgrades(speed,strength,recipes,fuzzy,1200-adjustment*120);
 }
 public float craftTime(int golemSpeed,int golemStrength,int heat){float goal=Math.max(4.1f,38-golemSpeed*speed-golemStrength*strength);if(heat>heatThreshold)goal=Math.min(200,goal*1.5f*heat/heatThreshold);return goal;}
}
