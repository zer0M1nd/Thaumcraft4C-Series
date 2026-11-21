package gadomancymodern.item;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import gadomancymodern.compressor.DarknessElementEntity;
public class DarknessElementItem extends Item {
    public DarknessElementItem(){super(new Properties().stacksTo(1).rarity(Rarity.EPIC));}
    @Override public void inventoryTick(ItemStack stack,Level level,Entity holder,int slot,boolean held){if(!level.isClientSide&&(level.getGameTime()&15)==0)DarknessElementEntity.affect(level,holder.position());}
    @Override public boolean hasCustomEntity(ItemStack stack){return true;}
    @Override public Entity createEntity(Level level,Entity original,ItemStack stack){var item=new DarknessElementEntity(gadomancymodern.Gadomancy.DARKNESS_ENTITY.get(),level);item.setItem(stack.copy());item.setPos(original.position());item.setDeltaMovement(original.getDeltaMovement());item.setDefaultPickUpDelay();return item;}
}
