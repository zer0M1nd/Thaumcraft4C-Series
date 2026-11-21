package automagy.content;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;
public final class ExperienceItemRules {
 public record Drain(int experience,ItemStack remainder){}
 public static Drain find(ItemStack stack){var id=ForgeRegistries.ITEMS.getKey(stack.getItem());if(id==null)return null;Drain found=null;for(String line:automagy.AutomagyConfig.AMNESIAC_ITEMS.get()){var parsed=automagy.logic.ExperienceItemRule.parse(line,ExperiencePhialItem.value());if(parsed.isEmpty()||!parsed.get().item().equals(id.toString()))continue;var rule=parsed.get();ItemStack rest=ItemStack.EMPTY;if(rule.craftingRemainder())rest=stack.copyWithCount(1).getCraftingRemainingItem();else if(!rule.remainder().isEmpty()){var key=ResourceLocation.tryParse(rule.remainder());if(key==null||!ForgeRegistries.ITEMS.containsKey(key))continue;rest=new ItemStack(ForgeRegistries.ITEMS.getValue(key));}found=new Drain(rule.experience(),rest);}return found;}
 private ExperienceItemRules(){}
}
