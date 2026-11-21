package emtmodern.item;

import java.util.List;

/** Original 1.2.6.2 balance, independent of GTNH's pack-specific adjustments. */
public class ToolProfile {
    protected String id,texture;
    protected double capacity,transfer;
    protected int tier,breakCost,hitCost;
    protected float hitDamage,speed;
    protected boolean pick,axe,shovel,shears;
    public ToolProfile(String id,String texture,double capacity,int tier,double transfer,int breakCost,int hitCost,float hitDamage,float speed,boolean pick,boolean axe,boolean shovel,boolean shears){this.id=id;this.texture=texture;this.capacity=capacity;this.tier=tier;this.transfer=transfer;this.breakCost=breakCost;this.hitCost=hitCost;this.hitDamage=hitDamage;this.speed=speed;this.pick=pick;this.axe=axe;this.shovel=shovel;this.shears=shears;}
    public String id(){return id;}public String texture(){return texture;}public double capacity(){return capacity;}public int tier(){return tier;}public double transfer(){return transfer;}public int breakCost(){return breakCost;}public int hitCost(){return hitCost;}public float hitDamage(){return hitDamage;}public float speed(){return speed;}public boolean pick(){return pick;}public boolean axe(){return axe;}public boolean shovel(){return shovel;}public boolean shears(){return shears;}
    public int boreDurability(){return pick?(int)Math.round(capacity/breakCost):27;}
    public static final List<ToolProfile> ALL=List.of(
        new ToolProfile("thaumium_drill","drill_thaumium",60000,2,100,250,0,0,20,true,false,true,false),
        new ToolProfile("diamond_chainsaw","chainsaw_diamond",40000,2,300,200,300,12,16,false,true,false,true),
        new ToolProfile("thaumium_chainsaw","chainsaw_thaumium",60000,2,600,250,350,12,21,false,true,false,true),
        new ToolProfile("iron_omnitool","omnitool_iron",20000,2,200,100,125,8,13,true,true,true,true),
        new ToolProfile("diamond_omnitool","omnitool_diamond",70000,2,400,200,250,10,16,true,true,true,true),
        new ToolProfile("thaumium_omnitool","omnitool_thaumium",140000,2,800,300,650,10,21,true,true,true,true));
}
