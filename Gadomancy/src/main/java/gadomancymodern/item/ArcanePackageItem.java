package gadomancymodern.item;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

/** Packages drop their recorded contents exactly once, on the server. */
public class ArcanePackageItem extends Item {
    private final boolean disguised;
    public ArcanePackageItem(boolean disguised){super(new Properties());this.disguised=disguised;}
    @Override public String getDescriptionId(){return disguised?"item.ItemLootbag.0.name":"item.ItemArcanePackage.name";}
    @Override public Component getName(ItemStack stack){return disguised?Component.translatable(stack.hasTag()&&stack.getTag().getBoolean("essentia_wrapper")?"item.thaumcraft.uncommon_loot_bag":"item.thaumcraft.common_loot_bag"):super.getName(stack);}
    public boolean setContents(ItemStack stack,List<ItemStack> contents){
        var encoded=PackageCodec.encode(contents);if(encoded.isEmpty())return false;
        stack.getOrCreateTag().putByteArray("items",encoded.get());return true;
    }
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
        ItemStack stack=player.getItemInHand(hand);
        var contents=PackageCodec.decode(stack.getTag()==null?new byte[0]:stack.getTag().getByteArray("items"));
        if(contents.isEmpty())return InteractionResultHolder.fail(stack);
        if(!level.isClientSide){
            // Debit before spawning so reentrant inventory listeners cannot open the same package twice.
            stack.shrink(1);
            for(ItemStack content:contents.get())level.addFreshEntity(new ItemEntity(level,player.getX(),player.getY(),player.getZ(),content));
            level.playSound(null,player.blockPosition(),SoundEvent.createVariableRangeEvent(new ResourceLocation("thaumcraft","coins")),SoundSource.PLAYERS,.75f,1f);
        }
        return InteractionResultHolder.sidedSuccess(stack,level.isClientSide);
    }
    @Override public void appendHoverText(ItemStack stack,Level level,List<Component> tooltip,TooltipFlag flags){
        if(!disguised)tooltip.add(Component.translatable("gadomancy.info.ArcanePackage"));
    }
}
