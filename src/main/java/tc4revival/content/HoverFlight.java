package tc4revival.content;

import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Server-owned flight. Entity references separate worlds and respawned players without retaining them. */
@Mod.EventBusSubscriber(modid = "thaumcraft")
public  class HoverFlight {
    private static final Map<Player, State> STATES = new WeakHashMap<>();
    private static final String FLIGHT_LEASE="TC4HoverPreviousMayfly";
    private static  class State { boolean hovering, granted, previousMayfly; }
    private HoverFlight() {}
    public static boolean equipped(Player player) { return player.getItemBySlot(EquipmentSlot.CHEST).getItem() instanceof HoverHarnessItem; }
    public static boolean girdle(Player player) { return WandInventory.wornBaubles(player).stream().anyMatch(stack -> stack.getItem() instanceof HoverGirdleItem); }
    public static boolean active(Player player) {
        if (player.level().isClientSide) {
            var stack = player.getItemBySlot(EquipmentSlot.CHEST);
            return equipped(player) && stack.hasTag() && stack.getTag().getByte("hover") == 1;
        }
        var state = STATES.get(player); return state != null && state.hovering;
    }
    public static void request(ServerPlayer player) {
        if (player == null || !player.isAlive() || player.isSpectator() || player.getAbilities().instabuild || !equipped(player)) return;
        var armor = player.getItemBySlot(EquipmentSlot.CHEST);
        var state = state(player, armor);
        if (!state.hovering && HoverFuel.fuel(armor) <= 0) return;
        state.hovering = !state.hovering;
        armor.getOrCreateTag().putByte("hover", (byte) (state.hovering ? 1 : 0));
        permissions(player, state, state.hovering);
        player.level().playSound(null, player, state.hovering ? TCSounds.HH_ON.get() : TCSounds.HH_OFF.get(), SoundSource.PLAYERS, .33f, 1);
        player.getInventory().setChanged(); player.containerMenu.broadcastChanges();
    }
    private static State state(Player player, ItemStack armor) {
        return STATES.computeIfAbsent(player, ignored -> {
            var state = new State(); state.hovering = armor.hasTag() && armor.getTag().getByte("hover") == 1; return state;
        });
    }
    private static void permissions(ServerPlayer player, State state, boolean enabled) {
        var abilities = player.getAbilities(); boolean oldFlying = abilities.flying, oldMayfly = abilities.mayfly;
        if (enabled) {
            if (!state.granted) {
                state.previousMayfly = player.getPersistentData().contains(FLIGHT_LEASE) ? player.getPersistentData().getBoolean(FLIGHT_LEASE) : abilities.mayfly;
                player.getPersistentData().putBoolean(FLIGHT_LEASE,state.previousMayfly); state.granted = true;
            }
            abilities.mayfly = true; abilities.flying = true;
        } else if (state.granted || player.getPersistentData().contains(FLIGHT_LEASE)) {
            boolean prior=state.granted ? state.previousMayfly : player.getPersistentData().getBoolean(FLIGHT_LEASE);
            abilities.mayfly = player.isSpectator() || abilities.instabuild || prior;
            if (!player.isSpectator() && !abilities.instabuild) abilities.flying = false;
            state.granted = false; player.getPersistentData().remove(FLIGHT_LEASE);
        }
        if (oldFlying != abilities.flying || oldMayfly != abilities.mayfly) player.onUpdateAbilities();
    }
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) return;
        var armor = player.getItemBySlot(EquipmentSlot.CHEST);
        // TC4's dimension restriction also applies to flight without a harness.
        if (!equipped(player) && inRestrictedFlight(player,player.getAbilities().flying)) {
            player.getAbilities().flying=false; player.onUpdateAbilities(); announceRestriction(player);
        }
        if (!equipped(player) || !player.isAlive() || player.isSpectator() || player.getAbilities().instabuild) {
            var state = STATES.remove(player); permissions(player, state == null ? new State() : state, false); return;
        }
        var state = state(player, armor);
        if (inRestrictedFlight(player,state.hovering || player.getAbilities().flying)) {
            state.hovering = false;
            player.getAbilities().flying=false;player.onUpdateAbilities();announceRestriction(player);
        }
        boolean before = state.hovering;
        if (state.hovering && HoverFuel.expend(armor, girdle(player))) player.fallDistance = 0;
        else { state.hovering = false; player.fallDistance *= .75f; }
        if (before && !state.hovering) player.level().playSound(null,player,TCSounds.HH_OFF.get(),SoundSource.PLAYERS,.33f,1);
        permissions(player, state, state.hovering);
        // Preserve the legacy one-tick saved flag when the final fuel unit is consumed.
        armor.getOrCreateTag().putByte("hover", (byte) (before ? 1 : 0));
    }
    private static boolean inRestrictedFlight(ServerPlayer player,boolean flying) {
        return flying && !player.isSpectator() && !player.getAbilities().instabuild
                && player.level().dimension().equals(TCEldritch.OUTER_LANDS) && player.tickCount%20==0;
    }
    private static void announceRestriction(ServerPlayer player) {
        player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("tc.break.fly")
                .withStyle(net.minecraft.ChatFormatting.ITALIC,net.minecraft.ChatFormatting.GRAY));
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        var state = STATES.remove(event.getEntity());
        if (state != null && event.getEntity() instanceof ServerPlayer player) permissions(player, state, false);
    }
    @SubscribeEvent public static void mining(PlayerEvent.BreakSpeed event) {
        if (!event.getEntity().onGround() && active(event.getEntity())) event.setNewSpeed(event.getOriginalSpeed() * 5);
    }
}
