package tc4revival.content;

import java.util.WeakHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tc4revival.config.TCConfig;

/** Server-only event integration. Charge is not vanilla absorption and is not saved between entities. */
@Mod.EventBusSubscriber(modid="thaumcraft")
public  class RunicShieldEvents {
    private static final WeakHashMap<MinecraftServer,ServerState> SERVERS=new WeakHashMap<>();
    private static  class ServerState {
        final WeakHashMap<Player,PlayerState> players=new WeakHashMap<>();
    }
    private static class PlayerState {
        final RunicShieldState shield=new RunicShieldState();
        long rechargeAfter;
        boolean dirty=true;
    }
    private RunicShieldEvents() {}
    public static void equipmentChanged(ServerPlayer player) { playerState(player).dirty=true; }
    private static PlayerState playerState(ServerPlayer player) { return server(player).players.computeIfAbsent(player,key->new PlayerState()); }
    public static RunicShieldState state(ServerPlayer player) { return playerState(player).shield; }
    public static float restore(ServerPlayer player,float amount) {
        var entry=playerState(player);entry.shield.equipment(equipment(player));entry.dirty=false;
        float restored=entry.shield.restore(amount);
        if(restored>0)sync(player,entry.shield);
        return restored;
    }
    private static long now(ServerPlayer player) { return player.getServer().overworld().getGameTime(); }
    private static ServerState server(ServerPlayer player) { return SERVERS.computeIfAbsent(player.getServer(),key->new ServerState()); }
    private static RunicShieldState.Equipment equipment(Player player) {
        var upgrades=equipment(player.getArmorSlots(),WandInventory.wornBaubles(player));
        return new RunicShieldState.Equipment((int)player.getAttributeValue(TCAttributes.RUNIC_SHIELD.get()),upgrades.charged(),upgrades.kinetic(),upgrades.healing(),upgrades.emergency());
    }
    public static RunicShieldState.Equipment equipment(Iterable<ItemStack> armor,Iterable<ItemStack> baubles) {
        int maximum=0,charged=0,kinetic=0,healing=0,emergency=0;
        for (var stack:armor) maximum+=RunicGear.charge(stack);
        for (ItemStack stack:baubles) {
            maximum+=RunicGear.charge(stack);
            if (stack.getItem() instanceof RunicUpgradeGear gear) switch(gear.runicUpgrade(stack)) {
                case CHARGED->charged++; case KINETIC->kinetic++; case HEALING->healing++; case EMERGENCY->emergency++; default->{}
            }
        }
        return new RunicShieldState.Equipment(maximum,charged,kinetic,healing,emergency);
    }
    @SubscribeEvent public static void tick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        var entry=playerState(player); var state=entry.shield; int before=state.charge(),maximum=state.maximum();
        if (entry.dirty || player.tickCount%40==0) { entry.dirty=false; state.equipment(equipment(player)); }
        long tick=now(player);
        if (tick>=entry.rechargeAfter) state.recharge(tick*50L,TCConfig.SERVER.shieldRechargeMilliseconds(),()->WandInventory.consume(player,java.util.Map.of(
                tc4revival.aspect.TCAspects.catalog().require("aer"),TCConfig.SERVER.shieldCostCentiVis(),tc4revival.aspect.TCAspects.catalog().require("terra"),TCConfig.SERVER.shieldCostCentiVis())));
        if (before!=state.charge() || maximum!=state.maximum() || player.tickCount%40==0) sync(player,state);
    }
    @SubscribeEvent public static void hurt(LivingHurtEvent event) {
        FortressMasks.hurt(event);
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        var source=event.getSource();
        if (source.is(DamageTypes.DROWN) || source.is(DamageTypes.WITHER) || source.is(DamageTypes.FELL_OUT_OF_WORLD) || source.is(DamageTypes.STARVE)) return;
        var state=state(player); var hit=state.hurt(event.getAmount(),now(player)*50L); if (!hit.shielded()) return;
        event.setAmount(hit.remainder());
        int target=source.is(DamageTypes.FALL) ? -2 : source.is(DamageTypes.FALLING_BLOCK) ? -3 : source.getEntity()==null ? -1 : source.getEntity().getId();
        tc4revival.network.TCNetwork.CHANNEL.send(net.minecraftforge.network.PacketDistributor.NEAR.with(()->new net.minecraftforge.network.PacketDistributor.TargetPoint(player.getX(),player.getY(),player.getZ(),64,player.level().dimension())),new tc4revival.network.RunicHitPacket(player.getId(),target));
        // Publish the resulting charge before callbacks: explosions can recursively damage the wearer.
        if (hit.broken()) playerState(player).rechargeAfter=now(player)+TCConfig.SERVER.shieldWaitTicks();
        if (hit.emergency()>0) playerState(player).dirty=true;
        sync(player,state);
        if (hit.kinetic()>0) player.level().explode(player,player.getX(),player.getY()+player.getBbHeight()/2,player.getZ(),1.5f+hit.kinetic()*.5f,net.minecraft.world.level.Level.ExplosionInteraction.NONE);
        if (hit.healing()>0) { player.addEffect(new MobEffectInstance(MobEffects.REGENERATION,240,hit.healing())); sound(player,TCSounds.RUNIC_SHIELD_EFFECT.get()); }
        if (hit.emergency()>0) sound(player,TCSounds.RUNIC_SHIELD_CHARGE.get());
    }
    private static void sound(ServerPlayer player,net.minecraft.sounds.SoundEvent sound) { player.level().playSound(null,player,sound,net.minecraft.sounds.SoundSource.PLAYERS,1,1); }
    private static void sync(ServerPlayer player,RunicShieldState state) {
        tc4revival.network.TCNetwork.CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(()->player),new tc4revival.network.RunicChargePacket(player.getId(),state.charge(),Math.max(0,state.maximum())));
    }
}
