package emtmodern.energy;

import emtmodern.ElectroMagicTools;
import net.minecraft.core.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import java.util.List;

public class SolarBlock extends BaseEntityBlock implements ic2.api.tile.IWrenchAble {
    public final String kind;
    public final int compression;
    public SolarBlock(String kind,int compression){super(Properties.of().strength(4).sound(SoundType.METAL));this.kind=kind;this.compression=compression;}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new SolarBlockEntity(pos,state);}
    @Override public RenderShape getRenderShape(BlockState state){return RenderShape.MODEL;}
    @Override public void appendHoverText(ItemStack stack,BlockGetter level,List<net.minecraft.network.chat.Component> lines,net.minecraft.world.item.TooltipFlag flags){
        var output=net.minecraft.network.chat.Component.literal(java.math.BigDecimal.valueOf(EmtEnergyConfig.output(compression)).stripTrailingZeros().toPlainString());
        if(kind.equals("compressed")){
            lines.add(output.append(net.minecraft.network.chat.Component.translatable("tooltip.EMT.euPerTick")));
            return;
        }
        if(kind.equals("water")||kind.equals("dark")){
            lines.add(label("specialEffect",net.minecraft.ChatFormatting.GREEN).append(output.withStyle(net.minecraft.ChatFormatting.GRAY)).append(" ")
                .append(net.minecraft.network.chat.Component.translatable("tooltip.EMT.euPerTick").withStyle(net.minecraft.ChatFormatting.GRAY)).append(" ")
                .append(net.minecraft.network.chat.Component.translatable("tooltip.EMT."+(kind.equals("water")?"raining":"night")).withStyle(net.minecraft.ChatFormatting.GRAY)));
            return;
        }
        lines.add(label("normalConditions",net.minecraft.ChatFormatting.AQUA).append(output.withStyle(net.minecraft.ChatFormatting.GRAY)).append(" ")
            .append(net.minecraft.network.chat.Component.translatable("tooltip.EMT.euPerTick").withStyle(net.minecraft.ChatFormatting.GRAY)));
        if(kind.equals("order"))lines.add(effect("output.triple",null));
        if(kind.equals("fire")){
            // The original third-tier description advertises lava even though the
            // author's fire generator only actually runs in the Nether.
            if(compression==2)lines.add(effect("output.double","output.double.lava"));
            lines.add(effect("output.double","output.double.nether"));
        }
        if(kind.equals("air")||kind.equals("earth"))lines.add(effect("output.double.half","output.double.half."+(kind.equals("air")?"height":"low")));
    }
    protected static net.minecraft.network.chat.MutableComponent label(String key,net.minecraft.ChatFormatting color){return net.minecraft.network.chat.Component.translatable("tooltip.EMT."+key).withStyle(color).append(": ");}
    protected static net.minecraft.network.chat.Component effect(String amount,String condition){
        var line=label("specialEffect",net.minecraft.ChatFormatting.GREEN).append(net.minecraft.network.chat.Component.translatable("tooltip.EMT."+amount).withStyle(net.minecraft.ChatFormatting.GRAY));
        if(condition!=null)line.append(" ").append(net.minecraft.network.chat.Component.translatable("tooltip.EMT."+condition).withStyle(net.minecraft.ChatFormatting.GRAY));
        return line;
    }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){return level.isClientSide?null:createTickerHelper(type,ElectroMagicTools.SOLAR_BE.get(),SolarBlockEntity::tick);}
    @Override public Direction getFacing(Level level,BlockPos pos){return Direction.UP;}
    @Override public boolean setFacing(Level level,BlockPos pos,Direction face,Player player){return false;}
    @Override public boolean wrenchCanRemove(Level level,BlockPos pos,Player player){return true;}
    @Override public List<ItemStack> getWrenchDrops(Level level,BlockPos pos,BlockState state,BlockEntity blockEntity,Player player,int fortune){return List.of(new ItemStack(this));}
}
