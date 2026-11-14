package advancedthaumaturgy;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import tc4revival.content.*;
public class EtherealJarBlock extends EssentiaJarBlock {
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new EtherealJarEntity(p,s);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> type){return l.isClientSide?null:createTickerHelper(type,AdvancedThaumaturgy.JAR_ENTITY.get(),(world,p,st,be)->be.tick());}
    @Override public int getAnalogOutputSignal(BlockState s,Level l,BlockPos p){return l.getBlockEntity(p) instanceof EtherealJarEntity jar?jar.amount()*14/256+(jar.amount()>0?1:0):0;}
    @Override public List<ItemStack> getDrops(BlockState s,LootParams.Builder params){var be=params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);return List.of(be instanceof EtherealJarEntity jar?jar.asItem():new ItemStack(this));}
    public static class JarItem extends BlockItem {
        public JarItem(EtherealJarBlock block){super(block,new Item.Properties().stacksTo(4));}
        @Override public int getMaxStackSize(ItemStack stack){return stack.hasTag()?1:4;}
        @Override public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer){
            consumer.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions(){
                @Override public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer(){return advancedthaumaturgy.client.ATBlockRenderer.items();}
            });
        }
        @Override public void appendHoverText(ItemStack s,Level l,List<net.minecraft.network.chat.Component> lines,TooltipFlag f){if(s.hasTag()){var aspects=tc4revival.aspect.AspectNbt.read(s.getTag(),tc4revival.aspect.TCAspects.catalog()).amounts();for(var a:aspects.aspects())lines.add(net.minecraft.network.chat.Component.literal(a.tag()+": "+aspects.amount(a)+" / 256"));}}
    }
}
