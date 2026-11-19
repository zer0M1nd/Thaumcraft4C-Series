package thmodern.client;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.item.Items;
import thmodern.Horizons;
/** TH supplied complete coloured armour skins, not an overlay on the modern horse UV map. */
public final class LegacySteedRenderer extends MobRenderer<Horse,LegacySteedModel> {
    public LegacySteedRenderer(EntityRendererProvider.Context context){super(context,new LegacySteedModel(),.75f);}
    @Override public ResourceLocation getTextureLocation(Horse horse){
        String name=horse.getType()==Horizons.NIGHTMARE.get()?"nightmare":"endersteed";var armour=horse.getArmor();
        String suffix=armour.is(Items.IRON_HORSE_ARMOR)?"iron":armour.is(Items.GOLDEN_HORSE_ARMOR)?"gold":armour.is(Items.DIAMOND_HORSE_ARMOR)?"diamond":"";
        return ResourceLocation.parse("thaumichorizons:textures/entity/"+name+suffix+".png");
    }
}
