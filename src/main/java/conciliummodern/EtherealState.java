package conciliummodern;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.*;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;

/** Authoritative phase/flesh state shared by the peeler and later pontifex/protolimb systems. */
@Mod.EventBusSubscriber(modid=ThaumicConcilium.ID)
public final class EtherealState {
    public static void restoreAllFlesh(net.minecraft.server.level.ServerPlayer player){var tag=data(player);tag.putInt("flesh",0);save(player,tag);sync(player);}
    private static final String KEY="thaumicconcilium:ethereal";
    private static final net.minecraftforge.network.simple.SimpleChannel NETWORK=net.minecraftforge.network.NetworkRegistry.newSimpleChannel(
        net.minecraft.resources.ResourceLocation.parse(KEY),()->"1","1"::equals,"1"::equals);
    private record State(boolean enabled,int flesh,int limbs,boolean robe){}
    public static void init(){NETWORK.messageBuilder(State.class,0,net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
        .encoder((state,buffer)->{buffer.writeBoolean(state.enabled);buffer.writeVarInt(state.flesh);buffer.writeVarInt(state.limbs);buffer.writeBoolean(state.robe);})
        .decoder(buffer->new State(buffer.readBoolean(),Math.max(0,buffer.readVarInt()),Math.max(0,buffer.readVarInt()),buffer.readBoolean()))
        .consumerMainThread((state,context)->{net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,()->()->EtherealClient.receive(state.enabled,state.flesh,state.limbs,state.robe));context.get().setPacketHandled(true);}).add();}
    static CompoundTag data(Player player){return player.getPersistentData().getCompound(KEY);}
    static void save(Player player,CompoundTag data){player.getPersistentData().put(KEY,data);}
    public static boolean active(Player player){return data(player).getBoolean("enabled") && !player.isSpectator();}
    public static boolean robe(Player player){return data(player).getBoolean("robe");}
    public static int flesh(Player player){return Math.max(0,data(player).getInt("flesh"));}
    public static int limbs(Player player){return Math.max(0,Math.min(6,data(player).getInt("limbs")));}
    public static void addLimb(ServerPlayer player){
        int count=Math.min(6,limbs(player)+1);var tag=data(player);tag.putInt("limbs",count);save(player,tag);sync(player);
        if(count==6 && !thaumcraft.api.ThaumcraftApiHelper.isResearchComplete(player,"thaumicconcilium:SOLIDVOID")){
            thaumcraft.api.ThaumcraftApiHelper.completeResearch(player,"thaumicconcilium:SOLIDVOID");
            player.level().playSound(null,player.blockPosition(),tc4revival.content.TCSounds.HEARTBEAT.get(),net.minecraft.sounds.SoundSource.PLAYERS,1,1);
        }
    }
    public static boolean restoreFlesh(ServerPlayer player){int amount=flesh(player);if(amount<=0)return false;var tag=data(player);tag.putInt("flesh",amount-1);save(player,tag);sync(player);return true;}
    public static void toggle(ServerPlayer player){if(robe(player))return;var tag=data(player);tag.putBoolean("enabled",!tag.getBoolean("enabled"));save(player,tag);if(!active(player))player.noPhysics=player.isSpectator();sync(player);}
    public static void toggleRobe(ServerPlayer player){
        if(!player.isAlive()||player.isSpectator()||!PontifexRobe.fullSet(player)||active(player)&&!robe(player))return;
        var tag=data(player);boolean enabled=!robe(player);tag.putBoolean("robe",enabled);tag.putBoolean("enabled",enabled);save(player,tag);
        if(enabled){CrimsonControl.chain(player,100);player.playSound(ConciliumSounds.SHACKLES.get(),.9f,1);}else player.noPhysics=player.isSpectator();sync(player);
    }
    private static void sync(ServerPlayer player){var tag=data(player);NETWORK.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(()->player),new State(tag.getBoolean("enabled"),flesh(player),tag.getInt("limbs"),robe(player)));}
    private static void wear(Player player,ItemStack item){
        if(!(player instanceof ServerPlayer server) || player.getAbilities().instabuild || !active(player) || data(player).getInt("limbs")>=6
                || item.getItem() instanceof tc4revival.content.CastingWandItem || item.getItem() instanceof DimensionalGem || item.is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM,net.minecraft.resources.ResourceLocation.parse("thaumicconcilium:ethereal_tools"))))return;
        addFlesh(server);
    }
    private static void addFlesh(ServerPlayer player){var tag=data(player);tag.putInt("flesh",Math.min(1000000,flesh(player)+1));save(player,tag);sync(player);}
    @SubscribeEvent public static void interaction(PlayerInteractEvent event){
        if(event instanceof PlayerInteractEvent.RightClickItem && event.getEntity() instanceof ServerPlayer player
                && event.getItemStack().getItem() instanceof tc4revival.content.CastingWandItem
                && player.getY()<player.level().getMinBuildHeight()-1 && limbs(player)>=6){
            var result=new ItemStack(ConciliumBlocks.SOLID_VOID.get());if(!player.getInventory().add(result))player.drop(result,false);
            player.level().playSound(null,player.blockPosition(),net.minecraft.sounds.SoundEvents.ITEM_PICKUP,net.minecraft.sounds.SoundSource.PLAYERS,.5f,1);
            event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);event.setCanceled(true);return;
        }
        if(event instanceof PlayerInteractEvent.RightClickBlock || event instanceof PlayerInteractEvent.RightClickItem || event instanceof PlayerInteractEvent.LeftClickBlock)
            if(event.getHand()==net.minecraft.world.InteractionHand.MAIN_HAND)wear(event.getEntity(),event.getItemStack());
    }
    @SubscribeEvent public static void use(LivingEntityUseItemEvent.Start event){if(event.getEntity() instanceof Player player)wear(player,event.getItem());}
    @SubscribeEvent public static void attack(AttackEntityEvent event){wear(event.getEntity(),event.getEntity().getMainHandItem());}
    @SubscribeEvent public static void heal(LivingHealEvent event){if(event.getEntity() instanceof Player player && active(player))event.setCanceled(true);}
    @SubscribeEvent(priority=EventPriority.HIGH) public static void damage(LivingAttackEvent event){
        if(!(event.getEntity() instanceof ServerPlayer player) || player.getAbilities().instabuild || !active(player))return;
        var source=event.getSource();
        if(source.is(net.minecraft.world.damagesource.DamageTypes.MAGIC) || source.is(net.minecraft.world.damagesource.DamageTypes.INDIRECT_MAGIC)
                || source.is(net.minecraft.world.damagesource.DamageTypes.FELL_OUT_OF_WORLD)
                || source.is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.DAMAGE_TYPE,net.minecraft.resources.ResourceLocation.parse("forge:is_magic"))))addFlesh(player);
        event.setCanceled(true);
    }
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent event){
        if(event.phase==TickEvent.Phase.END&&event.player instanceof ServerPlayer server&&robe(server)&&!PontifexRobe.fullSet(server)){
            var tag=data(server);tag.putBoolean("robe",false);tag.putBoolean("enabled",false);save(server,tag);server.noPhysics=server.isSpectator();sync(server);
        }
        var player=event.player;if(active(player)){player.noPhysics=true;player.fallDistance=0;if(!player.isShiftKeyDown())player.setDeltaMovement(player.getDeltaMovement().multiply(1,0,1));}
        if(event.phase==TickEvent.Phase.END && player instanceof ServerPlayer){float cap=Math.max(0,player.getMaxHealth()-flesh(player));if(player.getHealth()>cap)player.setHealth(cap);}
    }
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event){if(event.getEntity() instanceof ServerPlayer player)sync(player);}
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event){if(event.getEntity() instanceof ServerPlayer player)sync(player);}
    @SubscribeEvent public static void respawn(PlayerEvent.PlayerRespawnEvent event){if(event.getEntity() instanceof ServerPlayer player)sync(player);}
    @SubscribeEvent public static void clone(PlayerEvent.Clone event){var tag=data(event.getOriginal()).copy();if(event.isWasDeath()){
        tag.putBoolean("enabled",false);tag.putBoolean("robe",false);tag.putInt("limbs",0);tag.putInt("flesh",Math.min(tag.getInt("flesh"),Math.max(0,(int)event.getEntity().getMaxHealth()-2)));
    }save(event.getEntity(),tag);}
    private EtherealState(){}
}
