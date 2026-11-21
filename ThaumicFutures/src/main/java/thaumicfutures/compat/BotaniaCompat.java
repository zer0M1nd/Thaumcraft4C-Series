package thaumicfutures.compat;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import thaumcraft.api.IGoggles;
import vazkii.botania.api.BotaniaRegistries;
import vazkii.botania.api.brew.Brew;
import vazkii.botania.common.item.equipment.armor.manasteel.ManasteelHelmItem;
import vazkii.botania.common.item.equipment.armor.terrasteel.TerrasteelHelmItem;
import vazkii.botania.common.item.equipment.armor.elementium.ElementiumHelmItem;

public final class BotaniaCompat {
    private BotaniaCompat() {}
    public static void register(DeferredRegister<Item> items, IEventBus bus) {
        items.register("mana_inkwell", ManaInkwellItem::new);
        items.register("manasteel_revealing_helmet", Manasteel::new);
        items.register("terrasteel_revealing_helmet", Terrasteel::new);
        items.register("elementium_revealing_helmet", Elementium::new);
        DeferredRegister<Brew> brews = DeferredRegister.create(BotaniaRegistries.BREWS, "thaumicfutures");
        brews.register("warp_ward", () -> new Brew(0xFBBDFF, 25000,
                new MobEffectInstance(tc4revival.content.TCEffects.WARP_WARD.get(), 12000)).setNotBloodPendantInfusable());
        brews.register(bus);
    }
    private interface RevealingHelmet extends IGoggles {
        @Override default boolean showNodes(ItemStack stack, LivingEntity wearer) { return thaumicfutures.FutureFeatures.Group.BOTANIA.enabled(); }
        @Override default boolean showIngamePopups(ItemStack stack, LivingEntity wearer) { return thaumicfutures.FutureFeatures.Group.BOTANIA.enabled(); }
    }
    public static final class Manasteel extends ManasteelHelmItem implements RevealingHelmet {
        public Manasteel() { super(new Item.Properties()); }
    }
    public static final class Terrasteel extends TerrasteelHelmItem implements RevealingHelmet {
        public Terrasteel() { super(new Item.Properties()); }
    }
    public static final class Elementium extends ElementiumHelmItem implements RevealingHelmet {
        public Elementium() { super(new Item.Properties()); }
    }
}
