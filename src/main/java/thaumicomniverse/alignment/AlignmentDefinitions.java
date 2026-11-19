package thaumicomniverse.alignment;

import com.google.gson.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.resources.*;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.registries.ForgeRegistries;
import tc4revival.aspect.item.ItemStackAspects;

public final class AlignmentDefinitions extends SimpleJsonResourceReloadListener {
    public record Recipe(String id,String research,Ingredient center,List<Ingredient> near,ItemStack result,int minimum,int maximum,
            double risk,double stabilityPerPair,double quantityRisk,double kindRisk,int duration,java.util.function.Predicate<ItemStack> offerings,JsonObject answer,JsonObject raw) {}
    public record Target(String fingerprint,AlignmentMath.Summary summary) {}
    private static volatile SortedMap<String,Recipe> recipes=new TreeMap<>();
    private static final Map<String,Target> TARGETS=new HashMap<>();
    public AlignmentDefinitions() {super(new Gson(),"alignment");}
    public static Collection<Recipe> recipes() {return List.copyOf(recipes.values());}
    public static Recipe get(String id) {return recipes.get(id);}
    public static Map<String,Long> aspects(ItemStack stack) {
        var result=new TreeMap<String,Long>();ItemStackAspects.resolve(stack).ifPresent(a->a.aspects().forEach(kind->{if(a.amount(kind)>0)result.put(kind.tag(),(long)a.amount(kind));}));return result;
    }
    /** Cosmetic NBT does not add a kind; item and actual aspect content do. */
    public static String identity(ItemStack stack) {return ForgeRegistries.ITEMS.getKey(stack.getItem())+":"+aspects(stack);}
    public static AlignmentMath.Offering offering(ItemStack stack,int sign) {return new AlignmentMath.Offering(identity(stack),stack.getCount(),sign,aspects(stack));}
    public static ItemStack stack(JsonObject json) {
        var id=ResourceLocation.parse(json.get("item").getAsString());if(!ForgeRegistries.ITEMS.containsKey(id))throw new IllegalArgumentException("Unknown alignment item "+id);
        int count=json.has("count")?json.get("count").getAsInt():1;if(count<1||count>64)throw new IllegalArgumentException("Invalid alignment count "+count);
        var stack=new ItemStack(ForgeRegistries.ITEMS.getValue(id),count);if(stack.isEmpty())throw new IllegalArgumentException("Alignment cannot use air");
        if(json.has("nbt"))try{stack.setTag(TagParser.parseTag(json.get("nbt").getAsString()));}catch(com.mojang.brigadier.exceptions.CommandSyntaxException e){throw new IllegalArgumentException(e);}
        if(stack.hasTag()&&stack.getTag().sizeInBytes()>8192)throw new IllegalArgumentException("Alignment NBT exceeds 8 KiB");return stack;
    }
    public static String hash(String text) {try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));}catch(java.security.NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
    @Override protected void apply(Map<ResourceLocation,JsonElement> files,ResourceManager manager,ProfilerFiller profiler) {
        var next=new TreeMap<String,Recipe>();
        files.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(file->{var json=file.getValue().getAsJsonObject();
            if(json.has("requiredMod")&&!net.minecraftforge.fml.ModList.get().isLoaded(json.get("requiredMod").getAsString()))return;
            var near=new ArrayList<Ingredient>();for(var element:json.getAsJsonArray("near"))near.add(Ingredient.fromJson(element));
            if(near.size()>8)throw new IllegalArgumentException("Alignment near ring holds at most 8 items");
            int min=json.get("minimum").getAsInt(),max=json.get("maximum").getAsInt(),duration=json.has("duration")?json.get("duration").getAsInt():200;
            var output=stack(json.getAsJsonObject("result"));AlignmentMath.yield(min,max,0);
            if(max>output.getMaxStackSize()||duration<20||duration>72000)throw new IllegalArgumentException("Alignment yield/duration bounds");
            double risk=number(json,"risk",1),stability=number(json,"stabilityPerPair",.25),quantity=number(json,"quantityRisk",1),kinds=number(json,"kindRisk",1);
            var answer=json.getAsJsonObject("answer");if(answer==null || answer.has("explicit")==answer.has("random"))throw new IllegalArgumentException("Exactly one answer mode is required");
            if(answer.has("random"))AlignmentPredicates.decode(answer.getAsJsonObject("random").getAsJsonObject("predicate"));
            var offerings=AlignmentPredicates.decode(json.has("offerings")?json.getAsJsonObject("offerings"):new JsonObject());
            next.put(file.getKey().toString(),new Recipe(file.getKey().toString(),json.get("research").getAsString(),Ingredient.fromJson(json.get("center")),List.copyOf(near),output,min,max,risk,stability,quantity,kinds,duration,offerings,answer.deepCopy(),json.deepCopy()));
        });recipes=Collections.unmodifiableSortedMap(next);TARGETS.clear();
    }
    private static double number(JsonObject json,String key,double fallback) {double value=json.has(key)?json.get(key).getAsDouble():fallback;if(!Double.isFinite(value)||value<0||value>1000)throw new IllegalArgumentException("Invalid alignment "+key);return value;}
    public static Target target(ServerLevel level,Recipe recipe) {
        String cacheKey=level.getServer().overworld().getSeed()+":"+recipe.id();var cached=TARGETS.get(cacheKey);if(cached!=null)return cached;
        var answer=recipe.answer();var offerings=new ArrayList<AlignmentMath.Offering>();String candidateFingerprint="";
        if(answer.has("explicit")) {
            for(var element:answer.getAsJsonArray("explicit")){var json=element.getAsJsonObject();var stack=stack(json);if(!recipe.offerings().test(stack))throw new IllegalArgumentException("Standard answer rejected by offerings predicate: "+recipe.id());offerings.add(offering(stack,json.get("sign").getAsInt()));}
            candidateFingerprint=offerings.toString();
        } else {
            var random=answer.getAsJsonObject("random");var predicate=AlignmentPredicates.decode(random.getAsJsonObject("predicate"));
            var candidates=ForgeRegistries.ITEMS.getKeys().stream().sorted().map(id->new ItemStack(ForgeRegistries.ITEMS.getValue(id))).filter(s->!s.isEmpty()).filter(predicate).filter(recipe.offerings()).filter(s->!aspects(s).isEmpty()).toList();
            if(candidates.isEmpty())throw new IllegalArgumentException("No candidates for alignment "+recipe.id());
            int draws=random.get("draws").getAsInt(),min=random.get("minCount").getAsInt(),max=random.get("maxCount").getAsInt();
            if(draws<1||draws>32||min<1||max<min||max>64)throw new IllegalArgumentException("Alignment random bounds invalid");
            var text=new StringBuilder();for(var stack:candidates)text.append(identity(stack)).append(aspects(stack));candidateFingerprint=hash(text.toString());
            long seed=random.get("seed").getAsLong()^Long.parseUnsignedLong(hash(recipe.id()+candidateFingerprint).substring(0,16),16);
            if(random.has("worldSeed")&&random.get("worldSeed").getAsBoolean())seed^=level.getServer().overworld().getSeed();
            var rng=new Random(seed);boolean negative=random.has("negative")&&random.get("negative").getAsBoolean();
            for(int i=0;i<draws;i++){var stack=candidates.get(rng.nextInt(candidates.size())).copyWithCount(min+rng.nextInt(max-min+1));offerings.add(offering(stack,negative&&rng.nextBoolean()?-1:1));}
        }
        if(offerings.isEmpty()||offerings.size()>32)throw new IllegalArgumentException("Alignment answer needs 1..32 offerings");
        String fingerprint=hash(recipe.raw()+candidateFingerprint);
        var saved=AlignmentTargets.get(level).resolve(recipe.id()+"@"+fingerprint,AlignmentMath.summarize(offerings));
        var target=new Target(fingerprint,saved);TARGETS.put(cacheKey,target);return target;
    }
}
