package tc4revival.research;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import tc4revival.aspect.entity.EntityAspects;
import tc4revival.aspect.item.BlockScanMapping;
import tc4revival.aspect.item.ItemStackAspects;
import tc4revival.aspect.item.LegacyItemMappings;
import tc4revival.aspect.recipe.RecipeAspectData.Key;
import tc4revival.aspect.recipe.RecipeAspects;

/** Server observation adapters. A finished observation never retains a mutable live entity or stack. */
public  class ResearchClues {
    private ResearchClues() {}

    public static java.util.List<String> restoreKnownItems(PlayerKnowledge knowledge) {
        if(knowledge.scannedKeys(PlayerKnowledge.ScanKind.OBJECT).isEmpty())return java.util.List.of();
        var displays=ResearchDisplayItems.bundled();
        return ResearchCatalogs.rules().restoreItemClues(knowledge,trigger->{
            // Old scan hashes do not retain arbitrary stack NBT. They cannot prove
            // that a particular tagged variant was seen; normal scans still match it exactly.
            if(trigger.nbt().isPresent())return false;
            if(trigger.ore().isPresent()){
                if(!trigger.ore().get().equals("quicksilver"))return false;
                var tag=net.minecraft.tags.ItemTags.create(ResourceLocation.parse(materialTag(trigger.ore().get())));
                var items=ForgeRegistries.ITEMS.tags();
                if(items!=null)for(var item:items.getTag(tag)){
                    var stack=new ItemStack(item);
                    if(!stack.isEmpty()&&knowledge.scanned(PlayerKnowledge.ScanKind.OBJECT,ScanIdentity.item(stack)))return true;
                }
                return false;
            }
            int variants=displays.find(trigger).map(e->Math.max(1,e.variants().size())).orElse(1);
            for(int i=0;i<variants;i++){
                var stack=displays.resolve(trigger,i*1000L);
                if(stack.isPresent()&&knowledge.scanned(PlayerKnowledge.ScanKind.OBJECT,ScanIdentity.item(stack.get())))return true;
            }
            return false;
        });
    }

    public static ResearchRules.ItemClue dropped(ItemStack original) {
        var snapshot = original.copyWithCount(1);
        // Addon triggers use modern registry ids even when no legacy aspect alias exists.
        // Keep the original legacy metadata where one does exist, not grouped reward metadata.
        var key = ItemStackAspects.identity(snapshot).orElseGet(() ->
                new Key(ForgeRegistries.ITEMS.getKey(snapshot.getItem()).toString(), 0));
        var nbt = snapshot.hasTag() ? snapshot.getTag().copy() : null;
        // Since flattening, vanilla damage lives in the compound; old ItemStack kept it outside stackTagCompound.
        if (nbt != null && snapshot.getItem().canBeDepleted() && nbt.contains("Damage")) {
            nbt.remove("Damage");
            if (nbt.isEmpty()) nbt = null;
        }
        return item(key, snapshot.isDamageableItem(), nbt, tags(snapshot));
    }

    public static ResearchRules.ItemClue block(BlockScanMapping.Target target) {
        var key = target.legacyKey() == null ? null : LegacyItemMappings.identity().scanKey(target.legacyKey());
        var properties = key == null ? null : RecipeAspects.data().items().get(key.item());
        var tags = Set.<String>of();
        String modern = target.modernItem();
        if (key != null && !key.equals(target.legacyKey())) {
            // Grouped scan metadata also supplies the clue's material tags, not the pre-group picked item.
            modern = LegacyItemMappings.identity().items().entrySet().stream().filter(entry -> entry.getValue().equals(key))
                    .map(java.util.Map.Entry::getKey).sorted().findFirst().orElse(null);
        }
        if (modern != null) tags = tags(new ItemStack(Objects.requireNonNull(ForgeRegistries.ITEMS.getValue(ResourceLocation.parse(modern)))));
        var resolvedKey = key == null && modern != null ? new Key(modern, 0) : key;
        return item(resolvedKey, properties != null && properties.damageable(), null, tags);
    }

    public static Optional<ResearchRules.Clue> entity(Entity entity) {
        return EntityAspects.legacyName(entity).map(ResearchRules.EntityClue::new);
    }

    private static Set<String> tags(ItemStack stack) {
        return stack.getTags().map(tag -> tag.location().toString()).collect(Collectors.toUnmodifiableSet());
    }
    private static ResearchRules.ItemClue item(Key key, boolean damageable, CompoundTag nbt, Set<String> tags) {
        return new ResearchRules.ItemClue(trigger -> {
            boolean material = trigger.ore().map(ResearchClues::materialTag).filter(tags::contains).isPresent();
            return ResearchItemMatch.matches(trigger, key, damageable, Objects.equals(nbt, readNbt(trigger.nbt())), material);
        });
    }
    /** The default base catalog has exactly one OreDictionary-backed trigger material. */
    public static String materialTag(String legacyOre) {
        return switch (legacyOre) {
            case "quicksilver" -> "forge:quicksilver";
            default -> throw new IllegalArgumentException("Unmapped research trigger material " + legacyOre);
        };
    }
    public static CompoundTag readNbt(Optional<String> nbt) {
        return nbt.map(LegacyStackNbt::read).orElse(null);
    }
}
