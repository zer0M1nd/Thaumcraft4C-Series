package dev.tc4.updaterui;

import com.google.gson.JsonParser;

import dev.tc4.download.ui.DownloadScreen;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

@Mod.EventBusSubscriber(modid = "thaumcraftupdater", value = Dist.CLIENT)
public final class UpdateScreen {
    private static boolean opened;

    @SubscribeEvent
    public static void opening(ScreenEvent.Opening event) throws IOException {
        if (!Boolean.getBoolean("tc4.downloadWindow")
                || opened
                || !(event.getNewScreen() instanceof net.minecraft.client.gui.screens.TitleScreen
                        || event.getNewScreen()
                                instanceof
                                net.minecraft.client.gui.screens.AccessibilityOnboardingScreen))
            return;
        opened = true;
        var data = Files.readAllLines(FMLPaths.GAMEDIR.get().resolve("connection"));
        var socket = new Socket("127.0.0.1", Integer.parseInt(data.get(0)));
        socket.setTcpNoDelay(true);
        var input = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
        var output = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
        output.writeUTF(data.get(1));
        output.flush();
        var mc = Minecraft.getInstance();
        var parent = ProcessHandle.of(Long.parseLong(data.get(2))).orElseThrow();
        parent.onExit().thenRun(() -> mc.execute(mc::stop));
        event.setNewScreen(
                new DownloadScreen(
                        new DownloadScreen.Controller() {
                            private String current = request("snapshot", "");

                            private String request(String action, String id) {
                                try {
                                    output.writeUTF(action);
                                    output.writeUTF(id);
                                    output.flush();
                                    int size = input.readInt();
                                    if (size < 0 || size > 4 * 1024 * 1024)
                                        throw new IOException("Invalid download state length");
                                    byte[] bytes = new byte[size];
                                    input.readFully(bytes);
                                    return new String(bytes, StandardCharsets.UTF_8);
                                } catch (IOException e) {
                                    throw new UncheckedIOException(
                                            "Lost startup download controller", e);
                                }
                            }

                            public String snapshot() {
                                return current = request("snapshot", "");
                            }

                            public void action(String action, String id) {
                                current = request(action, id);
                            }

                            public boolean proceed() {
                                boolean confirm =
                                        JsonParser.parseString(current)
                                                .getAsJsonObject()
                                                .get("confirm")
                                                .getAsBoolean();
                                java.util.concurrent.CompletableFuture.supplyAsync(
                                                () ->
                                                        request(
                                                                confirm ? "confirmed" : "continue",
                                                                ""))
                                        .whenComplete(
                                                (reply, error) ->
                                                        mc.execute(
                                                                () -> {
                                                                    if (error != null
                                                                            || "finished"
                                                                                    .equals(reply))
                                                                        mc.stop();
                                                                    else {
                                                                        current = reply;
                                                                        mc.setScreen(
                                                                                new DownloadScreen(
                                                                                        this));
                                                                    }
                                                                }));
                                return true;
                            }
                        }));
    }
}
