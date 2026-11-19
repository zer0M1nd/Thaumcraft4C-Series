package tc4revival.content;

import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import tc4revival.TC4Revival;

public  class TCTables {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, TC4Revival.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, TC4Revival.MOD_ID);
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, TC4Revival.MOD_ID);
    public static final RegistryObject<TableBlock> TABLE = BLOCKS.register("table", TableBlock::new);
    public static final RegistryObject<BlockEntityType<ResearchTableBlockEntity>> TABLE_ENTITY = ENTITIES.register("table",
            () -> BlockEntityType.Builder.of(ResearchTableBlockEntity::new, TABLE.get()).build(null));
    public static final RegistryObject<MenuType<ResearchTableMenu>> RESEARCH_MENU = MENUS.register("research_table",
            () -> IForgeMenuType.create(ResearchTableMenu::new));
    private TCTables() {}
    public static void register(IEventBus bus) { BLOCKS.register(bus); ENTITIES.register(bus); MENUS.register(bus); }
}
