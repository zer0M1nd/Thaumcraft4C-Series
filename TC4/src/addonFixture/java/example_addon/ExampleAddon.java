package example_addon;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import thaumcraft.api.wands.FocusUpgradeType;

/** Copy into a Forge 1.20.1 addon with modid example_addon and a required thaumcraft dependency. */
@Mod("example_addon")
public  class ExampleAddon {
    public ExampleAddon(FMLJavaModLoadingContext context) {
        context.getModEventBus().addListener(this::setup);
    }
    private void setup(FMLCommonSetupEvent event) {
        event.enqueueWork(()->FocusUpgradeType.registerBehavior("example_addon:economy",new FocusUpgradeType.Behavior() {
            @Override public double costMultiplier(net.minecraft.world.item.ItemStack focus,net.minecraft.world.entity.player.Player player,
                    tc4revival.aspect.Aspect aspect,int level) { return Math.max(.1,1-.1*level); }
        }));
    }
}
