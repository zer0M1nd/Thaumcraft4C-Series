package wgmodern;
import java.util.*;import net.minecraft.server.level.ServerPlayer;import net.minecraft.world.*;import net.minecraft.world.entity.Entity;import net.minecraft.world.entity.player.Player;import net.minecraft.world.item.*;import net.minecraft.world.level.Level;import net.minecraft.network.chat.Component;import net.minecraft.nbt.CompoundTag;import tc4revival.research.*;import tc4revival.network.*;
public class ScanCamera extends Item {
 private static final Map<ServerPlayer,CompoundTag> PREVIEWS=Collections.synchronizedMap(new WeakHashMap<>());
 public ScanCamera(){super(new Properties().stacksTo(1));}
 @Override public String getDescriptionId(){return "item.WG_ScanCamera";}
 public static Item mirrorGlass(){return net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(new net.minecraft.resources.ResourceLocation("thaumcraft","mirrored_glass"));}
 @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){var held=player.getItemInHand(hand);if(player instanceof ServerPlayer p){var selection=ScanTargetSelector.select(p);selection.ifPresent(value->take(p,value));}return InteractionResultHolder.consume(held);}
 public static boolean take(ServerPlayer p,ScanTargetSelector.Selection selection){
  var data=KnowledgeSavedData.get(p.getServer());data.ensure(p.getUUID(),p.getRandom()::nextInt);
  TCNetwork.send(p,new ScanEffectPacket(p.level().dimension().location(),selection.runeOrigin(),selection.runeLifetime(),false));
  if(!PhotoData.canPhotograph(data.snapshot(p.getUUID()).orElseThrow(),selection.target().aspects()))return false;
  var scan=PhotoData.capture(p,selection);if(!PhotoData.valid(scan))return false;
  for(int i=0;i<p.getInventory().items.size();i++)if(p.getInventory().getItem(i).is(mirrorGlass())){p.getInventory().removeItem(i,1);var photo=PhotoData.create(false,scan);if(!p.getInventory().add(photo))p.drop(photo,false);for(String sound:List.of("cameradone","cameraticks"))p.level().playSound(null,p.getX(),p.getY(),p.getZ(),net.minecraft.sounds.SoundEvent.createVariableRangeEvent(new net.minecraft.resources.ResourceLocation("thaumcraft",sound)),net.minecraft.sounds.SoundSource.PLAYERS,.3f,1.9f+p.getRandom().nextFloat()*.2f);return true;}
  return false;
 }
 @Override public void inventoryTick(ItemStack stack,Level level,Entity entity,int slot,boolean selected){if(!(entity instanceof ServerPlayer p)||p.tickCount%4!=0||(!selected&&p.getOffhandItem()!=stack))return;var selection=ScanTargetSelector.select(p);var tag=new CompoundTag();if(selection.isPresent()){var s=selection.get();tag=PhotoData.summary(s.target(),s.name());var knowledge=KnowledgeSavedData.get(p.getServer()).snapshot(p.getUUID());if(knowledge.isEmpty()||!PhotoData.scanned(tag,knowledge.get()))tag.remove("aspects");}if(!tag.equals(PREVIEWS.get(p))){PREVIEWS.put(p,tag.copy());WGNetwork.cameraPreview(p,tag);}}
 @Override public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> c){c.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions(){public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer(){return CameraRenderer.instance();}});}
 public static class Photograph extends Item {
  public final boolean developed;public Photograph(boolean developed){super(new Properties().stacksTo(developed?1:64));this.developed=developed;}
  @Override public String getDescriptionId(){return "item.WG_Material."+(developed?"developedPhoto":"photoPlate");}
  @Override public InteractionResultHolder<ItemStack> use(Level level,Player p,InteractionHand hand){var stack=p.getItemInHand(hand);if(developed&&p instanceof ServerPlayer sp)learn(sp,stack);return InteractionResultHolder.sidedSuccess(stack,level.isClientSide);}
  public static boolean learn(ServerPlayer p,ItemStack stack){var target=PhotoData.resolve(PhotoData.of(stack),p.serverLevel());if(target.isEmpty())return false;var store=KnowledgeSavedData.get(p.getServer());store.ensure(p.getUUID(),p.getRandom()::nextInt);if(!target.get().canTrack(store.snapshot(p.getUUID()).orElseThrow(),PlayerKnowledge.ScanMode.THAUMOMETER))return false;var result=store.update(p.getUUID(),p.getRandom()::nextInt,k->target.get().completeWithClue(k,tc4revival.aspect.TCAspects.catalog(),PlayerKnowledge.ScanMode.THAUMOMETER,tc4revival.config.TCConfig.SERVER.aspectPoolSoftCap.get(),ResearchCatalogs.rules(),p.getRandom()::nextInt));TCNetwork.send(p,ScanFeedbackPacket.from(result.scan()));result.clue().ifPresent(key->TCNetwork.send(p,new CluePacket(key)));net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new thaumcraft.api.research.ScanEvent(p,target.get(),result.scan()));if(result.scan().succeeded()){stack.shrink(1);return true;}return false;}
  @Override public void appendHoverText(ItemStack s,Level l,List<Component> tips,TooltipFlag flag){if(l!=null&&l.isClientSide)CameraClient.tooltip(s,tips);}
  @Override public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> c){c.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions(){public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer(){return PhotoRenderer.instance();}});}
 }
}
