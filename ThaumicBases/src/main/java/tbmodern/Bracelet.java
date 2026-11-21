package tbmodern;

import com.google.gson.Gson;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraft.world.item.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.Component;
import tc4revival.content.*;
import tc4revival.aspect.Aspect;

/** Named, JSON-defined casting carriers using the base's shared focus and charging protocol. */
public final class Bracelet extends CastingWandItem {
    public record Definition(String id,int capacity,String rod,String cap,String texture) {}
    public static List<Definition> definitions() {
        try(var reader=new InputStreamReader(Objects.requireNonNull(Bracelet.class.getResourceAsStream("/tbmodern/bracelets.json")),StandardCharsets.UTF_8)) {
            return List.of(new Gson().fromJson(reader,Definition[].class));
        } catch(IOException failure) {throw new IllegalStateException(failure);}
    }
    private final Definition definition;
    public Bracelet(Definition definition) {this.definition=definition;}
    public String texture() {return definition.texture();}
    @Override public WandParts.Rod rod(ItemStack stack) {return WandParts.Rod.require(definition.rod());}
    @Override public WandParts.Cap cap(ItemStack stack) {return WandParts.Cap.require(definition.cap());}
    @Override public boolean isStaff(ItemStack stack) {return true;}
    @Override public boolean isSceptre(ItemStack stack) {return false;}
    @Override public int capacity(ItemStack stack) {return definition.capacity()*100;}
    @Override public int focusCooldownMillis(ItemStack stack,ItemStack focus,int normal) {return normal/2;}
    @Override public int continuousFocusCalls(ItemStack stack,ItemStack focus) {return 2;}
    @Override public Component getName(ItemStack stack) {return Component.translatable(getDescriptionId());}
    @Override public float consumptionModifier(ItemStack stack,Player player,Aspect aspect,boolean crafting) {
        var focus=WandFocusItem.attached(stack);
        int frugal=!crafting && focus.getItem() instanceof WandFocusItem item?item.upgradeLevel(focus,1):0;
        float discount=player==null?0:WandInventory.discount(player,aspect);
        return Math.max(.1F,(cap(stack).cost(aspect.tag())/1.5F-discount-frugal/11F)
            *(float)thaumcraft.api.wands.FocusUpgradeType.costMultiplier(focus,player,aspect));
    }
    @Override public void onUseTick(net.minecraft.world.level.Level level,net.minecraft.world.entity.LivingEntity entity,ItemStack stack,int remaining) {
        if (!(entity instanceof Player) || !thaumcraft.api.wands.WandAutomation.allowed(stack,(Player)entity)) return;
        try (var automation = thaumcraft.api.wands.WandAutomation.open(stack,(Player)entity)) {
        if(entity instanceof Player player) {
            var target=WandUseTarget.read(stack.getTag(),level.dimension().location().toString());
            if(target.isPresent()) {
                var t=target.get();var pos=new net.minecraft.core.BlockPos(t.x(),t.y(),t.z());
                if(level.hasChunkAt(pos) && level.getBlockEntity(pos) instanceof WandInteractable interactable) {
                    interactable.onWandUseTick(stack,player,remaining);
                    if(player.isUsingItem() && player.getUseItem()==stack)interactable.onWandUseTick(stack,player,remaining+1);
                    return;
                }
            }
        }
        super.onUseTick(level,entity,stack,remaining);
            }
    }
    @Override public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
        consumer.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions() {
            @Override public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {return tbmodern.client.BraceletRenderer.instance();}
        });
    }
}
