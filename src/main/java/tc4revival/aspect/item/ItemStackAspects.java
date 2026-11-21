package tc4revival.aspect.item;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.registries.ForgeRegistries;
import tc4revival.TC4Revival;
import tc4revival.aspect.AspectAmounts;
import tc4revival.aspect.TCAspects;
import tc4revival.aspect.recipe.RecipeAspects;
import tc4revival.aspect.recipe.RecipeAspectData.Key;

/** Legacy stack values and identities. TC containers and wands integrate with their complete item implementations. */
public  class ItemStackAspects {
    private static volatile ItemAspectResolver equipment;
    private ItemStackAspects() {}
    public static void addReloadListener(AddReloadListenerEvent event) { event.addListener(new Reload()); }
    public static void stopped(ServerStoppedEvent event) { equipment = null; }
    public static void tagsUpdated() { var current=equipment;if(current!=null)current.clearTagCache(); }
    public static Optional<AspectAmounts> resolve(ItemStack stack) { return resolve(stack,ItemAspectAccess.SERVER); }
    public static Optional<AspectAmounts> resolve(ItemStack stack,ItemAspectAccess access) { return resolve(stack,access,false); }
    /** Scan rewards exclude mutable bulk contents without erasing part, enchantment or aspect-rule NBT. */
    public static Optional<AspectAmounts> resolveForScan(ItemStack stack) { return resolve(stack,ItemAspectAccess.SERVER,true); }
    public static Optional<AspectAmounts> resolve(ItemStack stack,ItemAspectAccess access,boolean scanning) {
        return resolveUncapped(stack,access,scanning).map(a -> tc4revival.aspect.recipe.GeneratedAspectLimit.limitTypes(a,access.catalog(),10));
    }
    private static Optional<AspectAmounts> resolveUncapped(ItemStack stack,ItemAspectAccess access,boolean scanning) {
        var extension=thaumcraft.api.aspects.ItemAspectProviders.resolve(new thaumcraft.api.aspects.ItemAspectProviders.Query(stack,access,scanning));
        if(extension.handled())return extension.aspects();
        if(!stack.isEmpty()){
            var id=ForgeRegistries.ITEMS.getKey(stack.getItem()).toString();
            var edited=access.direct().editorOverride(id,tag->stack.is(net.minecraft.tags.ItemTags.create(new ResourceLocation(tag))),stack.getTag());
            if(edited.isPresent())return edited;
            if(access.direct().hasNbtRules(id)) {
                java.util.function.Predicate<String> tags=tag->stack.is(net.minecraft.tags.ItemTags.create(new ResourceLocation(tag)));
                var base=access.direct().resolve(id,tags,stack.getTag()).orElseGet(AspectAmounts::new);
                var contained=stack.getItem() instanceof tc4revival.content.StoredAspectItem container?(scanning?container.scanAspects(stack):container.storedAspects(stack)):null;
                var bonus=access.equipment().resolve(id,tags,stack.getTag()).orElse(null);
                return Optional.of(new StackAspectBonuses(access.catalog()).apply(base,contained,bonus,enchantments(stack)));
            }
        }
        if (stack.getItem() instanceof tc4revival.content.CastingWandItem wand) {
            var id = ForgeRegistries.ITEMS.getKey(stack.getItem()).toString();
            var base = access.baseSession().base(id, new Key("Thaumcraft:WandCasting", wand.legacyMetadata(stack))).orElseGet(AspectAmounts::new).copy();
            int value = wand.rod(stack).craftCost + wand.cap(stack).craftCost;
            base.merge(access.catalog().require("praecantatio"), Math.min(64, value / 2));
            base.merge(access.catalog().require("instrumentum"), Math.min(64, value / 3));
            return Optional.of(new StackAspectBonuses(access.catalog()).apply(base, null, null, enchantments(stack)));
        }
        if (stack.is(Items.POTION) || stack.is(Items.SPLASH_POTION)) {
            return access.potionKey(stack).map(key -> new StackAspectBonuses(access.catalog()).apply(
                    access.recipeObject(key), null, null, enchantments(stack)));
        }
        if (stack.is(Items.FILLED_MAP) || stack.getItem() instanceof SpawnEggItem || stack.getItem() instanceof tc4revival.content.EssentiaMirrorItem || stack.getItem() instanceof tc4revival.content.ItemMirrorItem || stack.getItem() instanceof tc4revival.content.SinisterStoneItem) {
            String id = ForgeRegistries.ITEMS.getKey(stack.getItem()).toString();
            return identity(stack,access).map(key -> new StackAspectBonuses(access.catalog()).apply(
                    access.baseSession().base(id, key).orElse(null), null, null, enchantments(stack)));
        }
        return ordinary(stack,access,scanning);
    }
    public static Optional<Key> identity(ItemStack stack) { return identity(stack,ItemAspectAccess.SERVER); }
    public static Optional<Key> identity(ItemStack stack,ItemAspectAccess access) {
        if (stack.isEmpty()) return Optional.empty();
        if (stack.getItem() instanceof tc4revival.content.SinisterStoneItem) return Optional.of(new Key("Thaumcraft:ItemCompassStone",tc4revival.content.SinisterStoneItem.legacyMetadata(stack)));
        if (stack.getItem() instanceof tc4revival.content.ItemMirrorItem) return Optional.of(new Key("Thaumcraft:blockMirror",tc4revival.content.ItemMirrorItem.legacyMetadata(stack)));
        if (stack.getItem() instanceof tc4revival.content.EssentiaMirrorItem)
            return Optional.of(new Key("Thaumcraft:blockMirror",tc4revival.content.EssentiaMirrorItem.legacyMetadata(stack)));
        if (stack.getItem() instanceof tc4revival.content.CastingWandItem wand) return Optional.of(new Key("Thaumcraft:WandCasting", wand.legacyMetadata(stack)));
        if (stack.is(Items.POTION) || stack.is(Items.SPLASH_POTION)) return access.potionKey(stack);
        String id = ForgeRegistries.ITEMS.getKey(stack.getItem()).toString();
        return Optional.ofNullable(access.identity().stackKey(id,
                stack.is(Items.FILLED_MAP) ? MapItem.getMapId(stack) : null, access.spawnEggs()));
    }
    public static OptionalInt scanHash(ItemStack stack) { return scanHash(stack,ItemAspectAccess.SERVER); }
    public static OptionalInt scanHash(ItemStack stack,ItemAspectAccess access) {
        if(!stack.isEmpty()) {
            String id=ForgeRegistries.ITEMS.getKey(stack.getItem()).toString();
            java.util.function.Predicate<String> tags=tag->stack.is(net.minecraft.tags.ItemTags.create(new ResourceLocation(tag)));
            String base=access.direct().nbtIdentity(id,tags,stack.getTag()),bonus=access.equipment().nbtIdentity(id,tags,stack.getTag());
            if(!base.isEmpty()||!bonus.isEmpty())return OptionalInt.of((id+":nbt:"+base+":bonus:"+bonus).hashCode());
        }
        var key = identity(stack,access);
        if (key.isPresent()) return OptionalInt.of(access.identity().scanHash(key.get()));
        if (stack.isEmpty()) return OptionalInt.empty();
        String id = ForgeRegistries.ITEMS.getKey(stack.getItem()).toString();
        return access.direct().contains(id) || !enchantments(stack).isEmpty() ? OptionalInt.of((id + ":-1").hashCode()) : OptionalInt.empty();
    }
    public static Optional<AspectAmounts> ordinary(ItemStack stack) { return ordinary(stack,ItemAspectAccess.SERVER); }
    public static Optional<AspectAmounts> ordinary(ItemStack stack,ItemAspectAccess access) { return ordinary(stack,access,false).map(a -> tc4revival.aspect.recipe.GeneratedAspectLimit.limitTypes(a,access.catalog(),10)); }
    private static Optional<AspectAmounts> ordinary(ItemStack stack,ItemAspectAccess access,boolean scanning) {
        if (stack.isEmpty()) return Optional.empty();
        var current = access.equipment();
        if (current == null) throw new IllegalStateException("Equipment aspect data has not loaded");
        var id = ForgeRegistries.ITEMS.getKey(stack.getItem()).toString();
        var session = access.baseSession();
        var enchantments=enchantments(stack);
        if (session.identity().recipeKey(id) == null && !session.hasCraftingItem(id) && !access.direct().contains(id) && !current.contains(id) && enchantments.isEmpty()) return Optional.empty();
        var contained = stack.getItem() instanceof tc4revival.content.StoredAspectItem container ? (scanning ? container.scanAspects(stack) : container.storedAspects(stack)) : null;
        return Optional.of(new StackAspectBonuses(access.catalog()).apply(session.base(id).orElse(null), contained,
                current.resolve(id, tag -> stack.is(net.minecraft.tags.ItemTags.create(new ResourceLocation(tag))),stack.getTag()).orElse(null), enchantments));
    }

    public static ItemAspectResolver equipmentResolver(){
        var current=equipment;if(current==null)throw new IllegalStateException("Equipment aspect data has not loaded");return current;
    }
    private static List<StackAspectBonuses.EnchantmentContribution> enchantments(ItemStack stack) {
        var levels = new ArrayList<StackAspectBonuses.EnchantmentContribution>();
        if (stack.hasTag()) {
            String label = stack.is(Items.ENCHANTED_BOOK) ? "StoredEnchantments" : "Enchantments";
            var entries = stack.getTag().getList(label, Tag.TAG_COMPOUND);
            for (int i = 0; i < entries.size(); i++) {
                var entry = entries.getCompound(i);
                var id = ResourceLocation.tryParse(entry.getString("id"));
                short level = entry.getShort("lvl");
                if (id != null && level > 0 && ForgeRegistries.ENCHANTMENTS.containsKey(id))
                    levels.add(new StackAspectBonuses.EnchantmentContribution(
                            thaumcraft.api.aspects.EnchantmentAspects.special(id.toString()), level));
            }
        }
        return List.copyOf(levels);
    }
    private static  class Reload extends SimpleJsonResourceReloadListener {
        Reload() { super(new Gson(), "item_bonus_aspects"); }
        @Override protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
            var rules = new ArrayList<ItemAspectRule>();
            files.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> rules.addAll(ItemAspectJson.decodeKnown(entry.getValue(), TCAspects.catalog(), AspectRegistryTargets::item)));
            equipment = new ItemAspectResolver(rules,TCAspects.catalog(),true,tc4revival.aspect.recipe.InferenceLimits.server(),AspectRegistryTargets.itemTags());
            tc4revival.aspect.sync.ServerAspectData.capture("item_bonus_aspects",files);
            TC4Revival.LOGGER.info("Loaded {} legacy equipment aspect bonus rules", equipment.ruleCount());
        }
    }
}
