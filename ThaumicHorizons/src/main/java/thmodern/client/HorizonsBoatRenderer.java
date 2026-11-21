package thmodern.client;
import com.mojang.datafixers.util.Pair;
import net.minecraft.client.model.BoatModel;
import net.minecraft.client.model.ListModel;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.vehicle.Boat;
import thmodern.boat.HorizonsBoat;
/** Native paddles, hurt wobble, bubble-column animation and water occlusion. */
public final class HorizonsBoatRenderer extends BoatRenderer {
    private final Pair<ResourceLocation,ListModel<Boat>> greatwood,thaumium;
    public HorizonsBoatRenderer(EntityRendererProvider.Context context){
        super(context,false);
        greatwood=Pair.of(ResourceLocation.parse("thaumichorizons:textures/entity/boat/greatwood.png"),new BoatModel(HorizonsBoatModel.createBodyLayer().bakeRoot()));
        thaumium=Pair.of(ResourceLocation.parse("thaumichorizons:textures/entity/boat/thaumium.png"),new BoatModel(HorizonsBoatModel.createBodyLayer().bakeRoot()));
    }
    @Override public Pair<ResourceLocation,ListModel<Boat>> getModelWithLocation(Boat boat){return boat instanceof HorizonsBoat horizons&&horizons.metal()?thaumium:greatwood;}
}
