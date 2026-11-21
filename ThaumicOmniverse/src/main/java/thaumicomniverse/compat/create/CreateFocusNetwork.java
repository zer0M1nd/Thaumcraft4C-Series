package thaumicomniverse.compat.create;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.ClipContext;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;
import tc4revival.content.*;

public final class CreateFocusNetwork {
    private static final SimpleChannel CHANNEL=NetworkRegistry.newSimpleChannel(
            ResourceLocation.parse("thaumicomniverse:create_foci"),()->"3","3"::equals,"3"::equals);
    public record Action(InteractionHand hand,int kind,int value,ListTag program){}
    public record Program(InteractionHand hand,ListTag program){}
    public static final int USE=0,RELEASE=1,ATTACK=2,SCROLL=3,SAVE=4,EDIT_PROGRAM=5,SET_SPEED=6,CANCEL=7;
    public static void register(){
        CHANNEL.messageBuilder(Action.class,0,NetworkDirection.PLAY_TO_SERVER)
                .encoder((p,b)->{b.writeEnum(p.hand);b.writeVarInt(p.kind);b.writeVarInt(p.value);var tag=new CompoundTag();tag.put("Program",p.program);b.writeNbt(tag);})
                .decoder(b->{var hand=b.readEnum(InteractionHand.class);int kind=b.readVarInt(),value=b.readVarInt();var tag=b.readNbt();return new Action(hand,kind,value,tag==null?new ListTag():tag.getList("Program",Tag.TAG_COMPOUND));})
                .consumerMainThread((p,c)->handle(c.get().getSender(),p)).add();
        CHANNEL.messageBuilder(Program.class,1,NetworkDirection.PLAY_TO_CLIENT)
                .encoder((p,b)->{b.writeEnum(p.hand);var tag=new CompoundTag();tag.put("Program",p.program);b.writeNbt(tag);})
                .decoder(b->{var hand=b.readEnum(InteractionHand.class);var tag=b.readNbt();return new Program(hand,tag==null?new ListTag():tag.getList("Program",Tag.TAG_COMPOUND));})
                .consumerMainThread((p,c)->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->CreateFocusClient.program(p))).add();
    }
    public static void send(InteractionHand hand,int kind,int value){CHANNEL.sendToServer(new Action(hand,kind,value,new ListTag()));}
    public static void save(InteractionHand hand,ListTag program){CHANNEL.sendToServer(new Action(hand,SAVE,0,program));}
    public static void openProgram(ServerPlayer player,InteractionHand hand,ListTag program){
        CHANNEL.send(PacketDistributor.PLAYER.with(()->player),new Program(hand,program));
    }
    private static void handle(ServerPlayer player,Action action){
        if(player==null||!player.isAlive()||player.isSpectator()||!ArcaneContraptions.enabled())return;
        if(action.kind==CANCEL){PowerFocusSessions.release(player);RelocationSessions.release(player);player.stopUsingItem();return;}
        var wand=player.getItemInHand(action.hand);var focus=WandFocusItem.attached(wand);
        if(!(wand.getItem() instanceof CastingWandItem casting)||casting.isSceptre(wand)
                ||!(focus.getItem() instanceof KineticFocusItem||focus.getItem() instanceof RelocationFocusItem)
                ||!thaumcraft.api.wands.WandAutomation.allowed(wand,player))return;
        switch(action.kind){
            case USE->{
                // Repeat-use packets cannot re-start a program while the mouse is still held.
                if(player.getPersistentData().getLong("OmniFocusUseTick")==player.level().getGameTime())return;
                player.getPersistentData().putLong("OmniFocusUseTick",player.level().getGameTime());
                var eye=player.getEyePosition();
                var hit=player.level().clip(new ClipContext(eye,eye.add(player.getLookAngle().scale(16)),ClipContext.Block.OUTLINE,ClipContext.Fluid.NONE,player));
                ((WandFocusItem)focus.getItem()).cast(wand,player.level(),player,action.hand,hit);
            }
            case RELEASE->{PowerFocusSessions.release(player);RelocationSessions.releaseByPlayer(player);player.stopUsingItem();}
            case ATTACK->{
                if(player.level().getGameTime()-player.getPersistentData().getLong("OmniFocusAttackTick")<4)return;
                player.getPersistentData().putLong("OmniFocusAttackTick",player.level().getGameTime());
                RelocationSessions.attack(player,action.hand);
            }
            case SCROLL->{
                int sign=Integer.signum(action.value);
                if(sign==0)return;
                if(focus.getItem() instanceof RelocationFocusItem&&player.isUsingItem()){
                    if(player.isShiftKeyDown())RelocationSessions.distance(player,sign);
                    else RelocationSessions.turn(player,sign);
                }
            }
            case SAVE->{
                if(KineticFocusItem.programmed(focus)){
                    focus.getOrCreateTag().put("KineticProgram",PowerFocusSessions.sanitize(action.program,KineticFocusItem.programSize(focus)));
                    WandFocusItem.attach(wand,focus);
                }
            }
            case EDIT_PROGRAM->{
                if(KineticFocusItem.programmed(focus))openProgram(player,action.hand,PowerFocusSessions.program(focus));
            }
            case SET_SPEED->{
                if(focus.getItem() instanceof KineticFocusItem){
                    int speed=net.minecraft.util.Mth.clamp(action.value,-256,256);
                    focus.getOrCreateTag().putInt("KineticRPM",speed==0?1:speed);
                    WandFocusItem.attach(wand,focus);
                }
            }
            default->{}
        }
        player.getInventory().setChanged();player.containerMenu.broadcastChanges();
    }
    private CreateFocusNetwork(){}
}
