package tc4revival.debug;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import tc4revival.content.EldritchMazeSavedData;
import tc4revival.content.TCEldritch;

public class OuterMazeDebugCommands {
    public static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> tree(boolean boss) {
        return Commands.literal(boss?"outer_boss":"outer_tablet").executes(context->locate(context.getSource(),boss));
    }
    public static java.util.Optional<BlockPos> locate(EldritchMazeSavedData.Plan plan,boolean boss) {
        for(int z=0;z<plan.layout().height();z++)for(int x=0;x<plan.layout().width();x++)
            if(plan.layout().cell(x,z).feature()==(boss?2:6))return java.util.Optional.of(new BlockPos(
                    (plan.originX()+x)*16+(boss?16:8),boss?52:53,(plan.originZ()+z)*16+(boss?16:8)));
        return java.util.Optional.empty();
    }
    public static int locate(CommandSourceStack source,boolean boss) {
        if(!source.getLevel().dimension().equals(TCEldritch.OUTER_LANDS)) {
            source.sendFailure(Component.literal("此指令只能在外域使用。"));return 0;
        }
        var position=BlockPos.containing(source.getPosition());int cx=position.getX()>>4,cz=position.getZ()>>4;
        var plan=EldritchMazeSavedData.get(source.getServer()).plans().stream().filter(p->cx>=p.originX()&&cz>=p.originZ()
                &&cx<p.originX()+p.layout().width()&&cz<p.originZ()+p.layout().height()).findFirst();
        var destination=plan.flatMap(p->locate(p,boss));
        if(destination.isEmpty()) { source.sendFailure(Component.literal("当前位置没有可查询的外域迷宫记录。"));return 0; }
        var pos=destination.get();String command="/tp @s "+(pos.getX()+.5)+" "+pos.getY()+" "+(pos.getZ()+.5);
        var coords=Component.literal("["+pos.getX()+", "+pos.getY()+", "+pos.getZ()+"]").withStyle(style->style
                .withColor(net.minecraft.ChatFormatting.AQUA).withUnderlined(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND,command))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,Component.literal("点击填写传送指令"))));
        source.sendSuccess(()->Component.literal(boss?"当前迷宫的 Boss 房间：":"当前迷宫的石板房间（初始石板位置）：").append(coords),false);
        return 1;
    }
}
