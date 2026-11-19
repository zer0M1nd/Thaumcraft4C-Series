package wgmodern;
import java.util.*;import net.minecraft.nbt.*;import net.minecraft.network.chat.Component;import net.minecraft.world.*;import net.minecraft.world.entity.*;import net.minecraft.world.entity.player.Player;import net.minecraft.world.item.*;import net.minecraft.world.level.Level;import net.minecraft.util.Mth;
public class LabyrinthString extends Item {
 public static final int MAX_POINTS=8192;
 public LabyrinthString(){super(new Properties().stacksTo(1));}@Override public String getDescriptionId(){return "item.WG_Material.guidingString";}
 public static ListTag points(ItemStack s){return s.hasTag()?s.getTag().getList("pos",10):new ListTag();}
 public static boolean active(ItemStack s){return s.hasTag()&&s.getTag().getBoolean("active");}
 public static void append(ItemStack s,Entity e){var list=points(s);if(list.size()>=MAX_POINTS){s.getOrCreateTag().putBoolean("active",false);return;}var p=new CompoundTag();p.putDouble("x",e.getX());p.putDouble("y",e.getY());p.putDouble("z",e.getZ());p.putInt("facing",Mth.floor(e.getYRot()*8/360+.5)&7);list.add(p);s.getOrCreateTag().put("pos",list);}
 @Override public InteractionResultHolder<ItemStack> use(Level l,Player p,InteractionHand hand){var stack=p.getItemInHand(hand);if(!l.isClientSide){if(p.isShiftKeyDown()){stack.getOrCreateTag().remove("pos");stack.getTag().putBoolean("active",false);}else{stack.getOrCreateTag().putBoolean("active",!active(stack));append(stack,p);}}return InteractionResultHolder.sidedSuccess(stack,l.isClientSide);}
 @Override public void inventoryTick(ItemStack s,Level l,Entity e,int slot,boolean held){if(l.isClientSide||!active(s))return;var list=points(s);if(list.isEmpty())return;var last=list.getCompound(list.size()-1);/* The original distance counts Z twice and omits Y. */double distance=Math.abs(last.getDouble("x")-e.getX())+2*Math.abs(last.getDouble("z")-e.getZ());int facing=Mth.floor(e.getYRot()*8/360+.5)&7;if(last.getInt("facing")!=facing&&distance>1)append(s,e);}
 @Override public boolean isFoil(ItemStack s){return active(s);}@Override public void appendHoverText(ItemStack s,Level l,List<Component> lines,TooltipFlag flag){if(active(s))lines.add(Component.translatable("wg.desc.active"));}
}
