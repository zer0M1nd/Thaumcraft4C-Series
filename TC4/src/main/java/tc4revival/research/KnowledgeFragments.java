package tc4revival.research;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntUnaryOperator;
import tc4revival.aspect.AspectCatalog;

/** Fragment use grants raw points, without scanning's discovery bonus or soft cap. */
public  class KnowledgeFragments {
    private KnowledgeFragments() {}
    public static List<ScanRules.Reward> consume(PlayerKnowledge knowledge, AspectCatalog aspects, IntUnaryOperator random) {
        var amounts = new ArrayList<Integer>();
        for (var ignored : thaumcraft.api.aspects.AspectSelection.select(aspects,thaumcraft.api.aspects.AspectSelection.KNOWLEDGE_GRANT)) {
            int roll = random.applyAsInt(2);
            if (roll < 0 || roll > 1) throw new IllegalArgumentException("Invalid fragment roll");
            amounts.add(roll + 1);
        }
        var rewards = new ArrayList<ScanRules.Reward>();
        for (int index = 0; index < amounts.size(); index++) {
            var aspect = thaumcraft.api.aspects.AspectSelection.select(aspects,thaumcraft.api.aspects.AspectSelection.KNOWLEDGE_GRANT).get(index);
            int amount = amounts.get(index);
            int before=knowledge.poolAmount(aspect);
            knowledge.changePool(aspect, amount);
            rewards.add(new ScanRules.Reward(aspect, knowledge.poolAmount(aspect)-before, knowledge.pool(aspect), false));
        }
        return List.copyOf(rewards);
    }
}
