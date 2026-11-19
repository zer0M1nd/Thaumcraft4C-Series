package tc4revival.content;

import java.util.Arrays;
import net.minecraft.nbt.CompoundTag;

/** Persisted customization and derived rules shared by the entity, menus and placer. */
public  class GolemConfiguration {
    private GolemMaterial material;
    private net.minecraft.nbt.CompoundTag addonData=new net.minecraft.nbt.CompoundTag();
    public net.minecraft.nbt.CompoundTag addonData(net.minecraft.resources.ResourceLocation id){return addonData.getCompound(id.toString()).copy();}
    public void addonData(net.minecraft.resources.ResourceLocation id,net.minecraft.nbt.CompoundTag value){var copy=addonData.copy();if(value.isEmpty())copy.remove(id.toString());else copy.put(id.toString(),value.copy());if(copy.sizeInBytes()>65536)throw new IllegalArgumentException("Golem addon data exceeds 64 KiB");addonData=copy;}
    private net.minecraft.resources.ResourceLocation customMaterial,customTask;
    private net.minecraft.world.level.block.state.BlockState blockAppearance;
    public net.minecraft.resources.ResourceLocation customMaterial(){return customMaterial;}
    public net.minecraft.resources.ResourceLocation customTask(){return customTask;}
    public boolean hasTask(){return core!=null||customTask!=null;}
    public net.minecraft.world.level.block.state.BlockState blockAppearance(){return blockAppearance;}
    public thaumcraft.api.golems.GolemApi.Material properties(){return customMaterial==null?thaumcraft.api.golems.GolemApi.Material.legacy(material):thaumcraft.api.golems.GolemApi.material(customMaterial).orElseGet(()->thaumcraft.api.golems.GolemApi.Material.legacy(material));}
    public void extension(net.minecraft.resources.ResourceLocation material,net.minecraft.resources.ResourceLocation task,net.minecraft.world.level.block.state.BlockState appearance){
        customMaterial=material;customTask=task;blockAppearance=appearance;
        int size=properties().upgrades()+(advanced?1:0);int old=upgrades.length;upgrades=java.util.Arrays.copyOf(upgrades,Math.max(old,size));if(size>old)java.util.Arrays.fill(upgrades,old,size,(byte)-1);
    }
    public void saveExtension(CompoundTag tag){if(!addonData.isEmpty())tag.put("AddonData",addonData.copy());if(customMaterial!=null)tag.putString("AddonMaterial",customMaterial.toString());if(customTask!=null)tag.putString("AddonTask",customTask.toString());if(blockAppearance!=null)tag.put("BlockAppearance",net.minecraft.nbt.NbtUtils.writeBlockState(blockAppearance));}
    public void loadExtension(CompoundTag tag){addonData=tag.getCompound("AddonData").copy();if(addonData.sizeInBytes()>65536)addonData=new CompoundTag();extension(optionalId(tag,"AddonMaterial"),optionalId(tag,"AddonTask"),tag.contains("BlockAppearance")?net.minecraft.nbt.NbtUtils.readBlockState(net.minecraft.core.registries.BuiltInRegistries.BLOCK.asLookup(),tag.getCompound("BlockAppearance")):null);}

    private static net.minecraft.resources.ResourceLocation optionalId(CompoundTag tag,String key) {
        // ResourceLocation.tryParse("") yields minecraft: rather than null in 1.20.1.
        // Missing fields, including the empty-path IDs saved by older builds, mean no addon override.
        if(!tag.contains(key,net.minecraft.nbt.Tag.TAG_STRING))return null;
        String value=tag.getString(key);if(value.isBlank())return null;
        var id=net.minecraft.resources.ResourceLocation.tryParse(value);
        return id==null || id.getPath().isBlank()?null:id;
    }

    private boolean advanced;
    private GolemCoreType core;
    private byte[] upgrades;
    private String decorations="";
    private byte toggles;
    private boolean axisAligned;
    public boolean axisAligned() { return axisAligned; }
    public void axisAligned(boolean value) { axisAligned=value; }
    public GolemConfiguration(GolemMaterial material,boolean advanced) {
        this.material=material; this.advanced=advanced; upgrades=new byte[material.upgrades+(advanced?1:0)]; Arrays.fill(upgrades,(byte)-1);
    }
    public GolemMaterial material() { return material; }
    public boolean advanced() { return advanced; }
    public GolemCoreType core() { return core; }
    public boolean installCore(GolemCoreType value) { if (core!=null || value==null) return false; core=value; return true; }
    public void removeCore() { core=null; }
    public byte[] upgrades() { return upgrades.clone(); }
    public int displayedUpgrade(int slot) {
        return slot>=0 && slot<upgrades.length?upgrades[slot]:-1;
    }
    public int upgrades(int type) { int count=0; for (int i=0;i<Math.min(upgrades.length,properties().upgrades()+(advanced?1:0));i++) if (upgrades[i]==type) count++; return count; }
    public boolean installUpgrade(int type) {
        if (type<0 || type>5 || upgrades(type)>=2) return false;
        for (int index=0;index<Math.min(upgrades.length,properties().upgrades()+(advanced?1:0));index++) if (displayedUpgrade(index)==-1) { upgrades[index]=(byte)type; return true; }
        return false;
    }
    public String decorations() { return decorations; }
    public boolean decorated(char type) { return decorations.indexOf(type)>=0; }
    public boolean decorate(char type) {
        if ("HGBFRVPM".indexOf(type)<0 || decorated(type)) return false;
        for (String exclusive:new String[]{"FH","GV","BP"})
            if (exclusive.indexOf(type)>=0 && (decorated(exclusive.charAt(0)) || decorated(exclusive.charAt(1)))) return false;
        decorations+=type; return true;
    }
    public boolean toggle(int index) { return index>=0 && index<8 && (toggles & (1<<(7-index)))!=0; }
    public void toggle(int index,boolean value) {
        if (index<0 || index>=8) return;
        int mask=1<<(7-index); toggles=(byte)(value?toggles|mask:toggles&~mask);
    }
    private int baseCarryLimit() { return properties().carry()+Math.min(16,Math.max(4,properties().carry()))*upgrades(1); }
    private static int scaledCargo(double base) { return Math.max(1,(int)Math.min(Integer.MAX_VALUE,base*tc4revival.config.TCConfig.SERVER.golemCarryMultiplier.get())); }
    public int carryLimit() { return scaledCargo(baseCarryLimit()); }
    // Scale the original liquid capacity itself, rather than taking the square root of the multiplier.
    public int fluidCarryLimit() { return scaledCargo((int)Math.sqrt(baseCarryLimit())*1000.0); }
    public int strength() { return properties().strength()+upgrades(1); }
    public int attackDamage() { return 2+strength()+upgrades(1)+(decorated('M')?2:0); }
    public int maxHealth() { return properties().health()+(decorated('H')?5:0); }
    public int armor(int equipmentArmor) { return Math.min(20,equipmentArmor+properties().armor()+(decorated('V')?1:0)+(decorated('P')?4:0)); }
    public int regenerationDelay() { return decorated('F')?(int)(properties().regenerationDelay()*.66f):properties().regenerationDelay(); }
    public float range() {
        float range=16+upgrades(3)*4;
        if (decorated('G')) range+=Math.max(range*.1f,1);
        if (advanced) range+=Math.max(range*.2f,2);
        return range;
    }
    public float movementSpeed(boolean paused,boolean inactive,boolean inWater) {
        if (paused || inactive) return 0;
        float speed=properties().speed()*(decorated('B')?1.1f:1);
        if (decorated('P')) speed*=.88f;
        speed*=1+upgrades(0)*.15f;
        if (advanced) speed*=1.1f;
        if (inWater && properties().sinks()) speed*=2;
        return speed*tc4revival.config.TCConfig.SERVER.golemSpeedMultiplier.get().floatValue();
    }
    public void save(CompoundTag tag) {
        saveExtension(tag); tag.putByte("GolemType",(byte)material.ordinal()); tag.putBoolean("advanced",advanced);
        tag.putByte("Core",(byte)(core==null?-1:core.ordinal())); tag.putString("Decoration",decorations);
        tag.putByteArray("upgrades",upgrades); tag.putByte("toggles",toggles);
        tag.putBoolean("AxisAligned",axisAligned);
    }
    public static GolemConfiguration load(CompoundTag tag) {
        var result=new GolemConfiguration(GolemMaterial.fromLegacy(tag.getByte("GolemType")),tag.getBoolean("advanced"));
        result.loadExtension(tag);
        result.core=tag.contains("Core")?GolemCoreType.fromLegacy(tag.getByte("Core")):null;
        result.decorations=tag.getString("Decoration"); result.toggles=tag.getByte("toggles");
        result.axisAligned=tag.getBoolean("AxisAligned");
        byte[] saved=tag.getByteArray("upgrades");
        if(result.customMaterial!=null&&saved.length>result.upgrades.length){int old=result.upgrades.length;result.upgrades=Arrays.copyOf(result.upgrades,Math.min(33,saved.length));Arrays.fill(result.upgrades,old,result.upgrades.length,(byte)-1);}
        for (int index=0;index<Math.min(saved.length,result.upgrades.length);index++)
            result.upgrades[index]=saved[index]>=0 && saved[index]<=5?saved[index]:-1;
        return result;
    }
}
