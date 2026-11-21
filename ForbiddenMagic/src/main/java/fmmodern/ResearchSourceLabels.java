package fmmodern;
import java.util.Map;
import net.minecraft.network.chat.Component;
import thaumcraft.api.research.ResearchTooltipEvent;
/** Text composition is shared with server-side tests; registration itself is client-only. */
public final class ResearchSourceLabels {
    private static final Map<String,String> LABELS=Map.ofEntries(
            Map.entry("forbidden_magic:BLOODMAGIC","[FM] [BM] "),
            Map.entry("forbidden_magic:ROD_blood","[FM] [BM] "),
            Map.entry("forbidden_magic:ROD_blood_staff","[FM] [BM] "),
            Map.entry("forbidden_magic:CAP_alchemical","[FM] [BM] "),
            Map.entry("forbidden_magic:BLOODWELL","[FM] [BM] "),
            Map.entry("forbidden_magic:BLOODRAPIER","[FM] [BM] "),
            Map.entry("forbidden_magic:ELDRITCHORB","[FM] [BM] "),
            Map.entry("forbidden_magic:BOTANY","[FM] [B] "),
            Map.entry("forbidden_magic:ROD_livingwood","[FM] [B] "),
            Map.entry("forbidden_magic:CAP_manasteel","[FM] [B] "),
            Map.entry("forbidden_magic:ROD_dreamwood_staff","[FM] [B] "),
            Map.entry("forbidden_magic:ROD_dreamwood","[FM] [B] "),
            Map.entry("forbidden_magic:CAP_elementium","[FM] [B] "),
            Map.entry("forbidden_magic:CAP_terrasteel","[FM] [B] "),
            Map.entry("forbidden_magic:EUCLIDAISY","[FM] [B] "),
            Map.entry("forbidden_magic:WHISPERWEED","[FM] [B] "),
            Map.entry("forbidden_magic:TAINTHISTLE","[FM] [B] "),
            Map.entry("forbidden_magic:CRYSTALWELL","[FM] "),
            Map.entry("forbidden_magic:PRIMEWELL","[FM] "),
            Map.entry("forbidden_magic:TRANSEMERALD","[FM] "),
            Map.entry("forbidden_magic:BLACKFLOWER","[FM] "),
            Map.entry("forbidden_magic:RIDINGCROP","[FM] "),
            Map.entry("forbidden_magic:CONSUMING","[FM] "),
            Map.entry("forbidden_magic:EDUCATIONAL","[FM] "),
            Map.entry("forbidden_magic:SCHOOLS","[FM] "),
            Map.entry("forbidden_magic:NETHERSHARDS","[FM] "),
            Map.entry("forbidden_magic:HELLFIRE","[FM] "),
            Map.entry("forbidden_magic:ROD_profane","[FM] "),
            Map.entry("forbidden_magic:SUBCOLLAR","[FM] "),
            Map.entry("forbidden_magic:SKULLAXE","[FM] "),
            Map.entry("forbidden_magic:RINGFOOD","[FM] "),
            Map.entry("forbidden_magic:ARCANECAKE","[FM] "),
            Map.entry("forbidden_magic:FOCUSBLINK","[FM] "),
            Map.entry("forbidden_magic:MORPHTOOLS","[FM] "),
            Map.entry("forbidden_magic:ROD_infernal","[FM] "),
            Map.entry("forbidden_magic:WRATH","[FM] "),
            Map.entry("forbidden_magic:GREEDY","[FM] "),
            Map.entry("forbidden_magic:CORRUPTING","[FM] "),
            Map.entry("forbidden_magic:CLUSTER","[FM] "),
            Map.entry("forbidden_magic:IMPACT","[FM] "),
            Map.entry("forbidden_magic:VOIDTOUCHED","[FM] "),
            Map.entry("forbidden_magic:FORK","[FM] "),
            Map.entry("forbidden_magic:WRATHCAGE","[FM] "),
            Map.entry("forbidden_magic:TAINTSHOVEL","[FM] "),
            Map.entry("forbidden_magic:TAINTPICK","[FM] "),
            Map.entry("forbidden_magic:ROD_tainted","[FM] "),
            Map.entry("forbidden_magic:TAINTTREE","[FM] "),
            Map.entry("forbidden_magic:TAINTSTONE","[FM] "));
    public static void decorate(ResearchTooltipEvent event){
        var label=LABELS.get(event.research());
        if(label!=null&&ForbiddenConfig.value(ForbiddenConfig.TAG_RESEARCH))event.setDescription(Component.literal(label).append(event.description()));
    }
    private ResearchSourceLabels(){}
}
