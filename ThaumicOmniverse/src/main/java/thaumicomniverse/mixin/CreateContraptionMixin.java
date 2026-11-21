package thaumicomniverse.mixin;

import java.util.Map;
import com.simibubi.create.content.contraptions.Contraption;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import thaumicomniverse.compat.create.*;

@Mixin(value=Contraption.class,remap=false)
public abstract class CreateContraptionMixin implements ContraptionAccess {
    @Shadow protected Map<BlockPos,CompoundTag> updateTags;
    @Unique private MountedArcaneStorage omniverse$mounted;
    @Unique private java.util.UUID omniverse$frameId=java.util.UUID.randomUUID();
    @Override public java.util.UUID omniverse$frameId(){return omniverse$frameId;}
    @Override public MountedArcaneStorage omniverse$storage() {
        if(omniverse$mounted==null)omniverse$mounted=new MountedArcaneStorage((Contraption)(Object)this);
        return omniverse$mounted;
    }
    @Override public Map<BlockPos,CompoundTag> omniverse$updates(){return updateTags;}
    @Inject(method="moveBlock",at=@At("HEAD"))
    private void omniverse$coupler(net.minecraft.world.level.Level level,net.minecraft.core.Direction forced,java.util.Queue<BlockPos> frontier,java.util.Set<BlockPos> visited,CallbackInfoReturnable<Boolean> cir){
        var at=frontier.peek();if(at==null)return;var partner=SuspensionCoupler.partner(level,at);
        if(partner!=null&&!visited.contains(partner)&&!frontier.contains(partner))frontier.add(partner);
    }
    @Inject(method="readNBT",at=@At("HEAD"))
    private void omniverse$reset(net.minecraft.world.level.Level level,CompoundTag tag,boolean spawn,CallbackInfo ci){omniverse$mounted=null;omniverse$frameId=tag.hasUUID("OmniGolemFrame")?tag.getUUID("OmniGolemFrame"):java.util.UUID.randomUUID();}
    @Inject(method="writeNBT",at=@At("RETURN"))
    private void omniverse$saveFrame(boolean spawn,CallbackInfoReturnable<CompoundTag> cir){cir.getReturnValue().putUUID("OmniGolemFrame",omniverse$frameId);}
    @Inject(method="removeBlocksFromWorld",at=@At("HEAD"))
    private void omniverse$mountGolems(net.minecraft.world.level.Level level,BlockPos offset,CallbackInfo ci){MovingGolemBindings.mount((Contraption)(Object)this,level,offset);}
    @Inject(method="addBlocksToWorld",at=@At("TAIL"))
    private void omniverse$unmountGolems(net.minecraft.world.level.Level level,com.simibubi.create.content.contraptions.StructureTransform transform,CallbackInfo ci){MovingGolemBindings.unmount((Contraption)(Object)this,level,transform);}
    @Inject(method="getBlockEntityNBT",at=@At("RETURN"))
    private void omniverse$capture(net.minecraft.world.level.Level level,BlockPos pos,CallbackInfoReturnable<CompoundTag> cir){
        var tag=cir.getReturnValue();if(tag==null||!ArcaneContraptions.enabled())return;
        if(level.getBlockEntity(pos) instanceof tc4revival.content.ItemMirrorBlockEntity mirror){mirror.prepareMovement();tag.merge(mirror.saveWithoutMetadata());}
        if(level.getBlockEntity(pos) instanceof tc4revival.content.ArcaneBoreBlockEntity bore){
            var base=bore.base();tag.putBoolean("OmniBorePowered",level.hasNeighborSignal(pos)||base!=null&&level.hasNeighborSignal(base.pos()));
        }
    }
}
