package ttmodern;

import java.util.List;

/** Order does not matter, multiplicity does. Used by both reload recipes and summoning. */
public final class SoulComposition {
    public static boolean matches(List<String> recipe,List<String> inputs){return recipe.size()==3&&inputs.size()==3&&recipe.stream().sorted().toList().equals(inputs.stream().sorted().toList());}
    private SoulComposition(){}
}
