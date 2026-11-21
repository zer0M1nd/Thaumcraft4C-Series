package conciliummodern;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

public class Protobody extends Item {
    public Protobody(){super(new Properties().stacksTo(1).rarity(Rarity.RARE));}
    @Override public boolean hasCustomEntity(ItemStack stack){return true;}
    @Override public Entity createEntity(Level level,Entity original,ItemStack stack){
        var result=new ProtobodyDrop(ConciliumEntities.PROTOBODY_DROP.get(),level);
        result.load(original.saveWithoutId(new net.minecraft.nbt.CompoundTag()));result.setItem(stack.copy());result.setPickUpDelay(40);return result;
    }
    @Override public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer){
        consumer.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions(){
            @Override public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer(){return VoidBodyRenderer.ITEM;}
        });
    }
}
