package gadomancymodern.test;
import gadomancymodern.Gadomancy;
import gadomancymodern.aura.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.minecraftforge.gametest.*;

@GameTestHolder("gadomancy") @PrefixGameTestTemplate(false)
public class AuraCoreServerTests {
    @GameTest(templateNamespace="gadomancy",template="test_empty",timeoutTicks=540)
    public static void collectionSurvivesReloadAndDoesNotDestroyBlocks(GameTestHelper test){
        var center=test.absolutePos(new BlockPos(5,12,5));var level=test.getLevel();
        for(var pos:BlockPos.betweenClosed(center.offset(-4,-4,-4),center.offset(4,4,4)))level.setBlockAndUpdate(pos,Blocks.DIAMOND_BLOCK.defaultBlockState());
        level.setBlockAndUpdate(center,Blocks.AIR.defaultBlockState());
        var drop=new ItemEntity(level,center.getX()+.5,center.getY()+.25,center.getZ()+.5,new ItemStack(Gadomancy.AURA_CORES.get("blank").get()));
        var core=new AuraCoreEntity(Gadomancy.AURA_ENTITY.get(),level);core.activate(drop,center,AuraComposition.PRIMALS);core.setNoGravity(true);core.setDeltaMovement(Vec3.ZERO);level.addFreshEntity(core);
        var active=new java.util.concurrent.atomic.AtomicReference<>(core);
        test.runAtTickTime(300,()->{
            var before=active.get();var save=before.saveWithoutId(new CompoundTag());test.assertTrue(save.getInt("GatherBlocks")>109,"Collection has already sampled enough valid blocks");
            var restored=new AuraCoreEntity(Gadomancy.AURA_ENTITY.get(),level);restored.load(save);before.discard();level.addFreshEntity(restored);active.set(restored);
            test.assertTrue(restored.elapsed()==save.getInt("GatherElapsed"),"Elapsed counter persists exactly");test.assertTrue(restored.composition.total()==before.composition.total(),"Primal weights survive reload");
        });
        test.runAtTickTime(510,()->{
            test.assertTrue(!active.get().isAlive(),"Completed core entity removed");
            var items=level.getEntitiesOfClass(ItemEntity.class,new AABB(center).inflate(3),e->e.isAlive()&&e.getItem().getItem() instanceof gadomancymodern.item.AuraCoreItem);
            test.assertTrue(items.size()==1&&!items.get(0).getItem().is(Gadomancy.AURA_CORES.get("blank").get()),"Exactly one colored core is produced");
            int count=0;for(var pos:BlockPos.betweenClosed(center.offset(-4,-4,-4),center.offset(4,4,4)))if(level.getBlockState(pos).is(Blocks.DIAMOND_BLOCK))count++;
            test.assertTrue(count==728,"Sampling does not consume environmental blocks");test.succeed();
        });
    }
    @GameTest(templateNamespace="gadomancy",template="test_empty")
    public static void sparseFailureAndRandomizedInfusionBounds(GameTestHelper test){
        var level=test.getLevel();var at=Vec3.atCenterOf(test.absolutePos(new BlockPos(3,15,3)));var core=new AuraCoreEntity(Gadomancy.AURA_ENTITY.get(),level);core.setPos(at);core.setItem(new ItemStack(Gadomancy.AURA_CORES.get("blank").get()));level.addFreshEntity(core);core.finish();
        var items=level.getEntitiesOfClass(ItemEntity.class,new AABB(at,at).inflate(1),e->e.isAlive());test.assertTrue(items.size()==1&&items.get(0).getItem().is(Gadomancy.AURA_CORES.get("blank").get()),"Empty environment returns one uncharged core");
        var recipe=(AuraCoreInfusion)level.getRecipeManager().byKey(new ResourceLocation("gadomancy","aura_core")).orElseThrow();
        var preview=recipe.plan(ItemStack.EMPTY);for(int attempt=0;attempt<100;attempt++){
            var plan=recipe.planForCraft(ItemStack.EMPTY,level);for(var aspect:preview.aspects().aspects()){
                int cap=preview.aspects().amount(aspect)/2,cost=plan.aspects().amount(aspect);test.assertTrue(cost>=cap&&cost<2*cap,"Actual infusion cost retains legacy half-cap bounds");
            }
        }
        test.assertTrue(recipe.plan(ItemStack.EMPTY).aspects().total()==preview.aspects().total(),"Preview never mutates when sampling real crafts");
        var out=new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());Gadomancy.AURA_RECIPE.get().toNetwork(out,recipe);var received=Gadomancy.AURA_RECIPE.get().fromNetwork(recipe.getId(),out);test.assertTrue(received instanceof AuraCoreInfusion,"Custom recipe survives recipe sync");out.release();test.succeed();
    }
}
