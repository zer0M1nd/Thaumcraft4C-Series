package tc4revival.data;

import com.google.gson.*;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import net.minecraft.data.*;
import net.minecraftforge.registries.ForgeRegistries;

/** Generates ordinary loot tables; runtime never constructs these pools. */
public  class ModernLootProvider implements DataProvider {
    private final PackOutput output;
    private final Map<String,JsonObject> tables=new LinkedHashMap<>();
    public ModernLootProvider(PackOutput output) { this.output=output; }
    static JsonArray array(JsonElement... values) { var a=new JsonArray();for(var value:values)a.add(value);return a; }
    static JsonObject object(String key,String value) { var o=new JsonObject();o.addProperty(key,value);return o; }
    static JsonElement uniform(int min,int max) { if(min==max)return new JsonPrimitive(min);var o=object("type","minecraft:uniform");o.addProperty("min",min);o.addProperty("max",max);return o; }
    static JsonObject looting(float base,float multiplier) { var o=object("type","thaumcraft:looting");o.addProperty("base",base);o.addProperty("multiplier",multiplier);return o; }
    static JsonObject chance(float chance) { var o=object("condition","minecraft:random_chance");o.addProperty("chance",chance);return o; }
    static JsonObject lootingChance(float chance,float increment) { var o=object("condition","minecraft:random_chance_with_looting");o.addProperty("chance",chance);o.addProperty("looting_multiplier",increment);return o; }
    static JsonObject count(JsonElement amount) { var o=object("function","minecraft:set_count");o.add("count",amount);return o; }
    static JsonObject item(String name) { var o=object("type","minecraft:item");o.addProperty("name",name.contains(":")?name:"thaumcraft:"+name);return o; }
    static JsonObject ref(String name) { var o=object("type","minecraft:loot_table");o.addProperty("name","thaumcraft:"+name);return o; }
    static JsonObject state(String operation) { var o=object("function","thaumcraft:item_state");o.add("settings",object("operation",operation));return o; }
    static void function(JsonObject entry,JsonObject function) { if(!entry.has("functions"))entry.add("functions",new JsonArray());entry.getAsJsonArray("functions").add(function); }
    static JsonObject pool(JsonElement rolls,JsonArray entries,JsonObject... conditions) { var o=new JsonObject();o.add("rolls",rolls);o.add("entries",entries);if(conditions.length>0)o.add("conditions",array(conditions));return o; }
    static JsonObject table(String type,JsonObject... pools) { var o=object("type","minecraft:"+type);o.add("pools",array(pools));return o; }
    static JsonObject stack(JsonObject definition) {
        var e=item(definition.get("item").getAsString());
        if(definition.has("count"))function(e,count(definition.get("count")));
        if(definition.has("nbt")){var f=object("function","minecraft:set_nbt");f.add("tag",definition.get("nbt"));function(e,f);}
        if(definition.has("precharged_amulet")&&definition.get("precharged_amulet").getAsBoolean()){var f=state("charge");f.getAsJsonObject("settings").addProperty("maximum",4);function(e,f);}
        return e;
    }
    static JsonObject enchant(int base,float multiplier) { var f=state("enchant");var s=f.getAsJsonObject("settings");s.addProperty("base",base);s.addProperty("multiplier",multiplier);s.addProperty("bound",18);return f; }
    static JsonObject chest(JsonObject source,JsonElement rolls) {
        var entries=new JsonArray();
        for(var value:source.getAsJsonArray("entries")) {
            var row=value.getAsJsonObject();var entry=stack(row.getAsJsonObject("stack"));entry.add("weight",row.get("weight"));
            function(entry,count(uniform(row.get("min").getAsInt(),row.get("max").getAsInt())));
            if(row.has("random_book")&&row.get("random_book").getAsBoolean()){entry.addProperty("name","minecraft:book");function(entry,enchant(30,0));}
            entries.add(entry);
        }
        return table("chest",pool(rolls,entries));
    }
    private void bags() throws java.io.IOException {
        var source=LegacyBagLootProvider.definition(Path.of(System.getProperty("tc4revival.legacyBaseline")));
        for(int rarity=0;rarity<3;rarity++) {
            var entries=new JsonArray();
            for(var value:source.getAsJsonArray("entries")) {
                var row=value.getAsJsonObject();int weight=row.getAsJsonArray("weights").get(rarity).getAsInt();if(weight==0)continue;
                var entry=stack(row.getAsJsonObject("stack"));entry.addProperty("weight",weight);
                if(entry.get("name").getAsString().equals("minecraft:book"))function(entry,enchant(5,rarity*.75f));
                entries.add(entry);
            }
            tables.put("bags/items_"+rarity,table("generic",pool(new JsonPrimitive(1),entries)));
            var choice=object("type","minecraft:alternatives");var gear=ref("bags/gear_"+rarity);gear.add("conditions",array(chance(.025f*rarity)));
            choice.add("children",array(gear,ref("bags/items_"+rarity)));
            tables.put("bags/roll_"+rarity,table("generic",pool(new JsonPrimitive(1),array(choice))));
            tables.put("bags/"+tc4revival.content.LootBagItem.Kind.values()[rarity].path,table("chest",pool(uniform(8,12),array(ref("bags/roll_"+rarity)))));
            // Distribution of the old six independent quality rolls, encoded as weights.
            double[] distribution={.5,.5,0,0,0,0,0};
            for(double probability:new double[]{.2,.15,.1,.095,.095}) { var next=new double[7];for(int q=0;q<7;q++){next[q]+=distribution[q]*(1-probability);if(q<6)next[q+1]+=distribution[q]*probability;}distribution=next; }
            var gearEntries=new JsonArray();
            for(int q=0;q<7;q++)for(int slot=0;slot<5;slot++) {
                var e=item(ForgeRegistries.ITEMS.getKey(tc4revival.content.LegacyLootBags.gearItem(slot,q)).toString());e.addProperty("weight",Math.max(1,(int)Math.round(distribution[q]*100000000)));
                var damage=object("function","minecraft:set_damage");var amount=object("type","minecraft:uniform");amount.addProperty("min",5d/6);amount.addProperty("max",1);damage.add("damage",amount);function(e,damage);
                var ench=enchant(5,rarity*.75f);ench.add("conditions",array(chance(rarity/4f)));function(e,ench);gearEntries.add(e);
            }
            tables.put("bags/gear_"+rarity,table("generic",pool(new JsonPrimitive(1),gearEntries)));
        }
        tables.put("chests/dungeon",chest(LegacyDungeonLootProvider.definition(),new JsonPrimitive(8)));
        tables.put("chests/dungeon_double",chest(LegacyDungeonLootProvider.definition(),new JsonPrimitive(16)));
    }
    @Override public CompletableFuture<?> run(CachedOutput cache) {
        tables.clear();
        try {
            bags();entities();
            var writes=new ArrayList<CompletableFuture<?>>();
            tables.forEach((id,json)->writes.add(DataProvider.saveStable(cache,json,output.getOutputFolder(PackOutput.Target.DATA_PACK).resolve("thaumcraft/loot_tables/"+id+".json"))));
            return CompletableFuture.allOf(writes.toArray(CompletableFuture[]::new));
        }catch(Exception e){return CompletableFuture.failedFuture(e);}
    }
    static JsonObject entityState(String state) { var c=object("condition","thaumcraft:entity_state");c.addProperty("state",state);return c; }
    static JsonObject killed() { return object("condition","minecraft:killed_by_player"); }
    static JsonObject aspectItem(String item,String aspect,int amount) { var e=item(item);var f=state("aspect");f.getAsJsonObject("settings").addProperty("aspect",aspect);f.getAsJsonObject("settings").addProperty("amount",amount);function(e,f);return e; }
    static JsonObject once(JsonObject entry,JsonObject... conditions) { return pool(new JsonPrimitive(1),array(entry),conditions); }
    static JsonObject taint(float probability,int rolls,int tendrilWeight) { var tendril=item("tainted_tendril");tendril.addProperty("weight",tendrilWeight);return pool(new JsonPrimitive(rolls),array(item("tainted_goo"),tendril),chance(probability)); }
    private void entity(String name,JsonObject... pools) { tables.put("entities/"+name,table("entity",pools)); }
    private void entities() {
        for(boolean giant:new boolean[]{false,true}) {
            var flesh=item("minecraft:rotten_flesh");function(flesh,count(new JsonPrimitive(giant?2:1)));
            var binomial=object("type","minecraft:binomial");binomial.addProperty("n",giant?12:3);binomial.addProperty("p",.5);
            var rare=giant?array(item("thaumium_ingot"),item("minecraft:carrot"),item("minecraft:potato"),item("amber")):array(item("minecraft:iron_ingot"),item("minecraft:carrot"),item("minecraft:potato"));
            entity(giant?"giant_brainy_zombie":"brainy_zombie",pool(binomial,array(flesh)),once(item("zombie_brain"),lootingChance(.5f,.1f)),pool(new JsonPrimitive(1),rare,killed(),lootingChance(.025f,.01f)));
        }
        var dissolved=object("type","thaumcraft:inventory");dissolved.addProperty("source","entity_aspects");dissolved.addProperty("chance",.5);dissolved.addProperty("divisor",2);dissolved.addProperty("minimum",1);dissolved.add("result",object("item","thaumcraft:essence_crystal"));
        tables.put("legacy_mob_bonus/dissolution",table("entity",once(dissolved)));
        tables.put("legacy_mob_bonus/zombie_brain",table("entity",once(item("zombie_brain"),killed(),lootingChance(.1f,.1f))));
        tables.put("legacy_mob_bonus/gold_coin",table("entity",once(item("gold_coin"),lootingChance(.1f,.1f))));
        var championEntries=new JsonArray();
        for(int roll=0;roll<9;roll++) {
            var children=new JsonArray();
            for(int rarity=2;rarity>=0;rarity--) {
                var e=item(tc4revival.content.LootBagItem.Kind.values()[rarity].path);
                var condition=object("condition","minecraft:value_check");condition.add("value",looting(roll,1));var bounds=new JsonObject();bounds.addProperty("min",rarity*5);condition.add("range",bounds);e.add("conditions",array(condition));children.add(e);
            }
            var alternative=object("type","minecraft:alternatives");alternative.add("children",children);
            tables.put("legacy_mob_bonus/champion_"+roll,table("entity",once(alternative)));championEntries.add(ref("legacy_mob_bonus/champion_"+roll));
        }
        tables.put("legacy_mob_bonus/champion",table("entity",pool(new JsonPrimitive(1),championEntries)));
        var creeper=object("type","minecraft:loot_table");creeper.addProperty("name","minecraft:entities/creeper");entity("tainted_creeper",once(creeper));
        var fragment=item("knowledge_fragment");fragment.add("conditions",array(chance(.1f)));
        var seed=item("void_seed");seed.add("conditions",array(chance(1f/9)));
        var coin=item("gold_coin");coin.add("conditions",array(lootingChance(.25f,.125f)));
        var choices=object("type","minecraft:alternatives");choices.add("children",array(fragment,seed,coin));
        for(String name:new String[]{"cultist_knight","cultist_cleric"})entity(name,once(choices),once(item("crimson_rites"),killed(),lootingChance(.025f,.01f)));
        entity("cultist_leader",once(item("rare_loot_bag")));
        entity("cultist_portal",once(item("primordial_pearl")));
        var near=object("condition","thaumcraft:no_same_entity_nearby");near.addProperty("radius",48);
        entity("taintacle_giant",once(item("primordial_pearl"),near));
        for(String name:new String[]{"eldritch_golem","eldritch_warden"})entity(name,once(item("primordial_pearl")),once(item("rare_loot_bag")));
        var uniformLoot=object("type","minecraft:uniform");uniformLoot.addProperty("min",0);uniformLoot.add("max",looting(0,1));
        var check=object("condition","minecraft:value_check");check.add("value",uniformLoot);var range=new JsonObject();range.addProperty("min",1);check.add("range",range);
        var crabCondition=object("condition","minecraft:any_of");crabCondition.add("terms",array(chance(1f/3),check));
        entity("eldritch_crab",once(item("minecraft:ender_pearl"),killed(),crabCondition));
        entity("eldritch_guardian",once(aspectItem("wisp_essence","exanimis",2),chance(.5f)),once(aspectItem("wisp_essence","alienis",2),chance(.5f)),once(item("eldritch_eye"),killed(),lootingChance(.025f,.01f)));
        var powder=item("minecraft:gunpowder");function(powder,count(uniform(0,2)));var extra=count(uniformLoot);extra.addProperty("add",true);function(powder,extra);
        entity("fire_bat",once(powder,entityState("not_summoned")));
        entity("taintacle",taint(1,1,1));entity("taintacle_small");entity("mind_spider");
        entity("taint_hive",taint(1,2,1));entity("taint_spore",taint(1,1,1));entity("taint_spider",taint(1f/6,1,1));
        entity("taint_swarm",once(item("tainted_goo"),chance(.5f)));
        entity("thaumic_slime",once(item("tainted_goo"),chance(1f/3),entityState("small_slime")));
        entity("tainted_chicken",taint(1,1,3));entity("tainted_cow",taint(1,1,1));entity("tainted_pig",taint(1f/3,1,1));entity("tainted_sheep",taint(1f/3,1,1));
        entity("tainted_villager",taint(.5f,1,1),once(item("gold_coin"),lootingChance(1f/13,1f/13)));
        var essence=item("wisp_essence");var f=state("aspect");f.getAsJsonObject("settings").addProperty("from_entity",true);f.getAsJsonObject("settings").addProperty("amount",2);function(essence,f);entity("wisp",once(essence));
        var pack=object("type","thaumcraft:inventory");pack.addProperty("source","pech_pack");pack.addProperty("chance",.88);
        var beanEntries=new JsonArray();for(var aspect:tc4revival.aspect.TCAspects.catalog().primals())beanEntries.add(aspectItem("mana_bean",aspect.tag(),1));
        var beanRolls=object("type","minecraft:binomial");beanRolls.add("n",looting(1,1));beanRolls.addProperty("p",.5);
        entity("pech",once(pack),pool(beanRolls,beanEntries),once(item("common_loot_bag"),lootingChance(.1f,.1f)),once(item("knowledge_fragment"),killed(),lootingChance(.025f,.01f)));
    }
    @Override public String getName() { return "Modern native loot tables for bags, chests and creatures"; }
}
