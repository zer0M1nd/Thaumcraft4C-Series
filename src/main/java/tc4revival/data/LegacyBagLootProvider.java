package tc4revival.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.Dynamic;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import net.minecraft.SharedConstants;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraftforge.registries.ForgeRegistries;
import tc4revival.aspect.item.PotionAspectJson;
import tc4revival.aspect.item.PotionAspectRules;
import tc4revival.aspect.item.PotionAspects;
import tc4revival.content.ElementalRingType;
import tc4revival.content.LegacyPotionEffects;

public  class LegacyBagLootProvider implements DataProvider {
    private final PackOutput output;
    public LegacyBagLootProvider(PackOutput output) { this.output=output; }
    public static JsonObject definition(Path baseline) throws java.io.IOException {
        var entries=new JsonArray();
        add(entries,stack("thaumcraft:gold_coin",1),2500,0,0);
        add(entries,stack("thaumcraft:gold_coin",2),0,2250,0);
        add(entries,stack("thaumcraft:gold_coin",3),0,0,2000);
        add(entries,stack("thaumcraft:primordial_pearl",1),0,0,1);
        add(entries,stack("minecraft:nether_star",1),0,0,1);
        add(entries,stack("minecraft:diamond",1),10,50,50);
        add(entries,stack("minecraft:emerald",1),15,75,75);
        add(entries,stack("minecraft:gold_ingot",1),100,100,100);
        add(entries,stack("minecraft:ender_pearl",1),100,100,100);
        add(entries,stack("thaumcraft:knowledge_fragment",1),25,25,25);
        for (String type:new String[]{"amulet","ring","belt"}) add(entries,stack("thaumcraft:mundane_"+type,1),10,0,0);
        for (var type:ElementalRingType.values()) add(entries,stack("thaumcraft:"+type.path(),1),0,5,7);
        var amulet=stack("thaumcraft:lesser_vis_amulet",1); amulet.addProperty("precharged_amulet",true); add(entries,amulet,0,6,6);
        add(entries,stack("thaumcraft:runic_ring_lesser",1),0,5,5);
        add(entries,stack("minecraft:experience_bottle",1),5,10,20);
        add(entries,stack("minecraft:enchanted_golden_apple",1),1,2,3);
        add(entries,stack("minecraft:golden_apple",1),3,6,9);
        add(entries,stack("minecraft:book",1),10,10,10);
        var old=PotionAspectJson.definitions(JsonParser.parseString(Files.readString(baseline.resolve("potion-aspects.json"))).getAsJsonArray());
        for (int base=0;base<=15;base++) for (int splash=0;splash<=1;splash++) for (int variant=0;variant<=2;variant++) {
            int metadata=base | (splash==0 ? 0x2000 : 0x4000) | (variant==1 ? 0x20 : variant==2 ? 0x40 : 0);
            var definition=old.get(metadata);
            if (definition==null) throw new IllegalStateException("Missing old potion evidence for "+metadata);
            if (definition.effects().isEmpty()) continue;
            add(entries,potion(metadata,definition),variant+1,variant+1,variant+1);
        }
        var root=new JsonObject(); root.add("entries",entries); return root;
    }
    private static JsonObject potion(int metadata,PotionAspectRules.Definition definition) {
        var legacy=new CompoundTag(); legacy.putString("id","minecraft:potion"); legacy.putShort("Damage",(short)metadata);
        legacy.putByte("Count",(byte)1); legacy.put("tag",new CompoundTag());
        var fixed=DataFixers.getDataFixer().update(References.ITEM_STACK,new Dynamic<>(NbtOps.INSTANCE,legacy),99,SharedConstants.getCurrentVersion().getDataVersion().getVersion());
        var converted=(CompoundTag)fixed.getValue(); var tag=converted.getCompound("tag");
        var potion=ForgeRegistries.POTIONS.getValue(ResourceLocation.parse(tag.getString("Potion")));
        if (potion==null || potion.getEffects().size()!=definition.effects().size()) throw new IllegalStateException("Unrepresented old loot potion "+metadata);
        var effects=new ListTag();
        for (int index=0;index<definition.effects().size();index++) {
            var old=definition.effects().get(index);
            var effect=ForgeRegistries.MOB_EFFECTS.getValue(ResourceLocation.parse(PotionAspectRules.modernEffect(old.id())));
            if (effect==null || potion.getEffects().get(index).getEffect()!=effect || potion.getEffects().get(index).getAmplifier()!=old.amplifier())
                throw new IllegalStateException("Mismatched old loot potion "+metadata);
            effects.add(new MobEffectInstance(effect,old.duration(),old.amplifier()).save(new CompoundTag()));
        }
        tag.putInt(PotionAspects.LEGACY_METADATA_TAG,metadata); tag.put(LegacyPotionEffects.TAG,effects);
        var result=stack(converted.getString("id"),1); result.addProperty("nbt",tag.toString()); return result;
    }
    private static JsonObject stack(String id,int count) { var stack=new JsonObject(); stack.addProperty("item",id); stack.addProperty("count",count); return stack; }
    private static void add(JsonArray entries,JsonObject stack,int common,int uncommon,int rare) {
        var row=new JsonObject(); row.add("stack",stack); var weights=new JsonArray(); weights.add(common); weights.add(uncommon); weights.add(rare); row.add("weights",weights); entries.add(row);
    }
    @Override public CompletableFuture<?> run(CachedOutput cache) {
        try { return DataProvider.saveStable(cache,definition(Path.of(System.getProperty("tc4revival.legacyBaseline"))),output.getOutputFolder(PackOutput.Target.DATA_PACK).resolve("thaumcraft/legacy_bag_loot/bags.json")); }
        catch (java.io.IOException failure) { return CompletableFuture.failedFuture(failure); }
    }
    @Override public String getName() { return "TC4 complete three-tier loot bag pools"; }
}
