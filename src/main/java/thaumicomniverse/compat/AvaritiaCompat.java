package thaumicomniverse.compat;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.DeferredRegister;
import tc4revival.aspect.TCAspects;
import tc4revival.content.CastingWandItem;
import tc4revival.research.KnowledgeSavedData;
import thaumcraft.api.wands.WandRod;

public final class AvaritiaCompat {
    private AvaritiaCompat() {}
    public static boolean loaded() {
        var mods=net.minecraftforge.fml.ModList.get();
        return mods.isLoaded("avaritia") || mods.isLoaded("endless");
    }
    public static ItemStack wand() {
        var stack=new ItemStack(tc4revival.content.TCItems.CASTING_WAND.get());
        stack.getOrCreateTag().putString("rod","thaumicomniverse:infinity");
        stack.getOrCreateTag().putString("cap","thaumicomniverse:matrix");
        stack.getOrCreateTag().putInt("tc4_metadata",9000);
        return stack;
    }
    public static void missingMappings(net.minecraftforge.registries.MissingMappingsEvent event) {
        for(var mapping:event.getMappings(net.minecraftforge.registries.ForgeRegistries.Keys.ITEMS,"thaumicfutures")) {
            var path=mapping.getKey().getPath();
            if(!java.util.Set.of("akashic_record","extremely_primordial_pearl").contains(path))continue;
            var id=net.minecraft.resources.ResourceLocation.parse("thaumicomniverse:"+path);
            if(net.minecraftforge.registries.ForgeRegistries.ITEMS.containsKey(id))
                mapping.remap(net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(id));
        }
    }
    public static void register(DeferredRegister<Item> items) {
        items.register("extremely_primordial_pearl", () -> new Item(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)) {
            @Override public boolean hasCraftingRemainingItem(ItemStack stack) { return true; }
            @Override public ItemStack getCraftingRemainingItem(ItemStack stack) { return stack.copyWithCount(1); }
        });
        items.register("akashic_record", () -> new Item(new Item.Properties().rarity(Rarity.EPIC)) {
            @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
                var stack = player.getItemInHand(hand);
                if (!thaumicomniverse.RevivalFeatures.Group.AVARITIA.enabled()) return InteractionResultHolder.pass(stack);
                if (player instanceof ServerPlayer serverPlayer) {
                    KnowledgeSavedData.get(serverPlayer.getServer()).update(player.getUUID(), level.random::nextInt, knowledge -> {
                        for (var aspect : TCAspects.catalog().all()) knowledge.changePool(aspect, (short) 999);
                        return null;
                    });
                    if (!player.getAbilities().instabuild) stack.shrink(1);
                }
                return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
            }
        });
    }
    public static void setup() {
        thaumcraft.api.wands.WandPropertyLayers.registerCapacity("thaumicomniverse:infinity",100,java.util.Set.of("thaumicomniverse:infinity"),
            (rod,base)->java.util.OptionalInt.of(9_999_999));
        WandRod.registerUpdate("thaumicomniverse:infinity", (stack, player) -> {
            if (!thaumicomniverse.RevivalFeatures.Group.AVARITIA.enabled())return;
            if (player.level().isClientSide || !(stack.getItem() instanceof CastingWandItem wand)) return;
            for (var aspect : TCAspects.catalog().primals())
                wand.addVis(stack, aspect, Math.max(0, wand.capacity(stack) - wand.vis(stack, aspect)), false, true);
        });
    }
}
