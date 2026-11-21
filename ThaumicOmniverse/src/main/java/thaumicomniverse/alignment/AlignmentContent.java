package thaumicomniverse.alignment;

import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.registries.*;
import thaumicomniverse.*;

public final class AlignmentContent {
    public static final DeferredRegister<Block> BLOCKS=DeferredRegister.create(ForgeRegistries.BLOCKS,ThaumicOmniverse.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> ENTITIES=DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES,ThaumicOmniverse.MOD_ID);
    public static final DeferredRegister<MenuType<?>> MENUS=DeferredRegister.create(ForgeRegistries.MENU_TYPES,ThaumicOmniverse.MOD_ID);
    public static final RegistryObject<AlignmentPedestalBlock> POSITIVE=block("positive_pedestal",1,false,false);
    public static final RegistryObject<AlignmentPedestalBlock> NEGATIVE=block("negative_pedestal",-1,false,false);
    public static final RegistryObject<AlignmentPedestalBlock> STACK_POSITIVE=block("stack_positive_pedestal",1,true,false);
    public static final RegistryObject<AlignmentPedestalBlock> STACK_NEGATIVE=block("stack_negative_pedestal",-1,true,false);
    public static final RegistryObject<AlignmentPedestalBlock> CORE=block("alignment_core",1,false,true);
    public static final RegistryObject<BlockEntityType<AlignmentPedestalEntity>> PEDESTAL_ENTITY=ENTITIES.register("alignment_pedestal",()->BlockEntityType.Builder.of(AlignmentPedestalEntity::new,POSITIVE.get(),NEGATIVE.get(),STACK_POSITIVE.get(),STACK_NEGATIVE.get()).build(null));
    public static final RegistryObject<BlockEntityType<AlignmentCoreEntity>> CORE_ENTITY=ENTITIES.register("alignment_core",()->BlockEntityType.Builder.of(AlignmentCoreEntity::new,CORE.get()).build(null));
    public static final RegistryObject<MenuType<AlignmentMenu>> MENU=MENUS.register("alignment",()->net.minecraftforge.common.extensions.IForgeMenuType.create(AlignmentMenu::new));
    public static final RegistryObject<Item> CRYSTAL=ThaumicOmniverse.ITEMS.register("aligned_crystal",()->new Item(new Item.Properties()));
    private static RegistryObject<AlignmentPedestalBlock> block(String id,int sign,boolean stacking,boolean core) {
        var entry=BLOCKS.register(id,()->new AlignmentPedestalBlock(sign,stacking,core));
        ThaumicOmniverse.ITEMS.register(id,()->new BlockItem(entry.get(),new Item.Properties()));return entry;
    }
    public static void register(net.minecraftforge.eventbus.api.IEventBus bus) {BLOCKS.register(bus);ENTITIES.register(bus);MENUS.register(bus);}
}
