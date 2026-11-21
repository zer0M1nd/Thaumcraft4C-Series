package gadomancymodern.item;

import net.minecraft.world.item.*;
import net.minecraft.world.InteractionResult;
import tc4revival.content.*;

/** Original six creative-only acquisition variants; placing does not consume the tool. */
public class CreativeNodeItem extends Item {
    public CreativeNodeItem(){super(new Properties());}
    public static AuraNodeType type(ItemStack stack){return AuraNodeType.fromId(stack.hasTag()?stack.getTag().getInt("NodeType"):0);}
    public static java.util.List<ItemStack> variants(Item item){var list=new java.util.ArrayList<ItemStack>();for(var type:AuraNodeType.values()){var stack=new ItemStack(item);stack.getOrCreateTag().putInt("NodeType",type.ordinal());list.add(stack);}return list;}
    @Override public String getDescriptionId(){return TCItems.AURA_NODE.get().getDescriptionId();}
    @Override public InteractionResult useOn(net.minecraft.world.item.context.UseOnContext context){var level=context.getLevel();var at=context.getClickedPos().relative(context.getClickedFace());if(!level.isEmptyBlock(at))return InteractionResult.PASS;if(level.isClientSide)return InteractionResult.SUCCESS;if(!AuraNodeGeneration.createRandom((net.minecraft.server.level.ServerLevel)level,at,level.random,false,false,false))return InteractionResult.PASS;var node=(AuraNodeBlockEntity)level.getBlockEntity(at);var type=type(context.getItemInHand());if(type==AuraNodeType.NORMAL){var values=new tc4revival.aspect.AspectAmounts();for(var aspect:tc4revival.aspect.TCAspects.catalog().primals())values.add(aspect,500);node.configure(values,type,AuraNodeModifier.BRIGHT,node.nodeId(),false);}else node.editPermanent(node.permanentBase(),type,node.permanentModifier());return InteractionResult.CONSUME;}
    @Override public void appendHoverText(ItemStack stack,net.minecraft.world.level.Level level,java.util.List<net.minecraft.network.chat.Component> lines,TooltipFlag flag){var type=type(stack);lines.add(net.minecraft.network.chat.Component.literal(type==AuraNodeType.NORMAL?"Place a huge and bright aura node":"Place a node of type "+type.name().toLowerCase(java.util.Locale.ROOT)).withStyle(net.minecraft.ChatFormatting.DARK_PURPLE));lines.add(net.minecraft.network.chat.Component.literal("Creative Mode Only").withStyle(net.minecraft.ChatFormatting.ITALIC));}
    @Override public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer){consumer.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions(){@Override public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer(){return gadomancymodern.client.CreativeNodeRenderer.instance();}});}
}
