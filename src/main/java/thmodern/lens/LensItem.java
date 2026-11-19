package thmodern.lens;
import net.minecraft.world.item.Item;
public final class LensItem extends Item {
    public final String key;
    public LensItem(String key){super(new Properties().stacksTo(1));this.key=key;}
}
