package tc4revival.content;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import tc4revival.TC4Revival;

/** Outer Lands registrations, kept together with the labyrinth implementation. */
public  class TCEldritch {
    public static final net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> OUTER_LANDS=
            net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,net.minecraft.resources.ResourceLocation.parse("thaumcraft:outer_lands"));
    private static final DeferredRegister<Block> BLOCKS=DeferredRegister.create(ForgeRegistries.BLOCKS,TC4Revival.MOD_ID);
    private static final DeferredRegister<Item> ITEMS=DeferredRegister.create(ForgeRegistries.ITEMS,TC4Revival.MOD_ID);
    private static final DeferredRegister<net.minecraft.world.level.block.entity.BlockEntityType<?>> BLOCK_ENTITIES=DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES,TC4Revival.MOD_ID);
    public static final RegistryObject<Item> ELDRITCH_EYE=ITEMS.register("eldritch_eye",()->new EldritchObjectItem(false));
    public static final RegistryObject<Item> RUNED_TABLET=ITEMS.register("runed_tablet",()->new EldritchObjectItem(true));
    public static final RegistryObject<Item> OBELISK_PLACER=ITEMS.register("obelisk_placer",ObeliskPlacerItem::new);
    public static final RegistryObject<Item> CRIMSON_RITES=ITEMS.register("crimson_rites",CrimsonRitesItem::new);
    public static final RegistryObject<Item> CRIMSON_SWORD=ITEMS.register("crimson_sword",CrimsonSwordItem::new);
    private static final java.util.EnumMap<CultistEquipment.Armor,RegistryObject<CultistArmorItem>> ARMOR=new java.util.EnumMap<>(CultistEquipment.Armor.class);
    static { for (var armor:CultistEquipment.Armor.values()) ARMOR.put(armor,ITEMS.register(armor.path,()->new CultistArmorItem(armor))); }
    public static CultistArmorItem armor(CultistEquipment.Armor armor) { return ARMOR.get(armor).get(); }
    /** Forms which shared the original blockEldritch identity, excluding cosmetic stone. */
    public static boolean isDevice(Block block) {
        var key=ForgeRegistries.BLOCKS.getKey(block);
        return key!=null && key.getNamespace().equals(TC4Revival.MOD_ID) && java.util.Set.of("eldritch_altar","eldritch_obelisk",
                "eldritch_obelisk_segment","eldritch_cap","eldritch_glowing_stone","eldritch_glyphed_stone","eldritch_carved_stone","eldritch_door_frame",
                "eldritch_lock","eldritch_crab_spawner","eldritch_rune_trap").contains(key.getPath());
    }
    public static final RegistryObject<EldritchMonumentBlock> ALTAR=block("eldritch_altar",()->new EldritchMonumentBlock(EldritchMonumentBlock.Kind.ALTAR));
    public static final RegistryObject<EldritchMonumentBlock> OBELISK=block("eldritch_obelisk",()->new EldritchMonumentBlock(EldritchMonumentBlock.Kind.OBELISK));
    public static final RegistryObject<EldritchMonumentBlock> OBELISK_SEGMENT=block("eldritch_obelisk_segment",()->new EldritchMonumentBlock(EldritchMonumentBlock.Kind.SEGMENT));
    public static final RegistryObject<EldritchMonumentBlock> CAP=block("eldritch_cap",()->new EldritchMonumentBlock(EldritchMonumentBlock.Kind.CAP));
    public static final RegistryObject<net.minecraft.world.level.block.entity.BlockEntityType<EldritchMonumentBlockEntity>> MONUMENT_ENTITY=
            BLOCK_ENTITIES.register("eldritch_monument",()->net.minecraft.world.level.block.entity.BlockEntityType.Builder.of(EldritchMonumentBlockEntity::new,ALTAR.get(),OBELISK.get(),CAP.get()).build(null));
    public static final RegistryObject<EldritchDoorBarrierBlock> DOOR_BARRIER=BLOCKS.register("eldritch_door_barrier",EldritchDoorBarrierBlock::new);
    public static final RegistryObject<EldritchPortalBlock> PORTAL=BLOCKS.register("eldritch_portal",EldritchPortalBlock::new);
    public static final RegistryObject<net.minecraft.world.level.block.entity.BlockEntityType<EldritchPortalBlockEntity>> PORTAL_ENTITY=
            BLOCK_ENTITIES.register("eldritch_portal",()->net.minecraft.world.level.block.entity.BlockEntityType.Builder.of(EldritchPortalBlockEntity::new,PORTAL.get()).build(null));
    public static final RegistryObject<EldritchLockBlock> LOCK=block("eldritch_lock",EldritchLockBlock::new);
    public static final RegistryObject<net.minecraft.world.level.block.entity.BlockEntityType<EldritchLockBlockEntity>> LOCK_ENTITY=
            BLOCK_ENTITIES.register("eldritch_lock",()->net.minecraft.world.level.block.entity.BlockEntityType.Builder.of(EldritchLockBlockEntity::new,LOCK.get()).build(null));
    public static final RegistryObject<EldritchCrystalBlock> CRYSTAL=block("eldritch_crystal",EldritchCrystalBlock::new);
    public static final RegistryObject<net.minecraft.world.level.block.entity.BlockEntityType<EldritchCrystalBlockEntity>> CRYSTAL_ENTITY=
            BLOCK_ENTITIES.register("eldritch_crystal",()->net.minecraft.world.level.block.entity.BlockEntityType.Builder.of(EldritchCrystalBlockEntity::new,CRYSTAL.get()).build(null));
    public static final RegistryObject<Block> OBSIDIAN_TILE=block("obsidian_tile",()->new Block(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().strength(30,999).sound(net.minecraft.world.level.block.SoundType.STONE)));
    public static final RegistryObject<Block> STONE=block("eldritch_stone",()->new Block(stone()));
    public static final RegistryObject<EldritchDecorBlock> GLOWING_STONE=block("eldritch_glowing_stone",()->new EldritchDecorBlock(EldritchDecorBlock.Kind.GLOWING));
    public static final RegistryObject<EldritchDecorBlock> GLYPHED_STONE=block("eldritch_glyphed_stone",()->new EldritchDecorBlock(EldritchDecorBlock.Kind.GLYPHED));
    public static final RegistryObject<EldritchDecorBlock> CARVED_STONE=block("eldritch_carved_stone",()->new EldritchDecorBlock(EldritchDecorBlock.Kind.CARVED));
    public static final RegistryObject<Block> DOOR_FRAME=block("eldritch_door_frame",()->new Block(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()
            .strength(-1,Float.MAX_VALUE).noOcclusion().sound(net.minecraft.world.level.block.SoundType.STONE).lightLevel(state->12).isValidSpawn((state,world,pos,type)->false)));
    public static final RegistryObject<Block> ROCK=block("eldritch_rock",()->new Block(stone()));
    public static final RegistryObject<Block> NONSPAWNING_STONE=block("eldritch_stone_nonspawning",()->new Block(stone().isValidSpawn((state,world,pos,type)->false)));
    public static final RegistryObject<Block> CRUST=block("eldritch_crust",()->new Block(stone().lightLevel(state->4)));
    public static final RegistryObject<Block> PEDESTAL_STONE=block("eldritch_pedestal_stone",()->new Block(stone()));
    public static final RegistryObject<StairBlock> STAIRS=block("eldritch_stairs",()->new StairBlock(()->STONE.get().defaultBlockState(),stone()));
    public static final RegistryObject<SlabBlock> SLAB=block("eldritch_slab",()->new SlabBlock(stone()));
    public static final RegistryObject<EldritchVoidBlock> VOID=BLOCKS.register("eldritch_void",EldritchVoidBlock::new);
    public static final RegistryObject<net.minecraft.world.level.block.entity.BlockEntityType<EldritchVoidBlockEntity>> VOID_ENTITY=
            BLOCK_ENTITIES.register("eldritch_void",()->net.minecraft.world.level.block.entity.BlockEntityType.Builder.of(EldritchVoidBlockEntity::new,VOID.get()).build(null));
    public static final RegistryObject<EldritchFieldBlock> FIELD=BLOCKS.register("eldritch_field",EldritchFieldBlock::new);
    public static final RegistryObject<EldritchTrapBlock> TRAP=block("eldritch_rune_trap",EldritchTrapBlock::new);
    public static final RegistryObject<EldritchCrabSpawnerBlock> CRAB_SPAWNER=block("eldritch_crab_spawner",EldritchCrabSpawnerBlock::new);
    public static final RegistryObject<net.minecraft.world.level.block.entity.BlockEntityType<EldritchCrabSpawnerBlockEntity>> CRAB_SPAWNER_ENTITY=
            BLOCK_ENTITIES.register("eldritch_crab_spawner",()->net.minecraft.world.level.block.entity.BlockEntityType.Builder.of(EldritchCrabSpawnerBlockEntity::new,CRAB_SPAWNER.get()).build(null));
    public static final RegistryObject<net.minecraft.world.level.block.entity.BlockEntityType<EldritchTrapBlockEntity>> TRAP_ENTITY=
            BLOCK_ENTITIES.register("eldritch_rune_trap",()->net.minecraft.world.level.block.entity.BlockEntityType.Builder.of(EldritchTrapBlockEntity::new,TRAP.get()).build(null));
    private static final java.util.List<RegistryObject<EldritchLootBlock>> LOOT=new java.util.ArrayList<>();
    static {
        for (boolean crate:new boolean[]{false,true}) for (int rarity=0;rarity<3;rarity++) {
            final int tier=rarity; String name=(crate ? "loot_crate_" : "loot_urn_")+rarity;
            var block=BLOCKS.register(name,()->new EldritchLootBlock(crate,tier)); LOOT.add(block);
            ITEMS.register(name,()->{
                var properties=new Item.Properties().rarity(tier==0 ? net.minecraft.world.item.Rarity.COMMON
                        : tier==1 ? net.minecraft.world.item.Rarity.UNCOMMON : net.minecraft.world.item.Rarity.RARE);
                return crate ? new LegacyWoodBlockItem(block.get(),properties) : new BlockItem(block.get(),properties);
            });
        }
    }
    public static EldritchLootBlock loot(boolean crate,int rarity) {
        if (rarity<0 || rarity>2) throw new IllegalArgumentException("Invalid loot rarity");
        return LOOT.get((crate ? 3 : 0)+rarity).get();
    }

    private static BlockBehaviour.Properties stone() {
        return BlockBehaviour.Properties.of().strength(2,10).sound(net.minecraft.world.level.block.SoundType.STONE);
    }
    private static <T extends Block> RegistryObject<T> block(String name,java.util.function.Supplier<T> factory) {
        RegistryObject<T> block=BLOCKS.register(name,factory);
        ITEMS.register(name,()->block.get() instanceof EldritchMonumentBlock || block.get() instanceof EldritchCrystalBlock || block.get() instanceof EldritchCrabSpawnerBlock
                ? new EldritchRenderedBlockItem(block.get()) : new BlockItem(block.get(),new Item.Properties())); return block;
    }
    public static void register(IEventBus bus) { BLOCKS.register(bus); ITEMS.register(bus); BLOCK_ENTITIES.register(bus); }
    private TCEldritch() {}
}
