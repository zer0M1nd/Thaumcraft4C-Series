package thmodern.vat;

import java.util.*;
import java.util.function.Predicate;

/** Each pedestal contributes one component. Backtracking through previous assignments handles overlapping tags. */
public final class IngredientAssignment {
    public static <T> Optional<int[]> match(List<? extends Predicate<T>> ingredients,List<T> stacks){
        if(ingredients.size()!=stacks.size())return Optional.empty();return matchAvailable(ingredients,stacks);
    }
    public static <T> Optional<int[]> matchAvailable(List<? extends Predicate<T>> ingredients,List<T> stacks){
        if(ingredients.size()>stacks.size()||ingredients.size()>64||stacks.size()>1024)return Optional.empty();
        int[] owners=new int[stacks.size()];Arrays.fill(owners,-1);
        for(int ingredient=0;ingredient<ingredients.size();ingredient++)if(!assign(ingredient,ingredients,stacks,owners,new boolean[stacks.size()]))return Optional.empty();
        int[] slots=new int[ingredients.size()];for(int slot=0;slot<owners.length;slot++)if(owners[slot]>=0)slots[owners[slot]]=slot;return Optional.of(slots);
    }
    private static <T> boolean assign(int ingredient,List<? extends Predicate<T>> ingredients,List<T> stacks,int[] owners,boolean[] visited){
        for(int slot=0;slot<stacks.size();slot++)if(!visited[slot]&&ingredients.get(ingredient).test(stacks.get(slot))){
            visited[slot]=true;
            if(owners[slot]<0||assign(owners[slot],ingredients,stacks,owners,visited)){owners[slot]=ingredient;return true;}
        }
        return false;
    }
}
