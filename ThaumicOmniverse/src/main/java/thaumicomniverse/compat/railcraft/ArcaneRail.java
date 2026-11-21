package thaumicomniverse.compat.railcraft;

import mods.railcraft.world.item.CartItem;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.*;
import thaumicomniverse.ThaumicOmniverse;
import tc4revival.content.StoredAspectItem;

public final class ArcaneRail {
    private static final DeferredRegister<Block> BLOCKS=DeferredRegister.create(ForgeRegistries.BLOCKS,"thaumicomniverse");
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES=DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES,"thaumicomniverse");
    private static final DeferredRegister<EntityType<?>> ENTITIES=DeferredRegister.create(ForgeRegistries.ENTITY_TYPES,"thaumicomniverse");
    private static final DeferredRegister<MenuType<?>> MENUS=DeferredRegister.create(ForgeRegistries.MENU_TYPES,"thaumicomniverse");
    public static final RegistryObject<EssentiaManipulatorBlock> LOADER=BLOCKS.register("essentia_loader",()->new EssentiaManipulatorBlock(true));
    public static final RegistryObject<EssentiaManipulatorBlock> UNLOADER=BLOCKS.register("essentia_unloader",()->new EssentiaManipulatorBlock(false));
    public static final RegistryObject<BlockEntityType<EssentiaManipulator>> MANIPULATOR=BLOCK_ENTITIES.register("essentia_manipulator",()->BlockEntityType.Builder.of(EssentiaManipulator::new,LOADER.get(),UNLOADER.get()).build(null));
    public static final RegistryObject<EntityType<EssentiaMinecart>> CART=ENTITIES.register("essentia_minecart",()->EntityType.Builder.<EssentiaMinecart>of(EssentiaMinecart::new,MobCategory.MISC).sized(.98f,.7f).clientTrackingRange(8).updateInterval(3).build("thaumicomniverse:essentia_minecart"));
    public static final RegistryObject<JarCartItem> CART_ITEM=ThaumicOmniverse.ITEMS.register("essentia_minecart",JarCartItem::new);
    public static final RegistryObject<MenuType<EssentiaManipulatorMenu>> MENU=MENUS.register("essentia_manipulator",()->IForgeMenuType.create(EssentiaManipulatorMenu::new));
    public static void register(IEventBus bus){
        ThaumicOmniverse.ITEMS.register("essentia_loader",()->new BlockItem(LOADER.get(),new Item.Properties()));
        ThaumicOmniverse.ITEMS.register("essentia_unloader",()->new BlockItem(UNLOADER.get(),new Item.Properties()));
        BLOCKS.register(bus);BLOCK_ENTITIES.register(bus);ENTITIES.register(bus);MENUS.register(bus);
    }
    public static class JarCartItem extends CartItem implements StoredAspectItem {
        public JarCartItem(){super(EssentiaMinecart::new,new Item.Properties().stacksTo(1));}
    }
    private ArcaneRail(){}
}
