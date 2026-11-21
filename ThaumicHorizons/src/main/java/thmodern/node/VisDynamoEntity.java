package thmodern.node;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import tc4revival.aspect.*;
import tc4revival.content.CastingWandItem;
import thmodern.Horizons;

public final class VisDynamoEntity extends DynamoEntity implements WandDevice {
    private final Set<String> disabled=new HashSet<>();
    public VisDynamoEntity(BlockPos pos,BlockState state){super(Horizons.VIS_DYNAMO_ENTITY.get(),pos,state);}
    public boolean enabled(Aspect aspect){return !disabled.contains(aspect.tag());}
    public void toggle(Aspect aspect){if(level==null||level.isClientSide||!aspect.isPrimal())return;if(!disabled.remove(aspect.tag()))disabled.add(aspect.tag());sync();}
    @Override public void onWandUseTick(ItemStack stack,Player player,int remainingTicks){
        if(!lookingAt(player)||level==null||level.isClientSide||remainingTicks%5!=0||!(stack.getItem() instanceof CastingWandItem wand))return;
        var next=new AspectAmounts();
        for(var aspect:TCAspects.catalog().primals())if(enabled(aspect)
                &&wand.consumeForPlayer(stack,player,Map.of(aspect,40),false,false,true))next.add(aspect,5);
        supply(next,next.isEmpty()?0:10);
        if(!next.isEmpty()&&level instanceof net.minecraft.server.level.ServerLevel server){
            thaumcraft.api.fx.LegacyEffects.emit(level,"wispy",player.getEyePosition(),worldPosition.getCenter(),visualColor(),1,8,1,0,0);
        }
    }
    @Override protected void saveAdditional(CompoundTag tag){super.saveAdditional(tag);var list=new net.minecraft.nbt.ListTag();disabled.stream().sorted().forEach(s->list.add(net.minecraft.nbt.StringTag.valueOf(s)));tag.put("Disabled",list);}
    @Override public void load(CompoundTag tag){super.load(tag);disabled.clear();for(var value:tag.getList("Disabled",8))disabled.add(value.getAsString());}
}
