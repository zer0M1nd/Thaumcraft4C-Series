package thaumicomniverse.mixin;

import java.util.function.Consumer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.*;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ItemStack.class, remap = false)
public abstract class CreateVoidDurabilityMixin {
    @Inject(method = {"hurtAndBreak", "m_41622_"}, at = @At("HEAD"), cancellable = true, remap = false)
    private <T extends LivingEntity> void omniverse$voidTools(int damage, T holder, Consumer<T> broken, CallbackInfo ci) {
        if(damage<=0 || !(holder instanceof net.minecraftforge.common.util.FakePlayer)
                || !thaumicomniverse.RevivalFeatures.VOID_FAKE_PLAYER_DURABILITY.get()) return;
        boolean deployer=thaumicomniverse.RevivalFeatures.Group.CREATE.enabled()
                &&holder.getClass().getName().equals("com.simibubi.create.content.kinetics.deployer.DeployerFakePlayer");
        boolean tablet=thaumcraft.api.wands.WandActors.resolveContext(holder).origin().map(origin->origin.kind().toString().equals("thaumic_tinkerer:tablet")).orElse(false);
        if(!deployer&&!tablet)return;
        Item item = ((ItemStack) (Object) this).getItem();
        var id = ForgeRegistries.ITEMS.getKey(item);
        if (item instanceof TieredItem tool && tool.getTier() == tc4revival.content.VoidToolTier.INSTANCE
                || id != null && id.getNamespace().equals("thaumicomniverse") && java.util.Set.of("void_sandpaper","void_knife","void_paxel").contains(id.getPath())) ci.cancel();
    }
}
