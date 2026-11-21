package thmodern.plane;
import java.util.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;

public final class KeystoneItem extends Item {
 public KeystoneItem(){super(new Properties().stacksTo(1));}
 public static boolean bound(ItemStack stack){return stack.hasTag()&&stack.getTag().contains("PlaneId")&&stack.getTag().hasUUID("Allocation");}
 public static Optional<PocketPlanes.Plane> resolve(ItemStack stack,ServerLevel level){if(!bound(stack))return Optional.empty();return PocketPlanes.get(level.getServer()).get(stack.getTag().getInt("PlaneId")).filter(p->p.ready()&&p.allocation().equals(stack.getTag().getUUID("Allocation")));}
 public static boolean bind(ItemStack stack,PocketPlanes.Plane plane){if(bound(stack)||!plane.ready())return false;var tag=stack.getOrCreateTag();tag.putInt("PlaneId",plane.id());tag.putUUID("Allocation",plane.allocation());tag.putString("PlaneName",plane.name());tag.putInt("PlaneColor",plane.color());return true;}
 @Override public String getDescriptionId(ItemStack stack){return bound(stack)?"item.thaumichorizons.keystone_bound":"item.thaumichorizons.keystone";}
 @Override public InteractionResult onItemUseFirst(ItemStack stack,UseOnContext context){
  var level=context.getLevel();var player=context.getPlayer();if(player==null||bound(stack)||!level.dimension().location().equals(PocketPlanes.DIMENSION)||!level.mayInteract(player,context.getClickedPos())||!player.mayUseItemAt(context.getClickedPos(),context.getClickedFace(),stack)||!(level.getBlockEntity(context.getClickedPos()) instanceof VortexEntity vortex))return InteractionResult.PASS;
  if(level instanceof ServerLevel server){var plane=PocketPlanes.get(server.getServer()).get(vortex.planeId()).filter(p->p.allocation().equals(vortex.allocation()));if(plane.isEmpty()||!bind(stack,plane.get()))return InteractionResult.FAIL;player.getInventory().setChanged();}return InteractionResult.sidedSuccess(level.isClientSide);
 }
 @Override public void appendHoverText(ItemStack stack,Level level,List<Component> lines,TooltipFlag flag){if(bound(stack))lines.add(Component.literal(stack.getTag().getString("PlaneName")));}
 public static int color(ItemStack stack){return bound(stack)?stack.getTag().getInt("PlaneColor"):0xffffff;}
}
