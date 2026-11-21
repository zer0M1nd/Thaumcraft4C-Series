package ttmodern;

import dan200.computercraft.api.turtle.*;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public final class FumeTurtleUpgrade extends AbstractTurtleUpgrade {
    private static final net.minecraftforge.registries.DeferredRegister<TurtleUpgradeSerialiser<?>> REGISTRY=net.minecraftforge.registries.DeferredRegister.create(TurtleUpgradeSerialiser.registryId(),Tinkerer.ID);
    public static final net.minecraftforge.registries.RegistryObject<TurtleUpgradeSerialiser<FumeTurtleUpgrade>> SERIALISER=REGISTRY.register("fume",()->TurtleUpgradeSerialiser.simple(FumeTurtleUpgrade::new));
    public static void register(net.minecraftforge.eventbus.api.IEventBus bus){REGISTRY.register(bus);net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,()->()->FumeTurtleClient.register(bus));}
    public FumeTurtleUpgrade(ResourceLocation id){super(id,TurtleUpgradeType.TOOL,"ttcomputer.dissipator",new ItemStack(Tinkerer.GAS_REMOVER.get()));}
    @Override public TurtleCommandResult useTool(ITurtleAccess turtle,TurtleSide side,TurtleVerb verb,Direction direction){
        if(verb!=TurtleVerb.DIG)return TurtleCommandResult.failure("The dissipator only clears gas with dig");
        if(turtle.isRemoved()||!(turtle.getLevel() instanceof net.minecraft.server.level.ServerLevel world))return TurtleCommandResult.failure("Turtle is unavailable");
        var profile=turtle.getOwningPlayer();if(profile==null)profile=new com.mojang.authlib.GameProfile(java.util.UUID.fromString("97d39738-01d3-44e3-ab69-208f28405cd7"),"[TTDissipator]");var actor=new net.minecraftforge.common.util.FakePlayer(world,profile);var pos=turtle.getPosition();actor.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+.5);GasClearing.clear(world,pos,actor);return TurtleCommandResult.success();
    }
}
