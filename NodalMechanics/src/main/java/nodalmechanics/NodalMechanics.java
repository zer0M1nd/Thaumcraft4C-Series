package nodalmechanics;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import thaumcraft.api.crafting.ArcaneResultTransforms;
import tc4revival.aspect.TCAspects;
import tc4revival.content.TCItems;

@Mod(NodalMechanics.ID)
public class NodalMechanics {
    public static final String ID = "nodalmechanics";
    public static final String RESEARCH = "NODECATALYZATION";
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, ID);
    public static final RegistryObject<MatrixItem> MATRIX = ITEMS.register("matrix", MatrixItem::new);
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, ID);
    public static final RegistryObject<NodeInfusionRecipe.Serializer> NODE_RECIPE = SERIALIZERS.register("node", NodeInfusionRecipe.Serializer::new);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(net.minecraft.core.registries.Registries.CREATIVE_MODE_TAB, ID);
    public static final RegistryObject<CreativeModeTab> TAB = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.NodalMechanics"))
            .icon(() -> new ItemStack(MATRIX.get())).displayItems((parameters, output) -> {
                output.accept(MATRIX.get());
                for (var aspect : TCAspects.catalog().primals()) output.accept(MatrixItem.attuned(java.util.Collections.nCopies(8, aspect.tag())));
            }).build());

    public NodalMechanics() {
        RevivalFeatures.register();
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ITEMS.register(bus); SERIALIZERS.register(bus); TABS.register(bus);
        ArcaneResultTransforms.register(id("attune"), (inputs, width, height, registries, base, config) -> {
            int matrices = 0, phials = 0;
            var tags = new java.util.ArrayList<String>();
            for (var stack : inputs) {
                if (stack.isEmpty()) continue;
                if (stack.is(MATRIX.get())) {
                    // Original 1.0-7 only tunes an untagged matrix once.
                    if (++matrices != 1 || stack.hasTag()) return ItemStack.EMPTY;
                } else if (stack.is(TCItems.ESSENTIA_PHIAL.get())) {
                    phials++;
                    var stored = TCItems.ESSENTIA_PHIAL.get().storedAspects(stack);
                    if (stored.isEmpty()) return ItemStack.EMPTY;
                    for (var aspect : stored.aspects()) tags.add(aspect.tag());
                } else return ItemStack.EMPTY;
            }
            if (matrices != 1 || phials < 1 || phials > 8 || tags.size() > 8) return ItemStack.EMPTY;
            return MatrixItem.attuned(tags);
        });
        thaumcraft.api.ThaumcraftApi.addWarpToItem(id("matrix"), 1);
    }
    public static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath(ID, path); }
}
