package automagy.compat.jade;

import automagy.content.*;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import snownee.jade.api.*;
import snownee.jade.api.config.IPluginConfig;
import tc4revival.content.BrainJarBlockEntity;
import tc4revival.content.GolemEntity;

/** Loaded by Jade's annotation scanner only; no hard dependency or client-only server references. */
@WailaPlugin
public final class AutomagyJadePlugin implements IWailaPlugin {
 private static final ResourceLocation BLOCK=ResourceLocation.parse("automagy:machines"),GOLEM=ResourceLocation.parse("automagy:golem_link"),STONE=ResourceLocation.parse("automagy:experience_stone"),BRAIN=ResourceLocation.parse("automagy:brain_jar");
 private static final String KEY="AutomagyInfo";
 @Override public void register(IWailaCommonRegistration registration){registration.registerBlockDataProvider(BlockInfo.INSTANCE,BlockEntity.class);registration.registerEntityDataProvider(GolemInfo.INSTANCE,GolemEntity.class);}
 @Override public void registerClient(IWailaClientRegistration registration){
  registration.registerBlockComponent(BlockInfo.INSTANCE,Block.class);registration.registerEntityComponent(GolemInfo.INSTANCE,GolemEntity.class);
  registration.addConfig(STONE,true);registration.addConfig(BRAIN,true);
 }
 private static void add(ListTag lines,Component text){lines.add(StringTag.valueOf(Component.Serializer.toJson(text)));}
 private static void append(ITooltip tooltip,CompoundTag data){
  for(var line:data.getList("Lines",Tag.TAG_STRING)){var text=Component.Serializer.fromJson(line.getAsString());if(text!=null)tooltip.add(text);}
 }
 private static void fluid(ListTag lines,net.minecraftforge.fluids.capability.templates.FluidTank tank){
  if(tank.isEmpty())add(lines,Component.translatable("jade.automagy.fluid_empty",tank.getCapacity()));
  else add(lines,Component.translatable("jade.automagy.fluid",tank.getFluidAmount(),tank.getCapacity(),tank.getFluid().getDisplayName()));
 }
 private static final class BlockInfo implements IServerDataProvider<BlockAccessor>,IBlockComponentProvider {
  static final BlockInfo INSTANCE=new BlockInfo();
  public ResourceLocation getUid(){return BLOCK;}
  public void appendServerData(CompoundTag out,BlockAccessor accessor){
   var tile=accessor.getBlockEntity();var data=new CompoundTag();var lines=new ListTag();
   if(tile instanceof HourglassBlockEntity glass){
    add(lines,Component.translatable("jade.automagy.timer",glass.timer.target()/20));
    if(glass.timer.running())add(lines,Component.translatable("jade.automagy.remaining",glass.timer.remainingSeconds()));
   }
   if(tile instanceof RedcrystalBlockEntity crystal){
    add(lines,Component.translatable("jade.automagy.power",Math.min(15,crystal.power),Math.max(0,crystal.power-15)));
    if(crystal.kind().equals("dim"))add(lines,Component.translatable("jade.automagy.cap",crystal.setting));
    if(crystal.kind().equals("dense"))add(lines,Component.translatable("jade.automagy.minimum",crystal.setting));
   }
   if(tile instanceof GolemInhibitorBlockEntity inhibitor)add(lines,Component.translatable(inhibitor.omitLinked?"jade.automagy.omit":"jade.automagy.exclusive"));
   if(tile instanceof BoilerBlockEntity boiler)fluid(lines,boiler.tank);
   if(tile instanceof ThirstyTankBlockEntity tank){fluid(lines,tank.tank);int glyph=tank.glyph(accessor.getSide());if(glyph>0&&Content.TANK_GLYPHS.containsKey(glyph))add(lines,new net.minecraft.world.item.ItemStack(Content.TANK_GLYPHS.get(glyph).get()).getHoverName().copy().withStyle(net.minecraft.ChatFormatting.AQUA));}
   int xp=-1;
   if(tile instanceof ExperienceContainer container){xp=container.experience();if(!(tile instanceof ExperienceJarBlockEntity))data.putString("Option","stone");}
   if(tile instanceof BrainJarBlockEntity brain){xp=brain.experience();data.putString("Option","brain");}
   if(xp>=0)add(lines,Component.translatable("jade.automagy.experience",xp,java.math.BigDecimal.valueOf(automagy.logic.ExperienceLevels.level(xp)).setScale(1,java.math.RoundingMode.DOWN).toPlainString()));
   if(!lines.isEmpty()){data.put("Lines",lines);out.put(KEY,data);}
  }
  public void appendTooltip(ITooltip tooltip,BlockAccessor accessor,IPluginConfig config){
   var data=accessor.getServerData().getCompound(KEY);String option=data.getString("Option");
   if(option.equals("stone")&&!config.get(STONE)||option.equals("brain")&&!config.get(BRAIN))return;append(tooltip,data);
  }
 }
 private static final class GolemInfo implements IServerDataProvider<EntityAccessor>,IEntityComponentProvider {
  static final GolemInfo INSTANCE=new GolemInfo();
  public ResourceLocation getUid(){return GOLEM;}
  public void appendServerData(CompoundTag out,EntityAccessor accessor){
   if(!(accessor.getEntity() instanceof GolemEntity golem))return;var lines=new ListTag();
   var tag=golem.configuration().addonData(GolemLinkerItem.LINK);var level=golem.level();
   if(tag.hasUUID("Device")&&tag.getString("Dimension").equals(level.dimension().location().toString())){
    var pos=NbtUtils.readBlockPos(tag.getCompound("Position"));
    if(level.hasChunkAt(pos)){var tile=level.getBlockEntity(pos);if(tile instanceof GolemLinkDevice device&&GolemLinkerItem.linked(golem,tile,device))add(lines,Component.translatable("jade.automagy.link",tile.getBlockState().getBlock().getName(),pos.getX(),pos.getY(),pos.getZ()));}
   }
   if(!golem.configuration().addonData(InventariumDeliveryGoal.ID).isEmpty())add(lines,Component.translatable("jade.automagy.delivery"));
   if(!lines.isEmpty()){var data=new CompoundTag();data.put("Lines",lines);out.put(KEY,data);}
  }
  public void appendTooltip(ITooltip tooltip,EntityAccessor accessor,IPluginConfig config){append(tooltip,accessor.getServerData().getCompound(KEY));}
 }
}
