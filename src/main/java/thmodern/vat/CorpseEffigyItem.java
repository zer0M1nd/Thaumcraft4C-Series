package thmodern.vat;

import net.minecraft.world.item.Item;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

public class CorpseEffigyItem extends Item {
    public CorpseEffigyItem(){super(new Properties().stacksTo(1));}
    @Override public void initializeClient(java.util.function.Consumer<IClientItemExtensions> consumer){
        consumer.accept(new IClientItemExtensions(){
            private net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer renderer;
            @Override public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer(){
                if(renderer==null)renderer=new thmodern.client.CorpseEffigyItemRenderer();return renderer;
            }
        });
    }
}
