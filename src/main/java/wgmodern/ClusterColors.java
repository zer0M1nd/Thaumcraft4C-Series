package wgmodern;
import java.util.*;import net.minecraft.client.Minecraft;import net.minecraft.client.renderer.texture.TextureAtlasSprite;import net.minecraft.world.item.*;
/** Each metal is sampled once per resource/tag generation, never per render frame. */
public class ClusterColors {
 private static final Map<String,int[]> VALUES=new HashMap<>();
 public static void clear(){VALUES.clear();}
 public static int[] get(String metal){return VALUES.computeIfAbsent(metal,ClusterColors::sample);}
 private static int[] sample(String metal){var ore=pixels(BlastRecipe.tagged("forge:ores/"+metal,1));var ingot=pixels(BlastRecipe.tagged("forge:ingots/"+metal,1));int type=0,color=0xffffff;if(!ore.isEmpty()){int c=ore.get(0),r=c>>16&255,g=c>>8&255,b=c&255;type=r>b&&g>b?2:r>g&&r>b?1:0;}if(!ingot.isEmpty())color=vibrant(vibrant(ingot.get((int)(ingot.size()*.65))));return new int[]{color,type};}
 private static List<Integer> pixels(ItemStack item){var values=new ArrayList<Integer>();if(item.isEmpty())return values;var mc=Minecraft.getInstance();var model=mc.getItemRenderer().getModel(item,mc.level,mc.player,0);var sprites=new LinkedHashSet<TextureAtlasSprite>();var random=net.minecraft.util.RandomSource.create(0);for(var quad:model.getQuads(null,null,random))sprites.add(quad.getSprite());if(sprites.isEmpty())sprites.add(model.getParticleIcon());int tint=mc.getItemColors().getColor(item,0);if(tint==-1)tint=0xffffff;for(var sprite:sprites)for(int y=0;y<sprite.contents().height();y++)for(int x=0;x<sprite.contents().width();x++){int abgr=sprite.getPixelRGBA(0,x,y);if((abgr>>>24)==0)continue;int r=abgr&255,g=abgr>>8&255,b=abgr>>16&255;/* The author averages sprite and item tint, rather than multiplying channels. */int c=((r+(tint>>16&255))/2<<16)|((g+(tint>>8&255))/2<<8)|(b+(tint&255))/2;if(c>0)values.add(c);}return values;}
 public static int vibrant(int c){int r=c>>16&255,g=c>>8&255,b=c&255;return ((r+(r>g&&r>b?255:r<g&&r<b?136:187))/2<<16)|((g+(g>r&&g>b?255:g<r&&g<b?136:187))/2<<8)|(b+(b>r&&b>g?255:b<r&&b<g?136:187))/2;}
}
