package conciliummodern;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.*;

public final class ConciliumBlocks {
    public static final DeferredRegister<Block> BLOCKS=DeferredRegister.create(ForgeRegistries.BLOCKS,ThaumicConcilium.ID);
    public static final DeferredRegister<BlockEntityType<?>> TILES=DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES,ThaumicConcilium.ID);
    public static final RegistryObject<DestabilizedCrystalBlock> CRYSTAL=BLOCKS.register("destabilized_crystal",DestabilizedCrystalBlock::new);
    public static final RegistryObject<VisCondenserBlock> CONDENSER=BLOCKS.register("vis_condenser",VisCondenserBlock::new);
    public static final RegistryObject<PredictabilityBlock> HEX=BLOCKS.register("hex_of_predictability",PredictabilityBlock::new);
    public static final RegistryObject<LithographerBlock> LITHOGRAPHER=BLOCKS.register("lithographer",LithographerBlock::new);
    public static final RegistryObject<ConciliumCrucibleBlock> QUICKSILVER_CRUCIBLE=BLOCKS.register("quicksilver_crucible",()->new ConciliumCrucibleBlock(false));
    public static final RegistryObject<ConciliumCrucibleBlock> FLESH_CRUCIBLE=BLOCKS.register("flesh_crucible",()->new ConciliumCrucibleBlock(true));
    public static final RegistryObject<SolidVoidBlock> SOLID_VOID=BLOCKS.register("solid_void",SolidVoidBlock::new);
    public static final RegistryObject<RedPoweredMindBlock> RED_MIND=BLOCKS.register("red_powered_mind",RedPoweredMindBlock::new);
    public static final RegistryObject<BlockEntityType<RedPoweredMindBlockEntity>> RED_MIND_TILE=TILES.register("red_powered_mind",()->BlockEntityType.Builder.of(RedPoweredMindBlockEntity::new,RED_MIND.get()).build(null));
    public static final RegistryObject<BlockEntityType<SolidVoidBlock.Tile>> SOLID_VOID_TILE=TILES.register("solid_void",()->BlockEntityType.Builder.of(SolidVoidBlock.Tile::new,SOLID_VOID.get()).build(null));
    public static final RegistryObject<BlockEntityType<ConciliumCrucibleBlockEntity>> QUICKSILVER_CRUCIBLE_TILE=TILES.register("quicksilver_crucible",()->BlockEntityType.Builder.of(ConciliumCrucibleBlockEntity::new,QUICKSILVER_CRUCIBLE.get()).build(null));
    public static final RegistryObject<BlockEntityType<ConciliumCrucibleBlockEntity>> FLESH_CRUCIBLE_TILE=TILES.register("flesh_crucible",()->BlockEntityType.Builder.of(ConciliumCrucibleBlockEntity::new,FLESH_CRUCIBLE.get()).build(null));
    public static final RegistryObject<BlockEntityType<LithographerBlockEntity>> LITHOGRAPHER_TILE=TILES.register("lithographer",()->BlockEntityType.Builder.of(LithographerBlockEntity::new,LITHOGRAPHER.get()).build(null));
    public static final RegistryObject<BlockEntityType<PredictabilityBlockEntity>> HEX_TILE=TILES.register("hex_of_predictability",()->BlockEntityType.Builder.of(PredictabilityBlockEntity::new,HEX.get()).build(null));
    public static final RegistryObject<BlockEntityType<VisCondenserBlockEntity>> CONDENSER_TILE=TILES.register("vis_condenser",()->BlockEntityType.Builder.of(VisCondenserBlockEntity::new,CONDENSER.get()).build(null));
    public static final RegistryObject<BlockEntityType<DestabilizedCrystalBlockEntity>> CRYSTAL_TILE=TILES.register("destabilized_crystal",()->BlockEntityType.Builder.of(DestabilizedCrystalBlockEntity::new,CRYSTAL.get()).build(null));
    public static void register(net.minecraftforge.eventbus.api.IEventBus bus) {
        BLOCKS.register(bus);TILES.register(bus);
        ConciliumItems.ITEMS.register("destabilized_crystal",DestabilizedCrystalItem::new);
        ConciliumItems.ITEMS.register("vis_condenser",VisCondenserItem::new);
        // The original registered an ItemBlock for scanning/commands, but kept it out of the creative tab.
        ConciliumItems.ITEMS.register("hex_of_predictability",()->new BlockItem(HEX.get(),new Item.Properties()));
        ConciliumItems.ITEMS.register("lithographer",LithographerItem::new);
        ConciliumItems.ITEMS.register("quicksilver_crucible",()->new BlockItem(QUICKSILVER_CRUCIBLE.get(),new Item.Properties()));
        ConciliumItems.ITEMS.register("flesh_crucible",()->new BlockItem(FLESH_CRUCIBLE.get(),new Item.Properties()));
        ConciliumItems.ITEMS.register("solid_void",SolidVoidBlock.Item::new);
        ConciliumItems.ITEMS.register("red_powered_mind",RedPoweredMindBlock.Item::new);
    }
    private ConciliumBlocks() {}
}
