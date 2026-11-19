package timodern.mixin;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import timodern.*;
@Mixin(value=FallingBlockEntity.class,remap=false)
public class InfusedFallingMixin {
    @Inject(method={"tick","m_8119_"},at=@At("RETURN"))
    private void tiLand(CallbackInfo ci){var self=(FallingBlockEntity)(Object)this;if(self.level() instanceof ServerLevel l&&self.isRemoved()&&self.getPersistentData().contains("ti_infusion")&&l.getBlockState(self.blockPosition()).is(self.getBlockState().getBlock())){InfusionData.get(l).entries.put(self.blockPosition().asLong(),InfusionData.Entry.load(self.getPersistentData().getCompound("ti_infusion")));self.getPersistentData().remove("ti_infusion");InfusionData.changed(l,self.blockPosition());}}
}
