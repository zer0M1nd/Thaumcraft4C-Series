package timodern;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;

public class EssentiaBlock extends BaseEntityBlock {
    public EssentiaBlock(){super(BlockBehaviour.Properties.copy(Blocks.STONE).strength(1.5f,10).lightLevel(s->15));}
    public RenderShape getRenderShape(BlockState state){return RenderShape.MODEL;}
    public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new Tile(pos,state);}
    public static ItemStack stack(Block block,String aspect){var stack=new ItemStack(block);var data=new CompoundTag();data.putString("aspect",aspect);stack.getOrCreateTag().put("BlockEntityTag",data);return stack;}
    public static String aspect(ItemStack stack){return stack.hasTag()?stack.getTag().getCompound("BlockEntityTag").getString("aspect"):"";}
    public static int color(String aspect){return tc4revival.aspect.TCAspects.catalog().find(aspect).map(tc4revival.aspect.Aspect::color).orElse(0xffffff);}
    public static class Tile extends BlockEntity {
        public String aspect="";
        public Tile(BlockPos p,BlockState s){super(ThaumicInfusion.ESSENTIA_TILE.get(),p,s);}
        public void load(CompoundTag t){super.load(t);aspect=t.getString("aspect");}
        protected void saveAdditional(CompoundTag t){super.saveAdditional(t);t.putString("aspect",aspect);}
        public CompoundTag getUpdateTag(){return saveWithoutMetadata();}
        public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
    }
    public ItemStack getCloneItemStack(net.minecraft.world.level.BlockGetter world,BlockPos p,BlockState s){return stack(this,world.getBlockEntity(p) instanceof Tile t?t.aspect:"");}
}
