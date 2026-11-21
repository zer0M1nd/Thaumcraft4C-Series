package tc4revival.content;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

/** Name-based permissions shared by TC4 doors, pressure plates and warded glass. */
public  class OwnedDeviceAccess {
    private String owner="";
    private final ArrayList<String> access=new ArrayList<>();
    public String owner() { return owner; }
    public List<String> entries() { return List.copyOf(access); }
    public void setOwner(String name) { owner=java.util.Objects.requireNonNull(name); }
    public boolean ownedBy(String name) { return owner.equals(name); }
    public boolean mayUse(String name) { return ownedBy(name) || access.contains("0"+name) || access.contains("1"+name); }
    public boolean mayConfigure(String name) { return ownedBy(name) || access.contains("1"+name); }
    public boolean mayBindKey(String name,boolean master) { return ownedBy(name) || !master && access.contains("1"+name); }
    public boolean triggersPlate(int setting,String playerName,boolean ignoresPressurePlates) {
        if (ignoresPressurePlates) return false;
        return switch(setting) {
            case 0->true;
            case 1->playerName==null || !mayUse(playerName);
            case 2->playerName!=null && mayUse(playerName);
            default->false;
        };
    }
    public boolean grant(String name,boolean master) {
        String entry=(master ? "1" : "0")+name;
        if (ownedBy(name) || access.contains(entry) || access.contains("1"+name)) return false;
        access.add(entry); return true;
    }
    /** Compare permission names after removing the access-level prefix. */
    public boolean matchesPressurePlate(OwnedDeviceAccess plate) {
        if (plate.mayUse(owner)) return true;
        for (String entry:access) {
            if (entry.isEmpty()) continue;
            String name=entry.substring(1);
            if (plate.mayUse(name)) return true;
        }
        return false;
    }
    public void save(CompoundTag tag) {
        tag.putString("owner",owner); var list=new ListTag();
        for (String entry:access) { var value=new CompoundTag(); value.putString("name",entry); list.add(value); }
        tag.put("access",list);
    }
    public void load(CompoundTag tag) {
        owner=tag.getString("owner"); access.clear();
        var list=tag.getList("access",Tag.TAG_COMPOUND);
        for (int index=0;index<list.size();index++) access.add(list.getCompound(index).getString("name"));
    }
}
