package tc4revival.content;

import java.util.*;
import com.google.gson.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** Named JSON upgrades. Numeric IDs are wire aliases; persistent items also store the stable key. */
public  class FocusUpgrade {
    private static final Map<String,FocusUpgrade> REGISTRY=new LinkedHashMap<>();
    private static final Map<Integer,FocusUpgrade> IDS=new LinkedHashMap<>();
    static {
        int next=21;
        for(var element:tc4revival.data.AddonDefinitions.load("focus_upgrades")) {
            var row=element.getAsJsonObject();
            int id=row.has("id")?row.get("id").getAsInt():next++;
            var upgrade=new FocusUpgrade(row,id);
            if(REGISTRY.putIfAbsent(upgrade.key,upgrade)!=null || IDS.putIfAbsent(id,upgrade)!=null)throw new IllegalArgumentException("Duplicate focus upgrade "+upgrade.key+" / "+id);
            next=Math.max(next,id+1);
        }
    }
    public static final FocusUpgrade POTENCY=require("thaumcraft:potency");
    public static final FocusUpgrade FRUGAL=require("thaumcraft:frugal");
    public static final FocusUpgrade TREASURE=require("thaumcraft:treasure");
    public static final FocusUpgrade ENLARGE=require("thaumcraft:enlarge");
    public static final FocusUpgrade ALCHEMISTS_FIRE=require("thaumcraft:alchemistsfire");
    public static final FocusUpgrade ALCHEMISTS_FROST=require("thaumcraft:alchemistsfrost");
    public static final FocusUpgrade ARCHITECT=require("thaumcraft:architect");
    public static final FocusUpgrade EXTEND=require("thaumcraft:extend");
    public static final FocusUpgrade SILK_TOUCH=require("thaumcraft:silktouch");
    public static final FocusUpgrade FIREBALL=require("thaumcraft:fireball");
    public static final FocusUpgrade FIREBEAM=require("thaumcraft:firebeam");
    public static final FocusUpgrade SCATTERSHOT=require("thaumcraft:scattershot");
    public static final FocusUpgrade ICE_BOULDER=require("thaumcraft:iceboulder");
    public static final FocusUpgrade BAT_BOMBS=require("thaumcraft:batbombs");
    public static final FocusUpgrade DEVIL_BATS=require("thaumcraft:devilbats");
    public static final FocusUpgrade NIGHTSHADE=require("thaumcraft:nightshade");
    public static final FocusUpgrade SEEKER=require("thaumcraft:seeker");
    public static final FocusUpgrade CHAIN_LIGHTNING=require("thaumcraft:chainlightning");
    public static final FocusUpgrade EARTH_SHOCK=require("thaumcraft:earthshock");
    public static final FocusUpgrade VAMPIRE_BATS=require("thaumcraft:vampirebats");
    public static final FocusUpgrade DOWSING=require("thaumcraft:dowsing");
    public final int id;
    public final String key,path;
    public final List<String> aspects;
    private final String name,description,texture;
    private final Set<String> foci;
    private final Set<Integer> ranks;
    private FocusUpgrade(JsonObject row,int id) {
        if(id<0||id>32767)throw new IllegalArgumentException("Upgrade wire ID outside short range");
        this.id=id;key=row.get("key").getAsString();ResourceLocation.parse(key);
        path=row.has("path")?row.get("path").getAsString():key.replace(':','.');
        name=row.has("name")?row.get("name").getAsString():"focus.upgrade."+path+".name";
        description=row.has("description")?row.get("description").getAsString():"focus.upgrade."+path+".text";
        texture=row.get("texture").getAsString();ResourceLocation.parse(texture);
        var tags=new ArrayList<String>();row.getAsJsonArray("aspects").forEach(e->tags.add(e.getAsString()));aspects=List.copyOf(tags);
        if(aspects.isEmpty())throw new IllegalArgumentException("Focus upgrade must have a nonzero payment: "+key);
        var targets=new LinkedHashSet<String>();if(row.has("foci"))row.getAsJsonArray("foci").forEach(e->targets.add(e.getAsString()));foci=Set.copyOf(targets);
        var levels=new LinkedHashSet<Integer>();if(row.has("ranks"))row.getAsJsonArray("ranks").forEach(e->levels.add(e.getAsInt()));ranks=Set.copyOf(levels);
        if(ranks.stream().anyMatch(rank->rank<1||rank>5))throw new IllegalArgumentException("Invalid focus upgrade rank "+key);
    }
    public String nameKey() { return name; }
    public String descriptionKey() { return description; }
    public ResourceLocation texture() { return ResourceLocation.parse(texture); }
    public static FocusUpgrade require(String key) { var value=REGISTRY.get(key);if(value==null)throw new IllegalArgumentException("Unknown upgrade "+key);return value; }
    public static Optional<FocusUpgrade> byId(int id) { return Optional.ofNullable(IDS.get(id)); }
    public static FocusUpgrade[] values() { return REGISTRY.values().toArray(FocusUpgrade[]::new); }
    public static int savedId(net.minecraft.nbt.CompoundTag tag) {
        if(tag.contains("key")) {var value=REGISTRY.get(tag.getString("key"));return value==null?-1:value.id;}
        return tag.getShort("id");
    }
    public boolean offered(ItemStack focus,int rank) {
        return foci.contains(net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(focus.getItem()).toString()) && ranks.contains(rank);
    }
}
