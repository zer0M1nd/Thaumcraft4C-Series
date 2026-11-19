package thmodern.misc;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import tc4revival.content.AuraNodeBlockEntity;
import thmodern.Horizons;
import thmodern.plane.VortexEntity;
/** Legacy creative testing tool; replaces a normal aura node while retaining its base aspects. */
public final class NodeCheatItem extends Item {
 public NodeCheatItem(){super(new Properties());}
 @Override public InteractionResult useOn(UseOnContext context){
  var level=context.getLevel();var pos=context.getClickedPos();if(!(level.getBlockEntity(pos) instanceof AuraNodeBlockEntity node))return InteractionResult.PASS;
  if(level.isClientSide)return InteractionResult.SUCCESS;
  if(!(context.getPlayer() instanceof ServerPlayer player)||!player.mayBuild()||!level.mayInteract(player,pos)||!player.mayUseItemAt(pos,context.getClickedFace(),context.getItemInHand()))return InteractionResult.FAIL;
  var state=level.getBlockState(pos);var saved=node.saveWithFullMetadata();var aspects=thaumcraft.api.nodes.NodeApi.getBase(node).unwrap().copy();
  var snapshot=net.minecraftforge.common.util.BlockSnapshot.create(level.dimension(),level,pos);
  if(net.minecraftforge.common.ForgeHooks.onBlockBreakEvent(player.serverLevel(),player.gameMode.getGameModeForPlayer(),player,pos)<0||level.getBlockEntity(pos)!=node)return InteractionResult.FAIL;
  if(!level.setBlock(pos,Horizons.VORTEX.get().defaultBlockState(),3))return InteractionResult.FAIL;
  if(!net.minecraftforge.event.ForgeEventFactory.onBlockPlace(player,snapshot,context.getClickedFace())&&level.getBlockEntity(pos) instanceof VortexEntity vortex){vortex.initialize(aspects,true);return InteractionResult.SUCCESS;}
  level.setBlock(pos,state,3);if(level.getBlockEntity(pos)!=null)level.getBlockEntity(pos).load(saved);return InteractionResult.FAIL;
 }
 @Override public void appendHoverText(ItemStack stack,Level level,java.util.List<Component> lines,TooltipFlag flag){lines.add(Component.translatable("thaumichorizons.node_cheat.tip"));}
}
