package tc4revival.debug;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.FakePlayer;
import tc4revival.content.*;
import tc4revival.research.*;
import tc4revival.aspect.TCAspects;

 class BrainDebugChecks {
    private BrainDebugChecks() {}
    static int run(CommandSourceStack source) {
        try { return verify(source); }
        catch (RuntimeException failure) {
            tc4revival.TC4Revival.LOGGER.error("Brain consumption validation failed", failure);
            source.sendFailure(Component.literal("brain FAILED: " + failure.getMessage())); return 0;
        }
    }
    private static int verify(CommandSourceStack source) {
        var level = source.getServer().overworld();
        var player = new FakePlayer(level, new GameProfile(UUID.fromString("18d8a252-bdb2-4a48-a74e-93750ca6a728"), "TC4BrainProbe"));
        var data = new KnowledgeSavedData(TCAspects.catalog()); data.ensure(player.getUUID(), bound -> 0);
        var initialRevision = data.revision(player.getUUID());
        int[] callbacks = {0};
        var eater = (ZombieBrainItem) TCItems.ZOMBIE_BRAIN.get();
        java.util.function.BiConsumer<net.minecraft.server.level.ServerPlayer, BrainConsumption.Gain> warp = (owner, gain) -> {
            require(owner == player, "Wrong food owner"); callbacks[0]++;
            data.update(owner.getUUID(), bound -> 0, knowledge -> { BrainConsumption.apply(knowledge, gain); return null; });
        };
        try {
            var registered = new ItemStack(TCItems.ZOMBIE_BRAIN.get(), 2);
            var identity = tc4revival.aspect.item.ItemStackAspects.identity(registered).orElseThrow();
            require(identity.item().equals("Thaumcraft:ItemZombieBrain") && identity.metadata() == 0, "Wrong edible brain identity");
            require(registered.getItem() != TCItems.material(MaterialType.DEFUNCT_BRAIN)
                    && !(registered.getItem() instanceof StoredAspectItem), "Edible and defunct brains conflated");
            var aspects = tc4revival.aspect.item.ItemStackAspects.resolve(registered).orElseThrow();
            require(aspects.size() == 3 && aspects.amount(TCAspects.catalog().require("cognitio")) == 4
                    && aspects.amount(TCAspects.catalog().require("exanimis")) == 2 && aspects.amount(TCAspects.catalog().require("corpus")) == 2, "Brain aspects");
            var food = registered.getFoodProperties(player);
            require(food.getNutrition() == 4 && food.getSaturationModifier() == .2f && food.isMeat() && !food.canAlwaysEat(), "Brain food properties");
            var effect = food.getEffects().get(0);
            require(food.getEffects().size() == 1 && effect.getSecond() == .8f && effect.getFirst().getEffect() == MobEffects.HUNGER
                    && effect.getFirst().getDuration() == 600 && effect.getFirst().getAmplifier() == 0, "Brain hunger probability/duration");
            require(registered.getUseDuration() == 32 && registered.getMaxStackSize() == 64, "Brain use time/stack size");
            player.setItemInHand(InteractionHand.MAIN_HAND, registered);
            player.getFoodData().setFoodLevel(20);
            require(!registered.getItem().use(level, player, InteractionHand.MAIN_HAND).getResult().consumesAction(), "Full player allowed to eat brain");
            require(callbacks[0] == 0, "Attempted use granted warp");
            for (boolean creative : new boolean[]{false, true}) {
                player.getAbilities().instabuild = creative;
                player.getFoodData().setFoodLevel(10); player.getFoodData().setSaturation(0);
                var stack = new ItemStack(TCItems.ZOMBIE_BRAIN.get(), 2);
                var result = eater.finishUsingItem(stack, level, player, warp);
                require(result.getCount() == 1 && player.getFoodData().getFoodLevel() == 14
                        && Math.abs(player.getFoodData().getSaturationLevel() - 1.6f) < .0001f, "Completed meal nutrition or debit");
                if (player.hasEffect(MobEffects.HUNGER)) require(player.getEffect(MobEffects.HUNGER).getDuration() == 600, "Native hunger application");
                player.removeAllEffects();
            }
            require(callbacks[0] == 2 && data.revision(player.getUUID()) == initialRevision + 2, "Warp callback transaction count");
            var restored = KnowledgeSavedData.load(data.save(new net.minecraft.nbt.CompoundTag()), TCAspects.catalog()).snapshot(player.getUUID()).orElseThrow();
            require(restored.totalWarp() >= 2 && restored.totalWarp() <= 6 && restored.warpCounter() == restored.totalWarp()
                    && restored.warp(PlayerKnowledge.WarpKind.PERMANENT) == 0, "Warp save/load or counter");
            source.sendSuccess(() -> Component.literal("brain PASS: real item properties/aspects, native meal completion and creative debit, isolated warp persistence; no live player data or client/network test"), false);
            return 1;
        } finally { player.getInventory().clearContent(); player.removeAllEffects(); }
    }
    private static void require(boolean value, String message) { if (!value) throw new IllegalStateException(message); }
}
