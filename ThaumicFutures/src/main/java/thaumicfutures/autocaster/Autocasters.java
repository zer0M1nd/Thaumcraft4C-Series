package thaumicfutures.autocaster;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.*;
import thaumicfutures.LegacyContent;

/** TC5's owned, surface-mounted focus turret, supplied by an actual wand. */
public final class Autocasters {
    private static final DeferredRegister<EntityType<?>> ENTITIES=DeferredRegister.create(ForgeRegistries.ENTITY_TYPES,"thaumicfutures");
    private static final DeferredRegister<MenuType<?>> MENUS=DeferredRegister.create(ForgeRegistries.MENU_TYPES,"thaumicfutures");
    public static final RegistryObject<EntityType<AutocasterEntity>> ENTITY=ENTITIES.register("autocaster",()->
            EntityType.Builder.<AutocasterEntity>of(AutocasterEntity::new,MobCategory.MISC).sized(.9f,.9f)
                    .clientTrackingRange(10).updateInterval(1).build("thaumicfutures:autocaster"));
    public static final RegistryObject<AutocasterItem> ITEM=LegacyContent.ITEMS.register("autocaster",AutocasterItem::new);
    public static final RegistryObject<MenuType<AutocasterMenu>> MENU=MENUS.register("autocaster",()->IForgeMenuType.create(AutocasterMenu::new));
    public static void register(IEventBus bus) {
        ENTITIES.register(bus);MENUS.register(bus);
        bus.addListener((EntityAttributeCreationEvent event)->event.put(ENTITY.get(),AutocasterEntity.attributes().build()));
    }
    private Autocasters() {}
}
