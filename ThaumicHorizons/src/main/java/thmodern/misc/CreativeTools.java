package thmodern.misc;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.RegistryObject;
import thmodern.Horizons;
public final class CreativeTools {
 public static final RegistryObject<Item> NODE_CHEAT=Horizons.ITEMS.register("node_cheat",NodeCheatItem::new);
 public static final RegistryObject<Item> SUICIDE_PILL=Horizons.ITEMS.register("suicide_pill",SuicidePillItem::new);
 public static void register(){}
}
