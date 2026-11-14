package advancedthaumaturgy;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import tc4revival.content.*;
import tc4revival.aspect.*;
public class CreativeNodeEntity extends AuraNodeBlockEntity {
    public CreativeNodeEntity(BlockPos p,BlockState s){super(AdvancedThaumaturgy.CREATIVE_ENTITY.get(),p,s);var aspects=new AspectAmounts();TCAspects.catalog().primals().forEach(a->aspects.add(a,100));configure(aspects,AuraNodeType.PURE,AuraNodeModifier.BRIGHT,"advancedthaumaturgy-creative",false);}
    @Override public boolean take(Aspect a,int amount){return ATConfig.enabled("creative_node")&&amount>=0&&aspects().amount(a)>=amount;}
    @Override public void tick(){}
}
