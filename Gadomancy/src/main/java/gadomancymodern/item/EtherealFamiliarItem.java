package gadomancymodern.item;

import gadomancymodern.familiar.*;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import tc4revival.aspect.TCAspects;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;
import java.util.*;

public class EtherealFamiliarItem extends Item implements ICurioItem {
    public static final Rarity SACRED=Rarity.create("GADOMANCY_SACRED",ChatFormatting.GOLD);
    public EtherealFamiliarItem(){super(new Properties().stacksTo(1).rarity(SACRED));}
    public static String aspect(ItemStack stack){return stack.hasTag()?stack.getTag().getString("aspect"):"";}
    public static void aspect(ItemStack stack,String aspect){stack.getOrCreateTag().putString("aspect",aspect);}
    public static Map<String,Integer> augments(ItemStack stack){var result=new LinkedHashMap<String,Integer>();if(stack.hasTag()){var tag=stack.getTag().getCompound("augments");for(String id:FamiliarAugment.REGISTRY.keySet())if(tag.getInt(id)>0)result.put(id,tag.getInt(id));}return result;}
    public static void augment(ItemStack stack,String id,int level){var tag=stack.getOrCreateTag();var augments=tag.getCompound("augments");augments.putInt(id,level);tag.put("augments",augments);}
    public static List<ItemStack> creative(Item item){var result=new ArrayList<ItemStack>();String[][] rows={{"praecantatio"},{"ignis","fire","3","damage","3"},{"tempestas","shock","3","speed","2"},{"venenum","poison","3","speed","2"},{"telum","weakness","3","range","2"}};for(var row:rows){var stack=new ItemStack(item);aspect(stack,row[0]);for(int i=1;i<row.length;i+=2)augment(stack,row[i],Integer.parseInt(row[i+1]));result.add(stack);}return result;}
    @Override public boolean canEquip(SlotContext context,ItemStack stack){if(context.entity() instanceof net.minecraft.server.level.ServerPlayer player)return thaumcraft.api.ThaumcraftApiHelper.isResearchComplete(player,"gadomancy:ethereal_familiar");return context.entity().level().isClientSide&&tc4revival.client.ClientKnowledge.snapshot().map(k->k.researchMarkers().contains("gadomancy:ethereal_familiar")).orElse(false);}
    @Override public void curioTick(SlotContext context,ItemStack stack){if(context.entity() instanceof Player player&&!player.level().isClientSide&&TCAspects.catalog().find(aspect(stack)).isPresent())FamiliarController.tick(player,stack);}
    @Override public void onUnequip(SlotContext context,ItemStack replacement,ItemStack stack){if(context.entity() instanceof Player player)FamiliarController.remove(player);}
    @Override public void appendHoverText(ItemStack stack,Level level,List<Component> lines,TooltipFlag flags){
        if(net.minecraft.client.gui.screens.Screen.hasShiftDown()){
            TCAspects.catalog().find(aspect(stack)).ifPresent(a->lines.add(Component.literal(thaumcraft.api.aspects.AspectViews.display(a).displayName()).withStyle(ChatFormatting.GRAY)));
            augments(stack).forEach((id,n)->lines.add(Component.translatable("familiar.augment."+id+".name").append(" "+roman(n)).withStyle(ChatFormatting.GRAY)));
        }else if(!aspect(stack).isEmpty()||!augments(stack).isEmpty())lines.add(Component.translatable("gadomancy.lore.hasAdditionalLore").withStyle(ChatFormatting.DARK_GRAY,ChatFormatting.ITALIC));
    }
    public static String roman(int number){if(number>3999)return Integer.toString(number);StringBuilder out=new StringBuilder();int[] v={1000,900,500,400,100,90,50,40,10,9,5,4,1};String[] s={"M","CM","D","CD","C","XC","L","XL","X","IX","V","IV","I"};for(int i=0;i<v.length;i++)while(number>=v[i]){out.append(s[i]);number-=v[i];}return out.toString();}
    @Override public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer){consumer.accept(new gadomancymodern.client.FamiliarItemExtension());}
}
