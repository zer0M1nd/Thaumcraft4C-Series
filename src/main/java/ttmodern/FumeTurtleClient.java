package ttmodern;

public final class FumeTurtleClient {
    public static void register(net.minecraftforge.eventbus.api.IEventBus bus){bus.addListener(FumeTurtleClient::models);}
    private static void models(dan200.computercraft.api.client.turtle.RegisterTurtleModellersEvent event){event.register(FumeTurtleUpgrade.SERIALISER.get(),dan200.computercraft.api.client.turtle.TurtleUpgradeModeller.flatItem());}
    private FumeTurtleClient(){}
}
