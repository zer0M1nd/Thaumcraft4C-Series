package tc4revival.content;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.*;

public  class TCCleansing {
    private static final DeferredRegister<Item> ITEMS=DeferredRegister.create(ForgeRegistries.ITEMS,"thaumcraft");
    private static final DeferredRegister<Block> BLOCKS=DeferredRegister.create(ForgeRegistries.BLOCKS,"thaumcraft");
    public static final RegistryObject<PureWaterBlock> PURE_WATER=BLOCKS.register("purifying_water",PureWaterBlock::new);
    public static final RegistryObject<Item> PURE_WATER_ITEM=ITEMS.register("purifying_water",()->new BlockItem(PURE_WATER.get(),new Item.Properties()));
    public static final RegistryObject<Item> BATH_SALTS=ITEMS.register("bath_salts",BathSaltsItem::new);
    public static final RegistryObject<Item> SANITY_SOAP=ITEMS.register("sanity_soap",SanitySoapItem::new);
    public static final RegistryObject<Block> TALLOW_BLOCK=BLOCKS.register("tallow_block",()->new Block(net.minecraft.world.level.block.state.BlockBehaviour.Properties.copy(Blocks.STONE).strength(2,10)));
    public static final RegistryObject<Item> TALLOW_BLOCK_ITEM=ITEMS.register("tallow_block",()->new BlockItem(TALLOW_BLOCK.get(),new Item.Properties()));
    private TCCleansing() {}
    public static void register(IEventBus bus) { BLOCKS.register(bus); ITEMS.register(bus); }
}
