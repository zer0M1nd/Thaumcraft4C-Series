package thaumicomniverse.compat.create;

import com.simibubi.create.api.behaviour.interaction.MovingInteractionBehaviour;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import net.minecraft.core.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraft.world.phys.AABB;
import tc4revival.content.*;

public class MovingArcaneDoor extends MovingInteractionBehaviour implements MovementBehaviour {
    public static void setup(){
        var behavior=new MovingArcaneDoor();
        for(var block:net.minecraftforge.registries.ForgeRegistries.BLOCKS){
            if(block instanceof ArcaneDoorBlock||block instanceof ArcanePressurePlateBlock)MovingInteractionBehaviour.REGISTRY.register(block,behavior);
            if(block instanceof ArcanePressurePlateBlock)MovementBehaviour.REGISTRY.register(block,behavior);
        }
    }
    private static OwnedDeviceAccess access(StructureBlockInfo info){
        var access=new OwnedDeviceAccess();if(info!=null&&info.nbt()!=null)access.load(info.nbt());return access;
    }
    private static void state(AbstractContraptionEntity entity,BlockPos local,BlockState state){
        var old=entity.getContraption().getBlocks().get(local);if(old==null||old.state()==state)return;
        var info=new StructureBlockInfo(local,state,old.nbt());entity.setBlock(local,info);
        for(var actor:entity.getContraption().getActors())if(actor.left.pos().equals(local)){
            actor.setLeft(info);if(actor.right!=null)actor.right.state=state;
        }
    }
    private static void open(AbstractContraptionEntity entity,BlockPos root,boolean open){
        for(var pos:new BlockPos[]{root,root.above()}){
            var info=entity.getContraption().getBlocks().get(pos);
            if(info!=null&&info.state().getBlock() instanceof ArcaneDoorBlock)state(entity,pos,info.state().setValue(ArcaneDoorBlock.OPEN,open));
        }
    }
    @Override public boolean handlePlayerInteraction(Player player,InteractionHand hand,BlockPos local,AbstractContraptionEntity entity){
        if(!ArcaneContraptions.enabled())return false;
        var info=entity.getContraption().getBlocks().get(local);if(info==null)return false;
        if(player.level().isClientSide)return true;
        if(info.state().getBlock() instanceof ArcaneDoorBlock){
            var root=ArcaneDoorBlock.root(info.state(),local);var owned=access(entity.getContraption().getBlocks().get(root));
            if(owned.mayUse(player.getGameProfile().getName()))open(entity,root,!info.state().getValue(ArcaneDoorBlock.OPEN));
            else player.displayClientMessage(net.minecraft.network.chat.Component.translatable("The door refuses to budge."),false);
        }else if(info.state().getBlock() instanceof ArcanePressurePlateBlock&&access(info).mayConfigure(player.getGameProfile().getName()))
            state(entity,local,info.state().cycle(ArcanePressurePlateBlock.MODE));
        return true;
    }
    @Override public void tick(MovementContext context){
        if(!ArcaneContraptions.enabled()||context.world.isClientSide||context.position==null||context.world.getGameTime()%5!=0)return;
        var entity=context.contraption.entity;var plate=context.contraption.getBlocks().get(context.localPos);if(plate==null)return;
        var owned=access(plate);boolean powered=false;
        var area=new AABB(.125,0,.125,.875,.25,.875).move(context.localPos);
        for(var candidate:context.world.getEntities(entity,new AABB(context.position,context.position).inflate(2))){
            var at=entity.toLocalVector(candidate.position(),1);
            if(!area.contains(at))continue;
            String name=candidate instanceof Player player?player.getGameProfile().getName():null;
            if(owned.triggersPlate(context.state.getValue(ArcanePressurePlateBlock.MODE),name,candidate.isIgnoringBlockTriggers())){powered=true;break;}
        }
        if(powered==context.state.getValue(ArcanePressurePlateBlock.POWERED))return;
        ArcaneMovement.state(context,context.state.setValue(ArcanePressurePlateBlock.POWERED,powered));
        for(var direction:Direction.Plane.HORIZONTAL){
            var pos=context.localPos.relative(direction);var door=context.contraption.getBlocks().get(pos);
            if(door==null||!(door.state().getBlock() instanceof ArcaneDoorBlock))continue;
            var root=ArcaneDoorBlock.root(door.state(),pos);var doorAccess=access(context.contraption.getBlocks().get(root));
            if(!doorAccess.matchesPressurePlate(owned))continue;
            boolean any=false;
            for(var level:new BlockPos[]{root,root.above()})for(var side:Direction.Plane.HORIZONTAL){
                var other=context.contraption.getBlocks().get(level.relative(side));
                if(other!=null&&other.state().getBlock() instanceof ArcanePressurePlateBlock&&other.state().getValue(ArcanePressurePlateBlock.POWERED)&&doorAccess.matchesPressurePlate(access(other)))any=true;
            }
            open(entity,root,any);
        }
    }
}
