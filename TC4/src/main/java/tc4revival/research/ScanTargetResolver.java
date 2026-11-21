package tc4revival.research;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;
import tc4revival.aspect.AspectAmounts;
import tc4revival.aspect.entity.EntityAspects;
import tc4revival.aspect.item.BlockScanTargets;
import tc4revival.aspect.item.ItemStackAspects;
import tc4revival.aspect.item.LegacyItemMappings;
import tc4revival.research.PlayerKnowledge.ScanKind;

/** Identity/value resolution after target selection. These methods do not perform range or sight checks. */
public  class ScanTargetResolver {
    private ScanTargetResolver() {}

    public static Optional<ScanTarget> block(ServerLevel level, BlockPos position) {
        requireServerThread(level);
        if (!level.hasChunkAt(position)) return Optional.empty();
        var custom=thaumcraft.api.research.ScanRegistry.block(level,position);if(custom.isPresent())return custom;
        if (level.getBlockEntity(position) instanceof tc4revival.content.ScannableNode node) {
            var rewards = new AspectAmounts(); var aspects = node.aspects(); var catalog = tc4revival.aspect.TCAspects.catalog();
            for (var aspect : aspects.alphabetical()) rewards.merge(aspect, Math.max(4, aspects.amount(aspect) / 10));
            switch (node.nodeType()) {
                case UNSTABLE -> rewards.merge(catalog.require("perditio"), 4);
                case HUNGRY -> rewards.merge(catalog.require("fames"), 4);
                case TAINTED -> rewards.merge(catalog.require("vitium"), 4);
                case PURE -> { rewards.merge(catalog.require("sano"), 2); rewards.add(catalog.require("ordo"), 2); }
                case DARK -> { rewards.merge(catalog.require("mortuus"), 2); rewards.add(catalog.require("tenebrae"), 2); }
                default -> {}
            }
            String id = "NODE" + node.nodeId();
            return Optional.of(new ScanTarget(new ScanTarget.Phenomenon(id), ScanKind.PHENOMENON, Optional.of(id), rewards));
        }
        var state = tc4revival.content.WardedBlock.queryOriginal(level, position);
        return blockState(state);
    }

    /** Resolve a transported block's normal identity without looking up unrelated world coordinates. */
    public static Optional<ScanTarget> blockState(net.minecraft.world.level.block.state.BlockState state) {
        if (state.isAir()) return Optional.empty();
        var edited=tc4revival.aspect.editor.EditorResources.block(state);
        if(edited.isPresent())return Optional.of(new ScanTarget(new ScanTarget.Block(state.toString(),0),ScanKind.OBJECT,
                Optional.of("modern:block:"+ForgeRegistries.BLOCKS.getKey(state.getBlock())),edited.get()));
        var mapped = BlockScanTargets.target(state);
        var explicit=tc4revival.aspect.item.BlockAspects.resolve(state);
        if (mapped.isPresent()) {
            var target = mapped.get();
            var key = target.legacyKey() == null ? null : LegacyItemMappings.identity().scanKey(target.legacyKey());
            var continuity = key == null ? new ScanTarget.Block(target.modernItem(), 0) : new ScanTarget.Block(key.item(), key.metadata());
            return Optional.of(new ScanTarget(continuity, ScanKind.OBJECT,
                    Optional.of(Integer.toString(BlockScanTargets.hash(target))), explicit.orElseGet(()->BlockScanTargets.aspects(target)),
                    Optional.of(ResearchClues.block(target))));
        }
        // Explicit mappings retain priority; ordinary new blocks share their item's
        // complete scan identity, aspects and clues, without copying block-entity NBT.
        if(state.getBlock().asItem()!=Items.AIR){var result=inventory(new ItemStack(state.getBlock()));return explicit.isEmpty()?result:result.map(t->new ScanTarget(t.continuity(),t.kind(),t.identity(),explicit.get(),t.clue()));}
        String modernIdentity="modern:block:"+ForgeRegistries.BLOCKS.getKey(state.getBlock());
        // 0.4.101 recorded soul fire as scanned even though its authored aspects
        // were empty. Give the repaired target a one-time rewarding identity.
        if (state.is(net.minecraft.world.level.block.Blocks.SOUL_FIRE)) modernIdentity += ":aspects_v1";
        return Optional.of(new ScanTarget(new ScanTarget.Block(state.toString(), 0), ScanKind.OBJECT,
                Optional.of(modernIdentity), explicit.orElseGet(AspectAmounts::new)));
    }

    public static Optional<ScanTarget> entity(Entity entity) {
        var target=entityBase(entity);
        var edited=tc4revival.aspect.editor.EditorResources.entity(entity.getType());
        return edited.isEmpty()?target:target.map(t->new ScanTarget(t.continuity(),t.kind(),t.identity().isPresent()?t.identity():
                Optional.of("modern:entity:"+ForgeRegistries.ENTITY_TYPES.getKey(entity.getType())),edited.get(),t.clue()));
    }
    private static Optional<ScanTarget> entityBase(Entity entity) {
        if (!(entity.level() instanceof ServerLevel level)) throw new IllegalArgumentException("Scan target must be server-owned");
        requireServerThread(level);
        if (entity.isRemoved()) return Optional.empty();
        var custom=thaumcraft.api.research.ScanRegistry.entity(entity);if(custom.isPresent())return custom;
        var continuity = new ScanTarget.Entity(entity.getUUID());
        if (entity instanceof ItemEntity dropped) {
            if (dropped.getItem().isEmpty()) return Optional.empty();
            var hash = ItemStackAspects.scanHash(dropped.getItem());
            return Optional.of(new ScanTarget(continuity, ScanKind.OBJECT,
                    Optional.of(ScanIdentity.item(dropped.getItem())),
                    ItemStackAspects.resolveForScan(groupedStack(dropped.getItem())).orElseGet(AspectAmounts::new),
                    Optional.of(ResearchClues.dropped(dropped.getItem()))));
        }
        var result = EntityAspects.resolve(entity);
        return Optional.of(new ScanTarget(continuity, ScanKind.ENTITY,
                Optional.of(result.map(value -> Integer.toString(value.scanHash())).orElseGet(()->"modern:entity:"+ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()))), result.map(value -> value.aspects()).orElseGet(AspectAmounts::new),
                ResearchClues.entity(entity)));
    }

    /** Grouped scan metadata applies before bonuses, while preserving the actual stack's NBT. */
    public static Optional<ScanTarget> inventory(ItemStack stack) {
        if(stack.isEmpty())return Optional.empty();
        return Optional.of(new ScanTarget(new ScanTarget.Block(ScanIdentity.item(stack),0),ScanKind.OBJECT,
                Optional.of(ScanIdentity.item(stack)),ItemStackAspects.resolveForScan(groupedStack(stack)).orElseGet(AspectAmounts::new),
                Optional.of(ResearchClues.dropped(stack))));
    }

    private static ItemStack groupedStack(ItemStack original) {
        var copy = original.copyWithCount(1);
        var key = ItemStackAspects.identity(copy);
        if (key.isEmpty()) return copy;
        var identity = LegacyItemMappings.identity();
        var grouped = identity.scanKey(key.get());
        if (grouped.equals(key.get())) return copy;
        // The base mod's only groups are the three old anvils. Choose the flattened group representative.
        String replacement = identity.items().entrySet().stream().filter(entry -> entry.getValue().equals(grouped))
                .map(java.util.Map.Entry::getKey).sorted().findFirst().orElseThrow(() -> new IllegalStateException("No item for scan group " + grouped));
        var item = ForgeRegistries.ITEMS.getValue(net.minecraft.resources.ResourceLocation.parse(replacement));
        if (item == null || item == Items.AIR) throw new IllegalStateException("Missing grouped scan item " + replacement);
        var result = new ItemStack(item);
        if (copy.hasTag()) result.setTag(copy.getTag().copy());
        return result;
    }

    private static void requireServerThread(ServerLevel level) {
        if (!level.getServer().isSameThread()) throw new IllegalStateException("Scan resolution must run on the server thread");
    }
}
