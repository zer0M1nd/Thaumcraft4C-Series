package wtmodern;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.zip.ZipFile;
import net.minecraft.util.RandomSource;
import net.minecraftforge.fml.loading.FMLPaths;

/** Legacy syllable constraints, with an optional server-side source archive/configuration. */
public class FriendNames {
    private static List<List<Syllable>> cached;
    private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();
    public static class Syllable {
        public final String text;
        public final int next, previous;
        public Syllable(String line) {
            var fields=line.toLowerCase(Locale.ROOT).trim().split("\\s+");text=fields[0];
            var rules=Arrays.asList(fields);next=rules.contains("+c")?2:rules.contains("+v")?1:0;
            previous=rules.contains("-c")?2:rules.contains("-v")?1:0;
        }
    }
    public static List<List<Syllable>> parse(List<String> lines) {
        var result=List.of(new ArrayList<Syllable>(),new ArrayList<Syllable>(),new ArrayList<Syllable>());
        for(String raw:lines){String s=raw.trim();if(s.isEmpty()||s.startsWith("#"))continue;int group=s.startsWith("-")?0:s.startsWith("+")?2:1;if(group!=1)s=s.substring(1);if(!s.isBlank())result.get(group).add(new Syllable(s));}
        if(result.stream().anyMatch(List::isEmpty))throw new IllegalArgumentException("Name syllables require prefixes, middle parts and suffixes");
        return new ArrayList<>(result);
    }
    private static synchronized List<List<Syllable>> syllables() {
        if(cached!=null)return cached;
        Path config=FMLPaths.CONFIGDIR.get().resolve("warptheory/normal.txt");
        try{if(Files.isRegularFile(config))return cached=parse(Files.readAllLines(config,StandardCharsets.UTF_8));}catch(Exception e){LOGGER.warn("Cannot read Warp Theory name configuration",e);}
        Path folder=FMLPaths.GAMEDIR.get().resolve("thaumcraft");
        if(Files.isDirectory(folder))try(var files=Files.list(folder)){
            for(Path file:files.filter(p->Files.isRegularFile(p)&&p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".jar")).sorted().toList()){
                try(var zip=new ZipFile(file.toFile())){var entry=zip.getEntry("assets/warptheory/names/normal.txt");if(entry!=null)try(var in=new BufferedReader(new InputStreamReader(zip.getInputStream(entry),StandardCharsets.UTF_8))){return cached=parse(in.lines().toList());}}
                catch(Exception e){LOGGER.debug("Skipping unusable name source {}",file,e);}
            }
        }catch(IOException e){LOGGER.warn("Cannot inspect Warp Theory name sources",e);}
        LOGGER.info("Original Warp Theory name list unavailable; using built-in syllables (optional config/warptheory/normal.txt overrides this)");
        return cached=parse(List.of("-ka","-lo","-mira","-rin","ve","ro","ni","tha","+nel","+rin","+var","+lon"));
    }
    private static int type(char c){return "aeiouäöõüy".indexOf(c)>=0?1:2;}
    public static String compose(RandomSource random){return compose(syllables(),2+random.nextInt(3),random);}
    public static String compose(List<List<Syllable>> groups,int count,RandomSource random){
        for(int attempt=0;attempt<64;attempt++){
            var chosen=groups.get(0).get(random.nextInt(groups.get(0).size()));var result=new StringBuilder(chosen.text);boolean valid=true;
            for(int i=1;i<count;i++){
                var previous=chosen;var candidates=groups.get(i==count-1?2:1).stream().filter(s->(previous.next==0||type(s.text.charAt(0))==previous.next)&&(s.previous==0||type(previous.text.charAt(previous.text.length()-1))==s.previous)).toList();
                if(candidates.isEmpty()){valid=false;break;}chosen=candidates.get(random.nextInt(candidates.size()));result.append(chosen.text);
            }
            if(valid){result.setCharAt(0,Character.toUpperCase(result.charAt(0)));return result.toString();}
        }
        return "Friend";
    }
}
