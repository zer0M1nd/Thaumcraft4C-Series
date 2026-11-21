package thaumicomniverse.compat.create;

import java.util.*;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.api.stress.BlockStressValues;
import com.simibubi.create.content.kinetics.base.*;
import com.simibubi.create.content.kinetics.gearbox.GearboxBlockEntity;
import com.simibubi.create.content.kinetics.transmission.SplitShaftBlockEntity;
import com.simibubi.create.content.kinetics.transmission.sequencer.*;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.*;
import net.minecraftforge.event.TickEvent;
import tc4revival.content.*;

/**
 * Transient generators live on the receiving kinetic block, without inserting a block into
 * the adjacent space. Create still owns rotation propagation, stress and machine behavior.
 */
public final class PowerFocusSessions {
    private static final Map<UUID, Session> PLAYERS = new HashMap<>();
    private static final Map<KineticBlockEntity, Session> INPUTS = new IdentityHashMap<>();
    private static boolean originalQuery;
    public static final class Session {
        final Player player;
        final InteractionHand hand;
        final ItemStack wand;
        final KineticBlockEntity tile;
        final Direction face;
        final boolean scripted;
        final ListTag instructions;
        final float capacity, nativeSpeed;
        final int direction;
        int index, timer, duration;
        float rpm, output;
        boolean initialized,engaged;
        Session(Player player, InteractionHand hand, ItemStack wand, KineticBlockEntity tile, Direction face) {
            this.player=player; this.hand=hand; this.wand=wand; this.tile=tile; this.face=face;
            var focus=WandFocusItem.attached(wand);
            scripted=KineticFocusItem.programmed(focus); instructions=program(focus);
            rpm=KineticFocusItem.rpm(focus);
            direction=player.isShiftKeyDown()?-1:1;
            capacity=(float)(BlockStressValues.getImpact(AllBlocks.MECHANICAL_PRESS.get()) * 256
                    * (1 + .2 * ((CastingWandItem)wand.getItem()).focusPotency(wand,player)));
            originalQuery=true;
            try { nativeSpeed=tile.getGeneratedSpeed(); } finally { originalQuery=false; }
        }
        CompoundTag step() { return index<instructions.size()?instructions.getCompound(index):new CompoundTag(); }
        boolean waiting() { return scripted && step().getString("Type").equals("AWAIT"); }
    }
    public static Session input(KineticBlockEntity tile) { return originalQuery?null:INPUTS.get(tile); }
    public static float generated(KineticBlockEntity tile, float vanilla) {
        var s=input(tile);
        if(s==null&&tile.getLevel()!=null&&tile.getLevel().isClientSide) {
            float shown=((KineticFocusView)tile).omniverse$driveSpeed();
            return Math.abs(shown)>Math.abs(vanilla)?shown:vanilla;
        }
        return s==null || s.output==0 || Math.abs(vanilla)>Math.abs(s.output)?vanilla:s.output;
    }
    public static float capacity(KineticBlockEntity tile, float vanilla) {
        var s=input(tile);
        return s==null?vanilla:s.output==0?Math.abs(s.nativeSpeed*tile.calculateAddedStressCapacity())
                :s.capacity+Math.abs(s.nativeSpeed*tile.calculateAddedStressCapacity());
    }
    public static Direction inputFace(KineticBlockEntity tile) {
        var s=input(tile);
        if(s==null&&tile.getLevel()!=null&&tile.getLevel().isClientSide&&!tile.hasSource())
            return ((KineticFocusView)tile).omniverse$driveFace();
        return s!=null&&!tile.hasSource()?s.face:null;
    }
    public static void write(KineticBlockEntity tile,CompoundTag tag,boolean clientPacket) {
        var s=input(tile);
        if(clientPacket) {
            tag.putFloat("OmniDriveSpeed",s==null?0:s.output);
            tag.putInt("OmniDriveFace",s==null?-1:s.face.get3DDataValue());
            return;
        }
        // A transient wand cannot leave cached unloaded stress capacity in a saved network.
        if(tile.hasNetwork()&&tag.contains("Network")){
            float transientCapacity=0;
            for(var source:tile.getOrCreateNetwork().sources.keySet()){
                var contribution=input(source);
                if(contribution!=null&&contribution.output!=0)transientCapacity+=contribution.capacity;
            }
            var network=tag.getCompound("Network");
            network.putFloat("Capacity",Math.max(0,network.getFloat("Capacity")-transientCapacity));
        }
        if(s!=null&&s.engaged){
            tag.putFloat("Speed",0);tag.remove("Source");tag.remove("Network");tag.remove("Sequence");
            tag.putBoolean("NeedsSpeedUpdate",true);
        }
    }
    public static Float axisModifier(KineticBlockEntity tile, Direction direction) {
        var s=input(tile);
        if(s==null || tile.hasSource())return null;
        if(tile instanceof GearboxBlockEntity)
            return direction.getAxis()==s.face.getAxis()?(direction==s.face?1f:-1f)
                    :(direction.getAxisDirection()==s.face.getAxisDirection()?-1f:1f);
        if(tile instanceof SplitShaftBlockEntity split) {
            // Native split shafts distinguish their input and output faces via source.
            tile.source=tile.getBlockPos().relative(s.face);
            try { return split.getRotationSpeedModifier(direction); } finally { tile.source=null; }
        }
        return null;
    }
    public static ListTag program(ItemStack focus) {
        return focus.hasTag()&&focus.getTag().contains("KineticProgram",Tag.TAG_LIST)
                ?sanitize(focus.getTag().getList("KineticProgram",Tag.TAG_COMPOUND),KineticFocusItem.programSize(focus))
                :Instruction.serializeAll(Instruction.createDefault());
    }
    public static ListTag sanitize(ListTag input) {
        return sanitize(input,5);
    }
    public static ListTag sanitize(ListTag input,int limit) {
        var out=new ListTag();
        for(int i=0;i<Math.min(Math.max(1,Math.min(11,limit)),input.size());i++) {
            var tag=input.getCompound(i); var clean=new CompoundTag();
            String type=tag.getString("Type"), modifier=tag.getString("Modifier");
            if(!Set.of("TURN_ANGLE","TURN_DISTANCE","DELAY","AWAIT","END").contains(type))type="END";
            if(!Set.of("FORWARD","FORWARD_FAST","BACK","BACK_FAST").contains(modifier))modifier="FORWARD";
            int max=switch(type){case "TURN_ANGLE"->360;case "TURN_DISTANCE"->128;case "DELAY"->600;default->1;};
            clean.putString("Type",type);clean.putString("Modifier",modifier);
            clean.putInt("Value",net.minecraft.util.Mth.clamp(tag.getInt("Value"),1,max));out.add(clean);
            if(type.equals("END"))break;
        }
        if(out.isEmpty())return Instruction.serializeAll(Instruction.createDefault());
        return out;
    }
    public static boolean begin(Player player, InteractionHand hand, ItemStack wand, BlockHitResult hit) {
        var old=PLAYERS.get(player.getUUID());
        if(old!=null && old.scripted && old.wand==wand) {
            if(old.waiting() && hit.getType()==HitResult.Type.BLOCK && hit.getBlockPos().equals(old.tile.getBlockPos())) {
                old.index++;old.initialized=false;
                return true;
            }
            stop(old);
        }
        if(hit.getType()!=HitResult.Type.BLOCK || player.distanceToSqr(hit.getLocation())>256
                || !player.level().mayInteract(player,hit.getBlockPos())
                || !(player.level().getBlockEntity(hit.getBlockPos()) instanceof KineticBlockEntity tile)
                || !(tile.getBlockState().getBlock() instanceof IRotate rotate)
                || !rotate.hasShaftTowards(player.level(),hit.getBlockPos(),tile.getBlockState(),hit.getDirection())) return false;
        if(INPUTS.containsKey(tile))return false;
        stop(PLAYERS.get(player.getUUID()));
        var s=new Session(player,hand,wand,tile,hit.getDirection());
        if(!s.scripted&&!pay(s,false))return false;
        PLAYERS.put(player.getUUID(),s);INPUTS.put(tile,s);
        if(!s.scripted)hold(player,wand);
        return true;
    }
    private static boolean pay(Session s,boolean commit) {
        return pay(s,commit,1);
    }
    private static boolean pay(Session s,boolean commit,int multiplier) {
        var focus=WandFocusItem.attached(s.wand);
        if(!(focus.getItem() instanceof KineticFocusItem item))return false;
        var casting=(CastingWandItem)s.wand.getItem();
        if(multiplier==1)return casting.consumeForPlayer(s.wand,s.player,item.costs(focus),false,false,commit);
        if(!thaumcraft.api.wands.WandAutomation.allowed(s.wand,s.player))return false;
        try(var identity=thaumcraft.api.wands.WandAutomation.open(s.wand,s.player)){
            // Multiply the paid centivis cost after TC4's integer discount rounding.
            // Multiplying the raw cost first made thaumium caps charge 3x instead of 2x.
            var costs=new HashMap<>(item.costs(focus));
            costs.replaceAll((aspect,amount)->Math.multiplyExact((int)(amount*casting.consumptionModifier(s.wand,s.player,aspect,false)),multiplier));
            return casting.consume(s.wand,costs,aspect->1,false,commit,s.player.level());
        }
    }
    public static boolean hold(Player player,ItemStack wand) {
        var s=PLAYERS.get(player.getUUID());
        if(s==null||s.wand!=wand)return false;
        if(s.scripted)return true;
        var hit=player.level().clip(new net.minecraft.world.level.ClipContext(player.getEyePosition(),
                player.getEyePosition().add(player.getLookAngle().scale(16)),
                net.minecraft.world.level.ClipContext.Block.OUTLINE,net.minecraft.world.level.ClipContext.Fluid.NONE,player));
        if(hit.getType()!=HitResult.Type.BLOCK||!hit.getBlockPos().equals(s.tile.getBlockPos())
                ||hit.getDirection()!=s.face||!pay(s,true)){stop(s);return false;}
        s.rpm=KineticFocusItem.rpm(WandFocusItem.attached(wand));
        apply(s,KineticBlockEntity.convertToDirection(s.rpm*(player.isShiftKeyDown()?-1:1),s.face.getOpposite()),null);
        return true;
    }
    private static void apply(Session s,float speed,SequencedGearshiftBlockEntity.SequenceContext sequence) {
        if(s.output==speed && Objects.equals(s.tile.sequenceContext,sequence))return;
        var tile=s.tile;
        s.engaged=true;
        float before=tile.getTheoreticalSpeed();
        tile.detachKinetics();
        if(tile.hasNetwork())tile.getOrCreateNetwork().sources.remove(tile);
        tile.removeSource();
        s.output=speed;tile.sequenceContext=sequence;
        if(speed!=0) {
            tile.setSpeed(speed);
            tile.setNetwork(tile.getBlockPos().asLong());
            tile.getOrCreateNetwork().updateCapacityFor(tile,0);
        }
        tile.attachKinetics();
        if(tile.hasNetwork())tile.getOrCreateNetwork().updateNetwork();
        tile.onSpeedChanged(before);tile.sendData();
    }
    public static void release(Player player) {
        var s=PLAYERS.get(player.getUUID());if(s!=null&&!s.scripted)stop(s);
    }
    public static void stop(Session s) {
        if(s==null)return;
        if(!s.engaged){INPUTS.remove(s.tile);PLAYERS.remove(s.player.getUUID(),s);return;}
        apply(s,0,null);
        if(s.tile.hasNetwork()) {
            s.tile.getOrCreateNetwork().sources.remove(s.tile);
            s.tile.getOrCreateNetwork().updateCapacity();
        }
        INPUTS.remove(s.tile);PLAYERS.remove(s.player.getUUID(),s);
        if(!s.tile.isRemoved()&&s.tile.getLevel().hasChunkAt(s.tile.getBlockPos())) {
            if(s.tile instanceof GeneratingKineticBlockEntity generator)generator.updateGeneratedRotation();
            else s.tile.attachKinetics();
        }
    }
    public static void tick(TickEvent.ServerTickEvent event) {
        if(event.phase!=TickEvent.Phase.END)return;
        for(var s:List.copyOf(PLAYERS.values())) {
            if(!ArcaneContraptions.enabled()||!s.player.isAlive()||s.player.isSpectator()
                    ||s.player.getItemInHand(s.hand)!=s.wand||!(WandFocusItem.attached(s.wand).getItem() instanceof KineticFocusItem)||s.tile.isRemoved()
                    ||s.player.level()!=s.tile.getLevel()||!s.tile.getLevel().hasChunkAt(s.tile.getBlockPos())
                    ||s.tile.getLevel().getBlockEntity(s.tile.getBlockPos())!=s.tile
                    ||s.player.distanceToSqr(s.tile.getBlockPos().getCenter())>256) {stop(s);continue;}
            if(!s.scripted) {if(!s.player.isUsingItem())stop(s);continue;}
            var step=s.step();String type=step.getString("Type");
            if(type.isEmpty()||type.equals("END")){stop(s);continue;}
            int modifier=switch(step.getString("Modifier")){case "FORWARD_FAST"->2;case "BACK"->-1;case "BACK_FAST"->-2;default->1;};
            boolean turn=type.startsWith("TURN_");
            if(turn&&Math.abs(modifier)==2&&Math.abs(s.rpm)>128){
                s.player.displayClientMessage(net.minecraft.network.chat.Component.translatable("thaumicomniverse.focus.too_fast"),true);
                stop(s);continue;
            }
            if(turn&&!pay(s,true,Math.abs(modifier))){
                s.player.displayClientMessage(net.minecraft.network.chat.Component.translatable("thaumicomniverse.focus.no_vis"),true);
                stop(s);continue;
            }
            float speed=s.rpm*modifier*s.direction;
            if(!s.initialized) {
                s.duration=switch(type){
                    case "TURN_ANGLE"->(int)Math.ceil(step.getInt("Value")/Math.abs(KineticBlockEntity.convertToAngular(speed)))+2;
                    case "TURN_DISTANCE"->(int)Math.ceil(step.getInt("Value")/Math.abs(KineticBlockEntity.convertToLinear(speed)))+2;
                    case "DELAY"->step.getInt("Value");default->-1;};
                s.timer=0;s.initialized=true;
                float output=turn?KineticBlockEntity.convertToDirection(speed,s.face.getOpposite()):0;
                var context=turn?SequencedGearshiftBlockEntity.SequenceContext.fromGearshift(
                        SequencerInstructions.valueOf(type),output,step.getInt("Value")):null;
                apply(s,output,context);
            }
            if(s.duration>=0&&s.timer++>=s.duration){s.index++;s.initialized=false;}
        }
    }
    private PowerFocusSessions(){}
}
