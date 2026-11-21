package thaumicomniverse;

import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class JourneyMapApiIsolationTest {
    private static byte[] classBytes(String name) throws java.io.IOException {
        String path = "/" + name.replace('.', '/') + ".class";
        try (var stream = JourneyMapApiIsolationTest.class.getResourceAsStream(path)) {
            if (stream == null) throw new java.io.FileNotFoundException(path);
            return stream.readAllBytes();
        }
    }

    /** Simulate the runtime where the other API really cannot be loaded. */
    private static ClassLoader isolated(String blocked) {
        return new ClassLoader(JourneyMapApiIsolationTest.class.getClassLoader()) {
            @Override protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                if (name.startsWith(blocked)) throw new ClassNotFoundException("API absent in this profile: " + name);
                // Forge injects a no-arg event constructor at runtime; plain JUnit has no transformer.
                if(name.equals("journeymap.client.api.event.forge.FullscreenDisplayEvent") || name.equals("journeymap.client.api.event.forge.FullscreenDisplayEvent$AddonButtonDisplayEvent")) {
                    var loaded=findLoadedClass(name);if(loaded!=null)return loaded;
                    try(var input=getParent().getResourceAsStream(name.replace('.','/')+".class")) {
                        var reader=new org.objectweb.asm.ClassReader(input);
                        var writer=new org.objectweb.asm.ClassWriter(0);reader.accept(writer,0);
                        var ctor=writer.visitMethod(org.objectweb.asm.Opcodes.ACC_PUBLIC,"<init>","()V",null,null);
                        ctor.visitCode();ctor.visitVarInsn(org.objectweb.asm.Opcodes.ALOAD,0);
                        ctor.visitInsn(org.objectweb.asm.Opcodes.ACONST_NULL);ctor.visitInsn(org.objectweb.asm.Opcodes.ACONST_NULL);
                        ctor.visitMethodInsn(org.objectweb.asm.Opcodes.INVOKESPECIAL,name.replace('.','/'),"<init>","(Ljourneymap/client/api/model/IFullscreen;Ljourneymap/client/api/display/ThemeButtonDisplay;)V",false);
                        ctor.visitInsn(org.objectweb.asm.Opcodes.RETURN);ctor.visitMaxs(3,1);ctor.visitEnd();
                        byte[] bytes=writer.toByteArray();return defineClass(name,bytes,0,bytes.length);
                    } catch(java.io.IOException failure) {throw new ClassNotFoundException(name,failure);}
                }
                if (!name.startsWith("thaumicomniverse.client.")) return super.loadClass(name, resolve);
                synchronized (getClassLoadingLock(name)) {
                    var result = findLoadedClass(name);
                    if (result == null) try {
                        byte[] bytes = classBytes(name);
                        result = defineClass(name, bytes, 0, bytes.length);
                    } catch (java.io.IOException error) { throw new ClassNotFoundException(name, error); }
                    if (resolve) resolveClass(result);
                    return result;
                }
            }
        };
    }
    @Test void lifecycleDoesNotLinkEitherOptionalApi() throws Exception {
        var loader = isolated("journeymap.");
        var bridge = Class.forName("thaumicomniverse.client.NodeMapBridge", true, loader);
        bridge.getMethod("clear").invoke(null); // Logout before plugin initialization caused the crash.
        bridge.getMethod("refresh").invoke(null);
        byte[] eventClass = classBytes("thaumicomniverse.client.NodeMapClient");
        var constants = new String(eventClass, StandardCharsets.ISO_8859_1);
        assertFalse(constants.contains("journeymap/"));
        assertFalse(constants.contains("thaumicomniverse/client/JourneyMapNodes"));
    }
    private static void initialize(String plugin, String apiName, String blocked) throws Exception {
        var loader = isolated(blocked);
        var api = Class.forName(apiName, false, loader);
        var instance = Class.forName("thaumicomniverse.client." + plugin, true, loader).getConstructor().newInstance();
        var fakeApi = Proxy.newProxyInstance(api.getClassLoader(), new Class<?>[]{api}, (proxy, method, args) -> null);
        instance.getClass().getMethod("initialize", api).invoke(instance, fakeApi);
        assertEquals("thaumicomniverse", instance.getClass().getMethod("getModId").invoke(instance));
        Class.forName("thaumicomniverse.client.NodeMapBridge", true, loader).getMethod("clear").invoke(null);
    }
    @Test void actualMarkersHaveCenteredAnchors() throws Exception {
        for (var plugin : java.util.List.of("JourneyMapNodes", "JourneyMapNodesV2")) {
            var type=Class.forName("thaumicomniverse.client."+plugin);
            var marker=type.getMethod("createMarker",String.class,net.minecraft.core.BlockPos.class,net.minecraft.resources.ResourceLocation.class)
                    .invoke(null,"test",new net.minecraft.core.BlockPos(12,64,-9),net.minecraft.resources.ResourceLocation.parse("thaumcraft:textures/aspects/auram.png"));
            var icon=marker.getClass().getMethod("getIcon").invoke(marker);
            assertEquals(new net.minecraft.core.BlockPos(12,64,-9),marker.getClass().getMethod("getPoint").invoke(marker));
            // The old test passed because it exercised an unused helper. Guard the production call site too.
            var calls=new java.util.ArrayList<String>();
            var reader=new org.objectweb.asm.ClassReader(classBytes(type.getName()));
            reader.accept(new org.objectweb.asm.ClassVisitor(org.objectweb.asm.Opcodes.ASM9) {
                @Override public org.objectweb.asm.MethodVisitor visitMethod(int access,String name,String desc,String sig,String[] exceptions) {
                    if(!name.equals("refresh"))return null;
                    return new org.objectweb.asm.MethodVisitor(org.objectweb.asm.Opcodes.ASM9) {
                        @Override public void visitMethodInsn(int opcode,String owner,String name,String desc,boolean itf) {calls.add(owner+"."+name);}
                    };
                }
            },0);
            assertTrue(calls.contains("thaumicomniverse/client/"+plugin+".createMarker"));
            assertFalse(calls.stream().anyMatch(call->call.endsWith("MapImage.<init>")));
            assertEquals(8.0,((Number)icon.getClass().getMethod("getAnchorX").invoke(icon)).doubleValue());
            assertEquals(8.0,((Number)icon.getClass().getMethod("getAnchorY").invoke(icon)).doubleValue());
        }
    }
    @Test void fullscreenButtonsExistWithoutScannedNodesAndToggleBothWays(@org.junit.jupiter.api.io.TempDir Path temp) throws Exception {
        var config=com.electronwill.nightconfig.core.file.CommentedFileConfig.of(temp.resolve("nodemap.toml"));
        config.load();
        thaumicomniverse.compat.nodemap.NodeMapConfig.SPEC.setConfig(config);
        try {
            for(var plugin:java.util.List.of("JourneyMapNodes","JourneyMapNodesV2")) {
                boolean v2=plugin.endsWith("V2");
                String base=v2?"journeymap.api.v2.client.":"journeymap.client.api.";
                String ui=base+(v2?"fullscreen.":"display.");
                var display=Class.forName(ui+"ThemeButtonDisplay");
                var buttonType=Class.forName(ui+"IThemeButton");
                var actionType=Class.forName(ui+"IThemeButton$Action");
                var toggled=new java.util.concurrent.atomic.AtomicBoolean();
                var action=new java.util.concurrent.atomic.AtomicReference<Object>();
                var button=Proxy.newProxyInstance(buttonType.getClassLoader(),new Class<?>[]{buttonType},(proxy,method,args)->{
                    if(method.getName().equals("setToggled"))toggled.set((Boolean)args[0]);
                    return null;
                });
                var fake=Proxy.newProxyInstance(display.getClassLoader(),new Class<?>[]{display},(proxy,method,args)->{
                    assertEquals("addThemeToggleButton",method.getName());
                    assertEquals("omniverse.node_map.toggle",args[0]);
                    toggled.set((Boolean)args[2]);action.set(args[3]);return button;
                });
                var type=Class.forName("thaumicomniverse.client."+plugin);
                var instance=type.getConstructor().newInstance();
                thaumicomniverse.client.NodeMapBridge.install(()->{},()->{});
                for(boolean initiallyEnabled:new boolean[]{true,false}) {
                    thaumicomniverse.compat.nodemap.NodeMapConfig.SHOW.set(initiallyEnabled);
                    var add=type.getDeclaredMethod("addButtons",display);add.setAccessible(true);add.invoke(instance,fake);
                    assertEquals(initiallyEnabled,toggled.get());assertNotNull(action.get());
                    actionType.getMethod("doAction",buttonType).invoke(action.get(),button);
                    assertEquals(!initiallyEnabled,toggled.get());
                    assertEquals(!initiallyEnabled,thaumicomniverse.compat.nodemap.NodeMapConfig.SHOW.get());
                    try(var persisted=com.electronwill.nightconfig.core.file.CommentedFileConfig.of(temp.resolve("nodemap.toml"))) {
                        persisted.load();
                        assertEquals(initiallyEnabled?"FALSE":"TRUE",String.valueOf((Object)persisted.get("revival_features.node_map.show")));
                        assertFalse(persisted.contains("showScannedAuraNodes"),"UI saves the migrated key, not the retired boolean");
                    }
                }
            }
        } finally {config.close();thaumicomniverse.compat.nodemap.NodeMapConfig.SPEC.setConfig((com.electronwill.nightconfig.core.CommentedConfig)null);}
    }
    @Test void legacyPluginInitializesWithoutV2() throws Exception {
        initialize("JourneyMapNodes", "journeymap.client.api.IClientAPI", "journeymap.api.v2.");
    }
    @Test void v2PluginInitializesWithoutLegacyApi() throws Exception {
        initialize("JourneyMapNodesV2", "journeymap.api.v2.client.IClientAPI", "journeymap.client.api.");
    }
}
