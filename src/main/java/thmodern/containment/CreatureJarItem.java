package thmodern.containment;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import thmodern.Horizons;

public final class CreatureJarItem extends BlockItem {
    public CreatureJarItem(){super(Horizons.CREATURE_JAR.get(),new Properties().stacksTo(1));}
    @Override public Component getName(ItemStack stack){if(stack.hasTag()&&stack.getTag().contains("VatRecovery"))return Component.translatable("thaumichorizons.jar.vat_recovery");if(SoulJar.contains(stack))return Component.translatable("thaumichorizons.jar.soul");return stack.hasTag()&&stack.getTag().contains("CreatureName")?Component.translatable("thaumichorizons.jar.creature",stack.getTag().getString("CreatureName")):super.getName(stack);}
    @Override public void appendHoverText(ItemStack stack,net.minecraft.world.level.Level level,java.util.List<Component> lines,TooltipFlag flag){super.appendHoverText(stack,level,lines,flag);if(stack.hasTag()&&stack.getTag().contains("VatRecovery"))lines.add(Component.translatable("thaumichorizons.jar.vat_recovery_hint"));}
    @Override public InteractionResult place(BlockPlaceContext context){
        var stack=context.getItemInHand();if(!stack.hasTag()||(!stack.getTag().contains("Captured")&&!stack.getTag().contains("VatRecovery")&&!SoulJar.contains(stack)))return InteractionResult.FAIL;
        var result=super.place(context);if(result.consumesAction()&&context.getPlayer()!=null&&context.getPlayer().isCreative())stack.shrink(1);return result;
    }
    @Override public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer){consumer.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions(){private net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer renderer;@Override public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer(){if(renderer==null)renderer=new thmodern.client.CreatureJarItemRenderer();return renderer;}});}
}
