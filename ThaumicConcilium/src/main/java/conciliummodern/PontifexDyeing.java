package conciliummodern;

import java.util.*;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.*;

/** The original sheep palette, mandatory base red and integer brightness averaging. */
public final class PontifexDyeing {
    private static final float[][] COLORS={{1,1,1},{.85f,.5f,.2f},{.7f,.3f,.85f},{.4f,.6f,.85f},{.9f,.9f,.2f},{.5f,.8f,.1f},{.95f,.5f,.65f},{.3f,.3f,.3f},{.6f,.6f,.6f},{.3f,.5f,.6f},{.5f,.25f,.7f},{.2f,.3f,.7f},{.4f,.3f,.2f},{.4f,.5f,.2f},{.6f,.2f,.2f},{.1f,.1f,.1f}};
    public static void setup(){
        thaumcraft.api.crafting.RobeDyeing.registerRobe(ThaumicConcilium.ID+":pontifex",0,stack->stack.getItem() instanceof PontifexRobe,
            new thaumcraft.api.crafting.RobeDyeing.RobeDyeAdapter(){
                public int color(ItemStack robe){return ((PontifexRobe)robe.getItem()).getColor(robe);}
                public Integer dyeColor(ItemStack stack){var dye=dye(stack);if(dye==null)return null;var c=COLORS[dye.getId()];return (int)(c[0]*255)<<16|(int)(c[1]*255)<<8|(int)(c[2]*255);}
                public int blend(List<Integer> colors){return PontifexDyeing.blend(colors);}
                public ItemStack withColor(ItemStack robe,int color){((PontifexRobe)robe.getItem()).setColor(robe,color);return robe;}
            });
    }
    public static boolean contains(CraftingContainer grid){for(int i=0;i<grid.getContainerSize();i++)if(grid.getItem(i).getItem() instanceof PontifexRobe)return true;return false;}
    private static DyeColor dye(ItemStack stack){
        if(stack.is(net.minecraftforge.common.Tags.Items.DYES_BLACK))return DyeColor.BLACK;
        if(stack.getItem() instanceof DyeItem item)return item.getDyeColor();
        if(stack.is(Items.BONE_MEAL))return DyeColor.WHITE;if(stack.is(Items.INK_SAC))return DyeColor.BLACK;
        if(stack.is(Items.LAPIS_LAZULI))return DyeColor.BLUE;if(stack.is(Items.COCOA_BEANS))return DyeColor.BROWN;return null;
    }
    public static ItemStack assemble(CraftingContainer grid){
        ItemStack result=ItemStack.EMPTY;var colors=new ArrayList<Integer>();int dyes=0;
        for(int i=0;i<grid.getContainerSize();i++){
            var stack=grid.getItem(i);if(stack.isEmpty())continue;
            if(stack.getItem() instanceof PontifexRobe robe){if(!result.isEmpty())return ItemStack.EMPTY;result=stack.copyWithCount(1);colors.add(robe.getColor(stack));}
            else{var dye=dye(stack);if(dye==null)return ItemStack.EMPTY;var rgb=COLORS[dye.getId()];colors.add((int)(rgb[0]*255)<<16|(int)(rgb[1]*255)<<8|(int)(rgb[2]*255));dyes++;}
        }
        if(result.isEmpty()||dyes==0)return ItemStack.EMPTY;((PontifexRobe)result.getItem()).setColor(result,blend(colors));return result;
    }
    public static int blend(List<Integer> colors){
        if(colors.isEmpty())throw new IllegalArgumentException("No colors");long r=0,g=0,b=0,bright=0;
        for(int color:colors){int cr=color>>16&255,cg=color>>8&255,cb=color&255;r+=cr;g+=cg;b+=cb;bright+=Math.max(cr,Math.max(cg,cb));}
        r/=colors.size();g/=colors.size();b/=colors.size();bright/=colors.size();long peak=Math.max(r,Math.max(g,b));if(peak==0)return 0;
        return (int)(r*(float)bright/peak)<<16|(int)(g*(float)bright/peak)<<8|(int)(b*(float)bright/peak);
    }
}
