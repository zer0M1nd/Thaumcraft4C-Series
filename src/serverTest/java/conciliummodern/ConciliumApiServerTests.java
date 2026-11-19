package conciliummodern;

import com.ilya3point999k.thaumicconcilium.api.ThaumicConciliumApi;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraftforge.gametest.*;
import thaumcraft.api.aspects.*;

@GameTestHolder("thaumicconcilium") @PrefixGameTestTemplate(false)
public final class ConciliumApiServerTests {
    @GameTest(template="empty") public static void programmaticRecipesJoinRealReloadAndNetworkSerialization(GameTestHelper h){
        var found=h.getLevel().getRecipeManager().byKey(ResourceLocation.parse("thaumicconcilium:validation/api_reload")).orElse(null);h.assertTrue(found instanceof ChainedRiftRecipe,"API mixin injects into real initial resource reload");var recipe=(ChainedRiftRecipe)found;
        var output=recipe.getResultItem(h.getLevel().registryAccess());h.assertTrue(output.is(Items.ECHO_SHARD)&&output.getCount()==2&&output.getTag().getString("concilium_test").equals("output"),"Output count and NBT survive normal parser");
        var buffer=new FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());try{
            @SuppressWarnings("unchecked") var serializer=(net.minecraft.world.item.crafting.RecipeSerializer<ChainedRiftRecipe>)recipe.getSerializer();serializer.toNetwork(buffer,recipe);var decoded=serializer.fromNetwork(recipe.getId(),buffer);
            h.assertTrue(ItemStack.matches(output,decoded.getResultItem(h.getLevel().registryAccess()))&&decoded.aspectCosts().equals(recipe.aspectCosts()),"Normal recipe networking preserves full result and essentia");
        }finally{buffer.release();}
        var exact=ThaumicConciliumApi.getRiftRecipe(new ItemStack(Items.EMERALD),new AspectList().add(Aspect.AIR,17));h.assertTrue(exact!=null&&exact.getRecipeOutput().is(Items.ECHO_SHARD),"Public API queries effective server recipes");
        h.assertTrue(ThaumicConciliumApi.getRiftRecipe(new ItemStack(Items.EMERALD),new AspectList().add(Aspect.AIR,16))==null,"Insufficient essentia does not match");
        var polish=PolishmentRecipe.find(h.getLevel(),new ItemStack(Items.DIORITE));h.assertTrue(polish.isPresent()&&polish.get().amount()==31&&polish.get().aspect().equals("terra"),"Polishment API joins the same real reload");h.succeed();
    }
    @GameTest(template="empty") public static void publicRecipeOwnsMutableInputsAndMatchesNbtAlternatives(GameTestHelper h){
        var tagged=new ItemStack(Items.PAPER);tagged.getOrCreateTag().putString("seal","expected");var output=new ItemStack(Items.DIAMOND,3);output.getOrCreateTag().putString("mark","original");var costs=new AspectList().add(Aspect.AIR,10);
        var recipe=new com.ilya3point999k.thaumicconcilium.api.ChainedRiftRecipe("",output,java.util.List.of(tagged,new ItemStack(Items.STICK)),costs);
        tagged.getTag().putString("seal","mutated");output.setCount(1);costs.add(Aspect.AIR,20);
        var exact=new ItemStack(Items.PAPER);exact.getOrCreateTag().putString("seal","expected");var enough=new AspectList().add(Aspect.AIR,10);
        h.assertTrue(recipe.matches(enough,exact)&&recipe.matches(enough,new ItemStack(Items.STICK)),"Alternative ingredients retain their distinct matching rules");
        h.assertTrue(!recipe.matches(enough,new ItemStack(Items.PAPER))&&!recipe.matches(enough,tagged),"NBT catalyst cannot be substituted with unsealed or altered paper");
        var result=recipe.getRecipeOutput();h.assertTrue(result.getCount()==3&&result.getTag().getString("mark").equals("original"),"Construction owns output and costs");result.getTag().putString("mark","changed");h.assertTrue(recipe.getRecipeOutput().getTag().getString("mark").equals("original"),"Result access cannot alter registration");
        var pool=new AspectList().add(Aspect.AIR,12).add(Aspect.EARTH,5);var remainder=recipe.removeMatching(pool);h.assertTrue(pool.getAmount(Aspect.AIR)==12&&remainder.getAmount(Aspect.AIR)==2&&remainder.getAmount(Aspect.EARTH)==5,"Payment copy preserves caller and unrelated essentia");h.succeed();
    }
}
