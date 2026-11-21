package thaumicomniverse.content;

import tc4revival.content.*;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

public class EssentiaServoItem extends Item {
    public EssentiaServoItem() { super(new Properties()); }
    @Override public InteractionResult useOn(UseOnContext context) {
        if(!thaumicomniverse.RevivalFeatures.Group.TUBES.enabled())return InteractionResult.PASS;
        var player=context.getPlayer();var level=context.getLevel();var pos=context.getClickedPos();
        if(player==null||!(level.getBlockEntity(pos) instanceof ConvenientTubeBlockEntity tube))return InteractionResult.PASS;
        if(!level.mayInteract(player,pos)||!player.mayUseItemAt(pos,context.getClickedFace(),context.getItemInHand()))return InteractionResult.FAIL;
        int part=EssentiaTubeBlock.tracePart(tube,player);
        if(part<0||part>=6)return InteractionResult.FAIL;
        var side=net.minecraft.core.Direction.from3DDataValue(part);
        if(tube.servo(side)||!tube.storageFace(side))return InteractionResult.FAIL;
        if(!level.isClientSide){tube.setServo(side,true);if(!player.getAbilities().instabuild)context.getItemInHand().shrink(1);}
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
