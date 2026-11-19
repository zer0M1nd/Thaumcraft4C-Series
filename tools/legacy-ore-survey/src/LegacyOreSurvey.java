package tc4revival.legacyoresurvey;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLServerStartedEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

/** Test-only 1.7.10 preloader. No ore placement, worldgen registration or RNG changes. */
@Mod(modid="tc4oresurvey", name="TC4 legacy ore survey preloader", version="1",
     dependencies="required-after:Thaumcraft", acceptableRemoteVersions="*")
public class LegacyOreSurvey {
    private Object world, provider;
    private Method load, finishPopulation;
    private Field populated;
    private int generated, checked, oreId, populationCompletions;
    private boolean active;
    private static final int START=1000, WIDTH=64, PAD=1, GENERATION_WIDTH=WIDTH+2*PAD;

    @Mod.EventHandler public void started(FMLServerStartedEvent event) {
        try {
            Class<?> serverType=Class.forName("net.minecraft.server.MinecraftServer");
            Object server=serverType.getMethod("func_71276_C").invoke(null);
            world=serverType.getMethod("func_71218_a",int.class).invoke(server,0);
            load=world.getClass().getMethod("func_72964_e",int.class,int.class);
            provider=world.getClass().getMethod("func_72863_F").invoke(world);
            Class<?> providerType=Class.forName("net.minecraft.world.chunk.IChunkProvider");
            finishPopulation=Class.forName("net.minecraft.world.chunk.Chunk").getMethod("func_76624_a",providerType,providerType,int.class,int.class);
            populated=Class.forName("net.minecraft.world.chunk.Chunk").getField("field_76646_k");
            Object ore=Class.forName("thaumcraft.common.config.ConfigBlocks").getField("blockCustomOre").get(null);
            Class<?> block=Class.forName("net.minecraft.block.Block");
            oreId=(Integer)block.getMethod("func_149682_b",block).invoke(null,ore);
            long seed=(Long)world.getClass().getMethod("func_72905_C").invoke(world);
            if(seed!=9202026L)throw new IllegalStateException("Unexpected seed "+seed);
            Files.write(Paths.get("survey-environment.json"), ("{\"seed\":"+seed+",\"ore_registry_name\":\"Thaumcraft:blockCustomOre\",\"ore_numeric_id\":"+oreId+",\"target_start_chunk\":1000,\"target_width_chunks\":64,\"padding_chunks\":1}\n").getBytes(StandardCharsets.UTF_8));
            active=true;
            FMLCommonHandler.instance().bus().register(this);
            System.out.println("LEGACY ORE SURVEY START: real overworld seed="+seed+", oreId="+oreId+", generated area=66x66, counted area=64x64");
        } catch(Exception e) { throw new IllegalStateException("Cannot initialize legacy ore survey",e); }
    }

    @SubscribeEvent public void tick(TickEvent.ServerTickEvent event) {
        if(!active || event.phase!=TickEvent.Phase.END)return;
        try {
            int total=GENERATION_WIDTH*GENERATION_WIDTH;
            if(generated<total) { for(int batch=0;batch<2 && generated<total;batch++) {
                int x=START-PAD+generated%GENERATION_WIDTH,z=START-PAD+generated/GENERATION_WIDTH;
                ensurePopulated(x,z);
                generated++;
                if(generated%256==0 || generated==total)
                    System.out.println("LEGACY ORE SURVEY progress generated="+generated+"/"+total);
                }
                return;
            }
            // Re-run the same guarded neighbour-population check used by vanilla
            // loadChunk, only for pending chunks. Never call TC's generator directly.
            for(int n=0;n<16 && checked<WIDTH*WIDTH;n++) {
                int x=START+checked%WIDTH,z=START+checked/WIDTH;
                Object chunk=load.invoke(world,x,z);
                if(!populated.getBoolean(chunk))throw new IllegalStateException("Unpopulated target chunk "+x+","+z);
                checked++;
            }
            if(checked==WIDTH*WIDTH) {
                active=false;
                String message="LEGACY ORE SURVEY READY: generated="+generated+", verified_populated="+checked+", oreId="+oreId+", guarded_population_completions="+populationCompletions;
                Files.write(Paths.get("survey-ready.txt"),(message+"\n").getBytes(StandardCharsets.UTF_8));
                System.out.println(message+"; save-all and stop before reading region files.");
            }
        } catch(Exception e) {
            active=false;
            e.printStackTrace();
            throw new IllegalStateException("Legacy ore survey failed",e);
        }
    }

    private void ensurePopulated(int x,int z) throws Exception {
        Object chunk=load.invoke(world,x,z);
        // The existence test in vanilla population means loaded, not merely saved.
        // Keep its east/south 2x2 prerequisites live in this same server tick.
        load.invoke(world,x+1,z);
        load.invoke(world,x,z+1);
        load.invoke(world,x+1,z+1);
        if(!populated.getBoolean(chunk)) {
            finishPopulation.invoke(chunk,provider,provider,x,z);
            populationCompletions++;
        }
        if(!populated.getBoolean(chunk))throw new IllegalStateException("Immediate population failure "+x+","+z);
    }
}
