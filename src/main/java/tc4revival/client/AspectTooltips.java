package tc4revival.client;

import com.mojang.datafixers.util.Either;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tc4revival.aspect.*;
import tc4revival.content.*;

/** Shared native tooltip component. Each row wraps inside the available tooltip width. */
@Mod.EventBusSubscriber(modid="thaumcraft",value=Dist.CLIENT)
public  class AspectTooltips {
    public record Cell(String tag,String amount,int amountColor,boolean blink) {
        public Cell(String tag,String amount,int amountColor) { this(tag,amount,amountColor,false); }
        public Cell(String tag,String amount) { this(tag,amount,0xffffffff); }
    }
    public record Row(Component caption,List<Cell> cells) { public Row { cells=List.copyOf(cells); } }
    public record Data(List<Row> rows,int width) implements TooltipComponent { public Data { rows=List.copyOf(rows); } }
    private AspectTooltips() {}
    public static boolean enabled() { return tc4revival.config.TCConfig.CLIENT.aspectIconTooltips.get() && !Screen.hasShiftDown(); }
    private static String number(double amount) { return java.math.BigDecimal.valueOf(amount).setScale(2,java.math.RoundingMode.HALF_UP).stripTrailingZeros().toPlainString(); }
    private static List<Cell> stored(AspectAmounts aspects) {
        var known=ClientKnowledge.displaySnapshot();var cells=new ArrayList<Cell>();
        for(var aspect:aspects.alphabetical())cells.add(known.map(k->k.knows(aspect)).orElse(false)?new Cell(aspect.tag(),aspects.amount(aspect)>0?number(aspects.amount(aspect)):""):new Cell("", ""));
        return cells;
    }
    @SubscribeEvent public static void gather(RenderTooltipEvent.GatherComponents event) {
        if(!enabled())return;
        var stack=event.getItemStack();var rows=new ArrayList<Row>();
        if(stack.getItem() instanceof StoredAspectItem stored) {
            var amounts=stack.getItem() instanceof NodeJarItem?NodeJarData.read(stack.hasTag()?stack.getTag():new net.minecraft.nbt.CompoundTag()).aspects():stored.storedAspects(stack);
            if(!amounts.isEmpty())rows.add(new Row(Component.empty(),stored(amounts)));
            if(stack.getItem() instanceof EssentiaJarItem && stack.hasTag())TCAspects.catalog().find(stack.getTag().getString("AspectFilter")).ifPresent(filter->{
                if(amounts.amount(filter)==0)rows.add(new Row(Component.translatable("thaumcraft.tooltip.filter"),List.of(new Cell(filter.tag(),""))));
            });
        }
        if(stack.getItem() instanceof CastingWandItem wand) {
            var values=new ArrayList<Cell>();
            for(var aspect:thaumcraft.api.client.ClientAspectViews.primals())values.add(new Cell(aspect.tag(),number(wand.vis(stack,aspect)/100d)));
            rows.add(new Row(Component.empty(),values));
            var focus=WandFocusItem.attached(stack);
            if(focus.getItem() instanceof WandFocusItem item)rows.add(costs(item,focus,stack));
        }else if(stack.getItem() instanceof WandFocusItem focus)rows.add(costs(focus,stack,ItemStack.EMPTY));
        else if(stack.getItem() instanceof VisAmuletItem amulet) {
            var values=new ArrayList<Cell>();for(var aspect:thaumcraft.api.client.ClientAspectViews.primals())values.add(new Cell(aspect.tag(),number(amulet.vis(stack,aspect)/100d)));
            rows.add(new Row(Component.translatable("thaumcraft.tooltip.stored_vis"),values));
        }else if(stack.getItem() instanceof HoverHarnessItem && stack.hasTag()) {
            var jar=ItemStack.of(stack.getTag().getCompound("jar"));
            if(jar.getItem() instanceof StoredAspectItem stored && !stored.storedAspects(jar).isEmpty())rows.add(new Row(Component.empty(),stored(stored.storedAspects(jar))));
        }
        rows.removeIf(row->row.cells().isEmpty());if(rows.isEmpty())return;
        int available=Math.max(24,event.getScreenWidth()-24);
        if(event.getMaxWidth()>0)available=Math.min(available,event.getMaxWidth());
        // Leave room beside the pointer; vanilla remains responsible for the final tooltip position.
        int width=Math.min(available,Math.max(24,event.getScreenWidth()/2));
        event.getTooltipElements().add(Math.min(1,event.getTooltipElements().size()),Either.right(new Data(rows,width)));
    }
    private static Row costs(WandFocusItem focus,ItemStack stack,ItemStack wand) {
        var cells=new ArrayList<Cell>();var player=Minecraft.getInstance().player;
        focus.costs(stack).entrySet().stream().sorted(Map.Entry.comparingByKey(Comparator.comparing(Aspect::tag))).forEach(entry->{
            double factor=wand.getItem() instanceof CastingWandItem item?item.consumptionModifier(wand,player,entry.getKey(),false):1;
            cells.add(new Cell(entry.getKey().tag(),number(entry.getValue()*factor/100d)));
        });
        return new Row(Component.translatable(focus.costPerTick(stack)?"item.Focus.cost2":"item.Focus.cost1"),cells);
    }
    @Mod.EventBusSubscriber(modid="thaumcraft",value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
    public static  class Registration {
        @SubscribeEvent public static void register(RegisterClientTooltipComponentFactoriesEvent event) { event.register(Data.class,Renderer::new); }
    }
    private static  class Renderer implements ClientTooltipComponent {
        record Placed(Cell cell,int x,int y,int width) {}
        record Caption(List<net.minecraft.util.FormattedCharSequence> lines,int y) {}
        final List<Placed> cells=new ArrayList<>();final List<Caption> captions=new ArrayList<>();int width,height;
        Renderer(Data data) {
            Font font=Minecraft.getInstance().font;int y=0;
            for(var row:data.rows()) {
                if(!row.caption().getString().isEmpty()) {
                    var lines=font.split(row.caption(),data.width());
                    captions.add(new Caption(lines,y));
                    y+=lines.size()*font.lineHeight;
                    width=Math.max(width,Math.min(font.width(row.caption()),data.width()));
                }
                int x=0;
                for(var cell:row.cells()) {
                    int w=cellWidth(font,cell);
                    if(x>0 && x+w>data.width()){y+=18;x=0;}
                    cells.add(new Placed(cell,x,y+1,w));x+=w;width=Math.max(width,x);
                }
                y+=18;
            }
            height=y;
        }
        @Override public int getHeight(){return height;}
        @Override public int getWidth(Font font){return width;}
        @Override public void renderImage(Font font,int x,int y,GuiGraphics graphics) {
            for(var caption:captions) {
                var lines=caption.lines();for(int i=0;i<lines.size();i++)graphics.drawString(font,lines.get(i),x,y+caption.y()+i*font.lineHeight,0xffaaaaaa,false);
            }
            for(var placed:cells)draw(graphics,font,placed.cell(),x+placed.x(),y+placed.y());
        }
    }
    public static int cellWidth(Font font,Cell cell) { return Math.max(22,(font.width(cell.amount())+1)/2+9); }
    public static void draw(GuiGraphics g,Font font,Cell cell,int x,int y) {
        if(!cell.tag().isEmpty() && !thaumcraft.api.client.ClientAspectViews.visible(cell.tag()))cell=new Cell("","");
        var aspect=TCAspects.catalog().find(cell.tag()).orElse(null);int color=aspect==null?0xdddde8:aspect.color();
        String path=aspect==null?(cell.tag().equals(tc4revival.research.ScanIdentity.EMPTY_ICON)?"_empty":"_unknown"):aspect.tag();
        // Text is buffered, whereas the tinted icon is immediate. Flush before changing tint.
        g.flush();
        float alpha=cell.blink() ? (float)(.6+.4*Math.cos(net.minecraft.Util.getMillis()*Math.PI/300.0)) : 1;
        if(cell.blink()) com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        g.setColor((color>>16&255)/255f,(color>>8&255)/255f,(color&255)/255f,alpha);
        LegacyGui.blit(g,path.startsWith("_")?ResourceLocation.parse("thaumcraft:textures/aspects/"+path+".png"):thaumcraft.api.client.ClientAspectViews.texture(path),x,y,0,0,16,16,16,16);g.setColor(1,1,1,1);
        if(!cell.amount().isEmpty()) {
            g.pose().pushPose();g.pose().translate(x+cellWidth(font,cell)-4,y+16,0);g.pose().scale(.5f,.5f,1);
            g.drawString(font,cell.amount(),-font.width(cell.amount()),-font.lineHeight,cell.amountColor(),true);g.pose().popPose();
        }
    }
}
