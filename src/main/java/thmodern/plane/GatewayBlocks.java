package thmodern.plane;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.RegistryObject;
import thmodern.Horizons;
public final class GatewayBlocks {
 private GatewayBlocks(){}
 public static final RegistryObject<Block> SLOT=Horizons.BLOCKS.register("keystone_slot",KeystoneSlotBlock::new);
 public static final RegistryObject<Block> PORTAL=Horizons.BLOCKS.register("gateway_portal",GatewayPortalBlock::new);
 public static final RegistryObject<BlockEntityType<KeystoneSlotEntity>> SLOT_ENTITY=Horizons.ENTITIES.register("keystone_slot",()->BlockEntityType.Builder.of(KeystoneSlotEntity::new,SLOT.get()).build(null));
 public static final RegistryObject<BlockEntityType<GatewayPortalEntity>> PORTAL_ENTITY=Horizons.ENTITIES.register("gateway_portal",()->BlockEntityType.Builder.of(GatewayPortalEntity::new,PORTAL.get()).build(null));
 static{Horizons.ITEMS.register("keystone_slot",()->new BlockItem(SLOT.get(),new Item.Properties()));}
 public static void register(){}
}
