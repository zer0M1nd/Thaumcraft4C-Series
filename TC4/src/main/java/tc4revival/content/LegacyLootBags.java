package tc4revival.content;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public  class LegacyLootBags {
    private static volatile Map<ResourceLocation,Pool> pools=Map.of();
    private static final ResourceLocation DEFAULT=ResourceLocation.parse("thaumcraft:bags");
    private LegacyLootBags() {}
    private static  class Entry {
        final ItemStack stack; final int[] weights; final boolean prechargedAmulet;
        Entry(ItemStack stack,int[] weights,boolean prechargedAmulet) { this.stack=stack.copy(); this.weights=weights.clone(); this.prechargedAmulet=prechargedAmulet; }
    }
    public static  class Pool {
        private final List<Entry> entries;
        private final int[] totals=new int[3];
        private Pool(List<Entry> entries) {
            this.entries=List.copyOf(entries);
            for (int rarity=0;rarity<3;rarity++) {
                long total=0;
                for (var entry:entries) total+=entry.weights[rarity];
                if (total<1 || total>Integer.MAX_VALUE) throw new IllegalArgumentException("Invalid bag weight total");
                totals[rarity]=(int)total;
            }
        }
        public int totalWeight(int rarity) { return totals[rarity]; }
        public ItemStack generate(int rarity,RandomSource random) {
            if (rarity<0 || rarity>2) throw new IllegalArgumentException("Invalid bag rarity");
            if (rarity>0 && random.nextFloat()<.025f*rarity) return gear(rarity,random);
            int roll=random.nextInt(totals[rarity]);
            for (var entry:entries) {
                roll-=entry.weights[rarity]; if (roll>=0) continue;
                var stack=entry.prechargedAmulet?LegacyLootTemplates.prechargedAmulet(random::nextInt):entry.stack.copy();
                return stack.is(Items.BOOK)?LegacyRandomEnchantments.apply(stack,enchantingPower(rarity,random),random):stack;
            }
            throw new IllegalStateException("Invalid bag selection");
        }
    }
    public static Pool decode(JsonObject definition) {
        var entries=new ArrayList<Entry>();
        for (var value:GsonHelper.getAsJsonArray(definition,"entries")) {
            var row=value.getAsJsonObject(); var weights=GsonHelper.getAsJsonArray(row,"weights");
            if (weights.size()!=3) throw new IllegalArgumentException("A bag entry needs three rarity weights");
            var values=new int[3];
            for (int rarity=0;rarity<3;rarity++) { values[rarity]=weights.get(rarity).getAsInt(); if (values[rarity]<0) throw new IllegalArgumentException("Negative bag weight"); }
            var stack=LegacyLootTemplates.decode(GsonHelper.getAsJsonObject(row,"stack"));
            if (stack.isEmpty()) throw new IllegalArgumentException("Empty bag entry");
            entries.add(new Entry(stack,values,GsonHelper.getAsBoolean(GsonHelper.getAsJsonObject(row,"stack"),"precharged_amulet",false)));
        }
        return new Pool(entries);
    }
    public static ItemStack generate(int rarity,RandomSource random) {
        var pool=pools.get(DEFAULT);
        if (pool==null) throw new IllegalStateException("Legacy loot bags have not loaded");
        return pool.generate(rarity,random);
    }
    private static int enchantingPower(int rarity,RandomSource random) { return (int)(5+rarity*.75f*random.nextInt(18)); }
    private static ItemStack gear(int rarity,RandomSource random) {
        int quality=random.nextInt(2);
        for (float chance:new float[]{.2f,.15f,.1f,.095f,.095f}) if (random.nextFloat()<chance) quality++;
        var stack=new ItemStack(gearItem(random.nextInt(5),quality));
        stack.setDamageValue(random.nextInt(1+stack.getMaxDamage()/6));
        if (random.nextInt(4)<rarity) stack=LegacyRandomEnchantments.apply(stack,enchantingPower(rarity,random),random);
        return stack.copy();
    }
    public static Item gearItem(int slot,int quality) {
        if (slot<0 || slot>4 || quality<0 || quality>6) throw new IllegalArgumentException("Invalid old loot equipment slot/quality");
        if (slot==0) return switch (quality) {
            case 0->Items.IRON_AXE; case 1->Items.IRON_SWORD; case 2->Items.GOLDEN_AXE; case 3->Items.GOLDEN_SWORD;
            case 4->TCItems.tool(ThaumiumToolType.SWORD); case 5->Items.DIAMOND_SWORD; default->TCItems.voidTool(VoidEquipment.Tool.SWORD);
        };
        if (quality==4) return TCItems.armor(switch(slot) { case 1->ThaumiumArmorType.BOOTS; case 2->ThaumiumArmorType.LEGGINGS; case 3->ThaumiumArmorType.CHESTPLATE; default->ThaumiumArmorType.HELMET; });
        if (quality==6) return TCItems.voidArmor(switch(slot) { case 1->VoidEquipment.Armor.BOOTS; case 2->VoidEquipment.Armor.LEGGINGS; case 3->VoidEquipment.Armor.CHESTPLATE; default->VoidEquipment.Armor.HELMET; });
        Item[][] vanilla={
            {Items.LEATHER_BOOTS,Items.LEATHER_LEGGINGS,Items.LEATHER_CHESTPLATE,Items.LEATHER_HELMET},
            {Items.GOLDEN_BOOTS,Items.GOLDEN_LEGGINGS,Items.GOLDEN_CHESTPLATE,Items.GOLDEN_HELMET},
            {Items.CHAINMAIL_BOOTS,Items.CHAINMAIL_LEGGINGS,Items.CHAINMAIL_CHESTPLATE,Items.CHAINMAIL_HELMET},
            {Items.IRON_BOOTS,Items.IRON_LEGGINGS,Items.IRON_CHESTPLATE,Items.IRON_HELMET},
            {Items.DIAMOND_BOOTS,Items.DIAMOND_LEGGINGS,Items.DIAMOND_CHESTPLATE,Items.DIAMOND_HELMET}};
        return vanilla[quality==5 ? 4 : quality][slot-1];
    }
    public static void addReloadListener(net.minecraftforge.event.AddReloadListenerEvent event) { event.addListener(new Reload()); }
    public static void stopped(net.minecraftforge.event.server.ServerStoppedEvent event) { pools=Map.of(); }
    private static  class Reload extends SimpleJsonResourceReloadListener {
        Reload() { super(new Gson(),"legacy_bag_loot"); }
        @Override protected void apply(Map<ResourceLocation,JsonElement> files,ResourceManager resources,ProfilerFiller profiler) {
            var next=new java.util.LinkedHashMap<ResourceLocation,Pool>();
            files.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry->next.put(entry.getKey(),decode(entry.getValue().getAsJsonObject())));
            pools=Map.copyOf(next);
        }
    }
}
