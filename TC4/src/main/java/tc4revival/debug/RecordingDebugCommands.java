package tc4revival.debug;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.GameRules;

/** Uses Minecraft's command feedback rule without filtering ordinary chat or game notifications. */
public class RecordingDebugCommands {
    public static LiteralArgumentBuilder<CommandSourceStack> tree() {
        return Commands.literal("recording")
            .executes(context -> set(context.getSource(), true))
            .then(Commands.literal("on").executes(context -> set(context.getSource(), true)))
            .then(Commands.literal("off").executes(context -> set(context.getSource(), false)))
            .then(Commands.literal("status").executes(context -> {
                boolean enabled = !context.getSource().getServer().getGameRules().getBoolean(GameRules.RULE_SENDCOMMANDFEEDBACK);
                context.getSource().sendSystemMessage(Component.literal(enabled
                    ? "录制模式：已开启，当前存档/服务器的命令成功反馈已隐藏。"
                    : "录制模式：已关闭，命令成功反馈正常显示。"));
                return enabled ? 1 : 0;
            }));
    }

    private static int set(CommandSourceStack source, boolean recording) {
        source.getServer().getGameRules().getRule(GameRules.RULE_SENDCOMMANDFEEDBACK).set(!recording, source.getServer());
        // The explicit mode confirmation must still work after success feedback is disabled.
        source.sendSystemMessage(Component.literal(recording
            ? "录制模式已开启：隐藏当前存档/服务器的命令成功反馈；聊天、游戏提示及命令错误仍显示。"
            : "录制模式已关闭：恢复命令成功反馈。"));
        return 1;
    }

    /** Console regression uses a real ServerPlayer feedback policy and captures only its message sink. */
    public static int verify(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        var server = source.getServer();
        var rule = server.getGameRules().getRule(GameRules.RULE_SENDCOMMANDFEEDBACK);
        boolean previous = rule.get();
        boolean logAdmin = server.getGameRules().getBoolean(GameRules.RULE_LOGADMINCOMMANDS);
        var messages = new java.util.ArrayList<Component>();
        var player = new net.minecraft.server.level.ServerPlayer(server, source.getLevel(),
            new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "TC4RecordingCheck")) {
            @Override public void sendSystemMessage(Component text) { messages.add(text); }
            @Override public void sendSystemMessage(Component text, boolean overlay) { messages.add(text); }
        };
        var playerSource = player.createCommandSourceStack().withPermission(2);
        var dispatcher = server.getCommands().getDispatcher();
        try {
            dispatcher.execute("tcdebug recording", playerSource);
            if (rule.get() || player.acceptsSuccess()) throw new IllegalStateException("Recording mode still accepts command success feedback");
            messages.clear();
            playerSource.sendSuccess(() -> Component.translatable("commands.gamemode.success.self", Component.literal("Creative Mode")), false);
            if (!messages.isEmpty()) throw new IllegalStateException("Game mode success feedback remained visible");
            player.sendSystemMessage(Component.literal("Research unlocked"));
            playerSource.sendFailure(Component.literal("Command syntax error"));
            if (messages.size() != 2) throw new IllegalStateException("Ordinary game messages or command errors were hidden");
            dispatcher.execute("tcdebug recording off", playerSource);
            messages.clear();
            playerSource.sendSuccess(() -> Component.literal("Restored command feedback"), false);
            if (!rule.get() || messages.size() != 1) throw new IllegalStateException("Command feedback did not resume");
            dispatcher.execute("tcdebug recording on", playerSource);
            if (dispatcher.execute("tcdebug recording status", playerSource) != 1) throw new IllegalStateException("Recording status is incorrect");
            boolean denied = false;
            try { dispatcher.execute("tcdebug recording off", playerSource.withPermission(1)); }
            catch (com.mojang.brigadier.exceptions.CommandSyntaxException expected) { denied = true; }
            if (!denied || rule.get()) throw new IllegalStateException("Non-admin can change recording mode");
            if (server.getGameRules().getBoolean(GameRules.RULE_LOGADMINCOMMANDS) != logAdmin)
                throw new IllegalStateException("Recording mode changed server command logging");
        } finally { rule.set(previous, server); }
        source.sendSystemMessage(Component.literal("RECORDING_CHECK PASS: admin permission, on/off/status, hidden success, visible game messages/errors, unchanged server logging, previous rule restored"));
        return 1;
    }
}
