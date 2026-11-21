package automagy.gametest;

import automagy.content.*;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.*;
import net.minecraft.gametest.framework.*;
import net.minecraftforge.gametest.*;
import java.util.List;


@GameTestHolder("automagy") @PrefixGameTestTemplate(false)
public class EagerFiltersGameTests {
    @GameTest(template="empty", templateNamespace="automagy") public static void laterExplicitReferenceOverridesEarlierNameRule(GameTestHelper h){
        ItemFilter first=new ItemFilter(),second=new ItemFilter();first.name("*");second.entry(0,new ItemStack(Items.DIAMOND));
        assertEquals(0,EagerFilters.limit(List.of(new EagerFilters.Sheet(first,false),new EagerFilters.Sheet(second,true)),new ItemStack(Items.DIAMOND),List.of(),List.of()));
        h.succeed();
    }
    @GameTest(template="empty", templateNamespace="automagy") public static void unmatchedWhitelistDeniesWhileBlacklistPermits(GameTestHelper h){
        ItemFilter filter=new ItemFilter();filter.entry(0,new ItemStack(Items.DIAMOND));var iron=new ItemStack(Items.IRON_INGOT);
        assertEquals(0,EagerFilters.limit(List.of(new EagerFilters.Sheet(filter,false)),iron,List.of(),List.of()));
        assertEquals(Long.MAX_VALUE,EagerFilters.limit(List.of(new EagerFilters.Sheet(filter,true)),iron,List.of(),List.of()));
        h.succeed();
    }
    @GameTest(template="empty", templateNamespace="automagy") public static void pairedCountRulesRetainSourceOrFillDestination(GameTestHelper h){
        ItemFilter filter=new ItemFilter();var iron=new ItemStack(Items.IRON_INGOT);filter.entry(0,iron);filter.useCount=true;filter.count(0,16);
        var source=List.of(iron.copyWithCount(30));var destination=List.of(iron.copyWithCount(12));
        assertEquals(14,EagerFilters.limit(List.of(new EagerFilters.Sheet(filter,true)),iron,source,destination));
        assertEquals(4,EagerFilters.limit(List.of(new EagerFilters.Sheet(filter,false)),iron,source,destination));
        h.succeed();
    }
    private static void assertEquals(long expected,long actual){if(expected!=actual)throw new AssertionError("Expected "+expected+" but got "+actual);}
}
