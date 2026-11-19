package tc4revival.content;

/** Core capabilities are independent: sorting and guard have a GUI without a filter inventory. */
public enum GolemCoreType {
    FILL(true,true,true), EMPTY(true,true,true), GATHER(true,true,true), HARVEST(false,false,false),
    GUARD(true,false,false), LIQUID(true,false,true), ESSENTIA(true,false,true), LUMBER(false,false,false),
    USE(true,true,true), BUTCHER(false,false,false), SORTING(true,true,false), FISH(false,false,false);
    public final boolean hasGui,canSort,hasInventory;
    GolemCoreType(boolean gui,boolean sort,boolean inventory) { hasGui=gui; canSort=sort; hasInventory=inventory; }
    public int filterSlots(int fireUpgrades) { return hasInventory ? (this==LIQUID?1+fireUpgrades:6+6*fireUpgrades):0; }
    public static GolemCoreType fromLegacy(int id) { return id>=0 && id<values().length?values()[id]:null; }
}
