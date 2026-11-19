package tbmodern.acceptance;
import net.minecraft.core.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.*;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraftforge.registries.ForgeRegistries;
import tc4revival.content.*;
import tbmodern.*;
/** Isolated test body: the base-only acceptance class must not verify optional types. */
public class AutomagyAcceptance {
 private static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
 public static void run(ServerLevel level,BlockPos origin,long originalTime){

                level.setBlock(origin,Content.block("overchanter").defaultBlockState(),3);var tile=(Overchanter)level.getBlockEntity(origin);
                var jarPos=origin.east(2);level.setBlock(jarPos,ForgeRegistries.BLOCKS.getValue(new ResourceLocation("automagy","experience_jar")).defaultBlockState(),3);
                var jar=(automagy.content.ExperienceJarBlockEntity)level.getBlockEntity(jarPos);jar.insertExperience(900,false);
                level.setBlock(origin.west(2),TCArcane.ESSENTIA_JAR.get().defaultBlockState(),3);
                var magic=(EssentiaJarBlockEntity)level.getBlockEntity(origin.west(2));magic.addToContainer(tc4revival.aspect.TCAspects.catalog().require("praecantatio"),64);
                var sword=new ItemStack(Items.DIAMOND_SWORD);sword.enchant(net.minecraft.world.item.enchantment.Enchantments.SHARPNESS,5);tile.setItem(0,sword);require(tile.start(),"Overchanter refused input");
                for(int i=0;i<800&&tile.active();i++){((ServerLevelData)level.getLevelData()).setGameTime(originalTime+i+1);Overchanter.tick(level,origin,level.getBlockState(origin),tile);}
                require(!tile.active()&&net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(net.minecraft.world.item.enchantment.Enchantments.SHARPNESS,tile.getItem(0))==6,"Real machine did not finish");
                require(jar.experience()==75,"Jar did not pay exactly 825 XP");
                require(tbmodern.api.ExperienceSources.extract(jar,100)==75&&level.getBlockState(jarPos).is(TCArcane.ESSENTIA_JAR.get()),"Empty jar must revert without duplicated XP");
                level.removeBlock(jarPos,false);level.removeBlock(origin.west(2),false);

 }
}
