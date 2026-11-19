package tc4validation;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.lang.reflect.Method;
import java.util.Set;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;

/** Isolated acceptance harness only; excluded from every production source set. */
@Mod("tc4_validation_harness")
@Mod.EventBusSubscriber(modid="tc4_validation_harness")
public final class ValidationHarness {
    private static Object ticker;
    private static java.lang.reflect.Field activeTests, listeners;
    private static Method testTick, testDone, testFail, notifyFailure;
    private static Object annotatedTest(Class<?> owner, Method method, org.objectweb.asm.tree.AnnotationNode annotation) throws Exception {
        java.util.Map<String,Object> values = new java.util.HashMap<>();
        if (annotation.values != null)
            for (int i=0;i<annotation.values.size();i+=2) values.put((String)annotation.values.get(i), annotation.values.get(i+1));
        // ForgeGradle reobfuscates method calls but leaves annotation element names in Mojmap.
        // Read the original bytecode so production reflection cannot silently use default timeouts/templates.
        boolean prefix = (boolean)Class.forName("net.minecraftforge.gametest.ForgeGameTestHooks")
            .getMethod("prefixGameTestTemplate",Method.class).invoke(null,method);
        String name = (prefix ? owner.getSimpleName().toLowerCase(java.util.Locale.ROOT)+"." : "") + method.getName().toLowerCase(java.util.Locale.ROOT);
        String namespace = (String)Class.forName("net.minecraftforge.gametest.ForgeGameTestHooks")
            .getMethod("getTemplateNamespace",Method.class).invoke(null,method);
        String template = (String)values.getOrDefault("template", "");
        String structure = namespace+":"+(template.isEmpty()?name:(prefix?owner.getSimpleName().toLowerCase(java.util.Locale.ROOT)+".":"")+template);
        Class<?> rotationType=Class.forName("net.minecraft.world.level.block.Rotation");
        Object rotation=Class.forName("net.minecraft.gametest.framework.StructureUtils").getMethod("m_127835_",int.class).invoke(null,values.getOrDefault("rotationSteps",0));
        java.util.function.Consumer<Object> action=helper->{
            try { method.invoke(java.lang.reflect.Modifier.isStatic(method.getModifiers())?null:owner.getDeclaredConstructor().newInstance(),helper); }
            catch(java.lang.reflect.InvocationTargetException e){ if(e.getCause() instanceof RuntimeException r)throw r; if(e.getCause() instanceof Error r)throw r; throw new RuntimeException(e.getCause()); }
            catch(Exception e){throw new RuntimeException(e);}
        };
        return Class.forName("net.minecraft.gametest.framework.TestFunction")
            .getConstructor(String.class,String.class,String.class,rotationType,int.class,long.class,boolean.class,int.class,int.class,java.util.function.Consumer.class)
            .newInstance(values.getOrDefault("batch","defaultBatch")+"/"+owner.getName()+"/"+name,name,structure,rotation,values.getOrDefault("timeoutTicks",100),values.getOrDefault("setupTicks",0L),values.getOrDefault("required",true),values.getOrDefault("requiredSuccesses",1),values.getOrDefault("attempts",1),action);
    }
    @SubscribeEvent
    public static void advanceTests(net.minecraftforge.event.TickEvent.ServerTickEvent event) throws Exception {
        if (event.phase != net.minecraftforge.event.TickEvent.Phase.END || ticker == null) return;
        var tests=(java.util.Collection<?>)activeTests.get(ticker);
        for(Object info:tests.toArray()) {
            try { testTick.invoke(info); }
            catch(java.lang.reflect.InvocationTargetException error) {
                // An unchecked exception in a delayed test is a test failure, not a reason to kill this shared test server.
                org.slf4j.LoggerFactory.getLogger("TC4_VALIDATION").error("Unchecked GameTest failure: "+info,error.getCause());
                if((boolean)testDone.invoke(info)) { ticker=null; return; }
                testFail.invoke(info,error.getCause());
                for(Object listener:((java.util.Collection<?>)listeners.get(info)).toArray()) notifyFailure.invoke(listener,info);
            }
        }
        for(Object info:tests.toArray()) if((boolean)testDone.invoke(info)) tests.remove(info);
    }
    @SubscribeEvent
    public static void registerCommands(net.minecraftforge.event.RegisterCommandsEvent event) throws Exception {
        if (!Boolean.getBoolean("tc4.validationHarness")) return;
        Class.forName("net.minecraft.gametest.framework.TestCommand")
            .getMethod("m_127946_", com.mojang.brigadier.CommandDispatcher.class)
            .invoke(null, event.getDispatcher());
    }

    @SubscribeEvent
    public static void registerTests(ServerAboutToStartEvent event) throws Exception {
        if (!Boolean.getBoolean("tc4.validationHarness"))
            throw new IllegalStateException("Test harness requires explicit tc4.validationHarness opt-in");
        // Production MinecraftServer omits this clock. Never double-tick development servers.
        if (!Class.forName("net.minecraft.SharedConstants").getField("f_136183_").getBoolean(null)) {
            Class<?> tickerType = Class.forName("net.minecraft.gametest.framework.GameTestTicker");
            ticker = tickerType.getField("f_177648_").get(null);
            activeTests=tickerType.getDeclaredField("f_127784_");activeTests.setAccessible(true);
            Class<?> info=Class.forName("net.minecraft.gametest.framework.GameTestInfo");
            testTick=info.getMethod("m_127628_");testDone=info.getMethod("m_127641_");testFail=info.getMethod("m_127622_",Throwable.class);
            listeners=info.getDeclaredField("f_127601_");listeners.setAccessible(true);
            notifyFailure=Class.forName("net.minecraft.gametest.framework.GameTestListener").getMethod("m_8066_",info);
        }
        Class<?> reporterType = Class.forName("net.minecraft.gametest.framework.TestReporter");
        Object reporter = java.lang.reflect.Proxy.newProxyInstance(reporterType.getClassLoader(), new Class<?>[]{reporterType}, (proxy, method, args) -> {
            if (args != null && args.length == 1 && args[0].getClass().getName().endsWith("GameTestInfo")) {
                Object info = args[0];
                String name = (String) info.getClass().getMethod("m_127633_").invoke(info);
                Throwable error = (Throwable) info.getClass().getMethod("m_127642_").invoke(info);
                org.slf4j.LoggerFactory.getLogger("TC4_VALIDATION_RESULT").info("{} {}", error == null ? "PASS" : "FAIL", name);
                if (error != null) org.slf4j.LoggerFactory.getLogger("TC4_VALIDATION_RESULT").error("Failure: " + name, error);
            }
            return null;
        });
        Class.forName("net.minecraft.gametest.framework.GlobalTestReporter").getMethod("m_177655_", reporterType).invoke(null, reporter);
        Class<?> registry = Class.forName("net.minecraft.gametest.framework.GameTestRegistry");
        Method register = registry.getMethod("register", Method.class, Set.class);
        int classes = 0, methods = 0;
        try (var input = ValidationHarness.class.getResourceAsStream("/validation-test-classes.txt")) {
            if (input == null) throw new IllegalStateException("Missing acceptance class manifest");
            try (var reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
                for (String name : reader.lines().filter(s -> !s.isBlank()).toList()) {
                    if (!name.matches(System.getProperty("tc4.validationClassPattern", ".*"))) continue;
                    Class<?> type = Class.forName(name);
                    var classNode = new org.objectweb.asm.tree.ClassNode();
                    try(var bytes=type.getResourceAsStream("/"+name.replace('.','/')+".class")) {
                        new org.objectweb.asm.ClassReader(bytes).accept(classNode,org.objectweb.asm.ClassReader.SKIP_CODE);
                    }
                    for (Method method : type.getDeclaredMethods()) {
                        var node=classNode.methods.stream().filter(m->m.name.equals(method.getName())&&m.desc.equals(org.objectweb.asm.Type.getMethodDescriptor(method))).findFirst().orElseThrow();
                        var annotation=node.visibleAnnotations==null?null:node.visibleAnnotations.stream().filter(a->a.desc.equals("Lnet/minecraft/gametest/framework/GameTest;")).findFirst().orElse(null);
                        if(annotation==null) register.invoke(null, method, Set.of());
                        else ((java.util.Collection)registry.getMethod("m_127658_").invoke(null)).add(annotatedTest(type,method,annotation));
                        methods++;
                    }
                    classes++;
                }
            }
        }
        String completedFile=System.getProperty("tc4.validationSkipPassedFile", "");
        if(!completedFile.isBlank()) {
            var completed=new java.util.HashSet<>(java.nio.file.Files.readAllLines(java.nio.file.Path.of(completedFile),StandardCharsets.UTF_8));
            var functions=(java.util.Collection<?>)registry.getMethod("m_127658_").invoke(null);
            for(var function:functions.toArray()) {
                String name=(String)function.getClass().getMethod("m_128075_").invoke(function);
                if(completed.contains(name))functions.remove(function);
            }
        }
        org.slf4j.LoggerFactory.getLogger("TC4_VALIDATION").info("Registered classes=" + classes + " inspected methods=" + methods
            + " testFunctions=" + ((java.util.Collection<?>) registry.getMethod("m_127658_").invoke(null)).size());
    }
}
