package tc4revival.content;

import java.util.*;
import com.google.gson.*;
import net.minecraft.nbt.CompoundTag;
import tc4revival.data.AddonDefinitions;

/** JSON-backed named components. Built-in constants are aliases, never a closed enum. */
public  class WandParts {
    public static  class Cap {
        private static final Map<String,Cap> REGISTRY = load();
        public static final Cap IRON=require("iron");
        public static final Cap GOLD=require("gold");
        public static final Cap THAUMIUM=require("thaumium");
        public static final Cap VOID=require("void");
        public static final Cap COPPER=require("copper");
        public static final Cap SILVER=require("silver");
        public final String tag;
        public final String item;
        public final int metadata, craftCost;
        private final String texture;
        private static Map<String,Cap> load() {
            var entries=new LinkedHashMap<String,Cap>();
            for(var row:AddonDefinitions.load("wand_caps")) {
                var value=new Cap(row.getAsJsonObject());
                if(entries.putIfAbsent(value.tag,value)!=null)throw new IllegalArgumentException("Duplicate Cap: "+value.tag);
            }
            return Collections.unmodifiableMap(entries);
        }
        public static Cap require(String id) { var value=REGISTRY.get(id);if(value==null)throw new IllegalArgumentException("Unknown Cap: "+id);return value; }
        public static Cap[] values() { return REGISTRY.values().toArray(Cap[]::new); }
        public String texture() { return texture; }
        public static Cap decode(JsonObject row) { return new Cap(row); }
        public final float cost;
        private final String research;
        private final Map<String,Float> aspectCosts;
        private Cap(JsonObject row) {
            tag=id(row);item=string(row,"item",null);metadata=integer(row,"metadata",-1);craftCost=integer(row,"craftCost",1);
            cost=row.get("cost").getAsFloat();texture=WandParts.texture(row);research=string(row,"research","");
            var costs=new LinkedHashMap<String,Float>();
            if(row.has("aspectCosts"))row.getAsJsonObject("aspectCosts").entrySet().forEach(e->costs.put(e.getKey(),e.getValue().getAsFloat()));
            aspectCosts=Map.copyOf(costs);
            if(craftCost<0 || craftCost>10000 || !Float.isFinite(cost) || cost<0 || costs.values().stream().anyMatch(v->!Float.isFinite(v)||v<0))throw new IllegalArgumentException("Invalid cap cost "+tag);
        }
        public float cost(String aspect) { return thaumcraft.api.wands.WandAttributes.cost(tag,aspect,aspectCosts.getOrDefault(aspect,cost)); }
        public String research() { return research; }
    }
    public static  class Rod {
        private static final Map<String,Rod> REGISTRY = load();
        public static final Rod WOOD=require("wood");
        public static final Rod GREATWOOD=require("greatwood");
        public static final Rod OBSIDIAN=require("obsidian");
        public static final Rod BLAZE=require("blaze");
        public static final Rod ICE=require("ice");
        public static final Rod QUARTZ=require("quartz");
        public static final Rod BONE=require("bone");
        public static final Rod REED=require("reed");
        public static final Rod SILVERWOOD=require("silverwood");
        public static final Rod GREATWOOD_STAFF=require("greatwood_staff");
        public static final Rod OBSIDIAN_STAFF=require("obsidian_staff");
        public static final Rod BLAZE_STAFF=require("blaze_staff");
        public static final Rod ICE_STAFF=require("ice_staff");
        public static final Rod QUARTZ_STAFF=require("quartz_staff");
        public static final Rod BONE_STAFF=require("bone_staff");
        public static final Rod REED_STAFF=require("reed_staff");
        public static final Rod SILVERWOOD_STAFF=require("silverwood_staff");
        public static final Rod PRIMAL_STAFF=require("primal_staff");
        public final String tag;
        public final String item;
        public final int metadata, craftCost;
        private final String texture;
        private static Map<String,Rod> load() {
            var entries=new LinkedHashMap<String,Rod>();
            for(var row:AddonDefinitions.load("wand_rods")) {
                var value=new Rod(row.getAsJsonObject());
                if(entries.putIfAbsent(value.tag,value)!=null)throw new IllegalArgumentException("Duplicate Rod: "+value.tag);
            }
            return Collections.unmodifiableMap(entries);
        }
        public static Rod require(String id) { var value=REGISTRY.get(id);if(value==null)throw new IllegalArgumentException("Unknown Rod: "+id);return value; }
        public static Rod[] values() { return REGISTRY.values().toArray(Rod[]::new); }
        public String texture() { return texture; }
        public static Rod decode(JsonObject row) { return new Rod(row); }
        public final int capacity;
        public int capacity() { return thaumcraft.api.wands.WandAttributes.capacity(tag,capacity); }
        public final String regenerates;
        public final boolean regenerateAll;
        public final int regenerationInterval;
        private final String research;
        private final boolean staff,runes,glowing;
        public final boolean rechargeable;
        private Rod(JsonObject row) {
            tag=id(row);item=string(row,"item",null);metadata=integer(row,"metadata",-1);craftCost=integer(row,"craftCost",1);
            capacity=row.get("capacity").getAsInt();texture=WandParts.texture(row);research=string(row,"research","ROD_"+tag);
            regenerates=row.has("regenerates")&&!row.get("regenerates").isJsonNull()?row.get("regenerates").getAsString():null;
            rechargeable=!row.has("rechargeable") || row.get("rechargeable").getAsBoolean();
            staff=bool(row,"staff");runes=bool(row,"runes");glowing=bool(row,"glowing");
            regenerateAll=bool(row,"regenerateAll");regenerationInterval=integer(row,"regenerationInterval",200);
            if(regenerationInterval<1)throw new IllegalArgumentException("Invalid regeneration interval "+tag);
            if(capacity<1 || capacity>thaumcraft.api.wands.WandPropertyLayers.MAX_CAPACITY || craftCost<0 || craftCost>10000)throw new IllegalArgumentException("Invalid rod "+tag);
        }
        public String research() { return research; }
        public boolean staff() { return staff; }
        public boolean runes() { return runes; }
        public boolean glowing() { return glowing; }
    }
    private WandParts() {}
    private static String id(JsonObject row) { var tag=row.get("tag").getAsString();net.minecraft.resources.ResourceLocation.parse(tag);return tag; }
    private static String texture(JsonObject row) { var value=row.get("texture").getAsString();net.minecraft.resources.ResourceLocation.parse(value);return value; }
    private static int integer(JsonObject row,String key,int fallback) { return row.has(key)?row.get(key).getAsInt():fallback; }
    private static String string(JsonObject row,String key,String fallback) { return row.has(key)?row.get(key).getAsString():fallback; }
    private static boolean bool(JsonObject row,String key) { return row.has(key)&&row.get(key).getAsBoolean(); }
    public static Rod rod(CompoundTag nbt) { return nbt==null?Rod.WOOD:Rod.REGISTRY.getOrDefault(nbt.getString("rod"),Rod.WOOD); }
    public static Cap cap(CompoundTag nbt) { return nbt==null?Cap.IRON:Cap.REGISTRY.getOrDefault(nbt.getString("cap"),Cap.IRON); }
    public static boolean sceptre(CompoundTag nbt) { return nbt!=null&&nbt.contains("sceptre"); }
    public static int capacity(CompoundTag nbt) { return WandVis.capacity(rod(nbt).capacity(),sceptre(nbt)); }
}
