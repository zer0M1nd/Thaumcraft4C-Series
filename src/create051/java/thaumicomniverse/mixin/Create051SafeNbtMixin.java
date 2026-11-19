package thaumicomniverse.mixin;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import com.simibubi.create.content.schematics.SchematicExport;
import com.simibubi.create.content.schematics.SchematicAndQuillItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import thaumicomniverse.compat.create.LegacySafeNbt;
@Mixin(value=SchematicExport.class,remap=false)
public abstract class Create051SafeNbtMixin {
    @Redirect(method="saveSchematic",at=@At(value="INVOKE",target="Lcom/simibubi/create/content/schematics/SchematicAndQuillItem;replaceStructureVoidWithAir(Lnet/minecraft/nbt/CompoundTag;)V"))
    private static void omniverse$safe(CompoundTag data,java.nio.file.Path dir,String name,boolean overwrite,Level level,BlockPos first,BlockPos second){
        SchematicAndQuillItem.replaceStructureVoidWithAir(data);
        var origin=new BlockPos(Math.min(first.getX(),second.getX()),Math.min(first.getY(),second.getY()),Math.min(first.getZ(),second.getZ()));
        LegacySafeNbt.sanitize(data,level,origin);
    }
}
