package tc4revival.content;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FocusKeyValidationTest {
    private static FocusKeyValidation.Entry entry(String item,String key){return new FocusKeyValidation.Entry(item,key);}

    @Test void distinctKeysAreAccepted(){
        assertTrue(FocusKeyValidation.duplicates(List.of(
                entry("thaumcraft:focus_fire","AF-1-1-1-1-1"),
                entry("thaumcraft:focus_excavation","BE-1-1-1-1-1"),
                entry("thaumichorizons:focus_liquefaction","BQL-1-1-1-1-1"))).isEmpty());
    }

    @Test void sharedPrefixNamesBothOwners(){
        var duplicates=FocusKeyValidation.duplicates(List.of(
                entry("thaumcraft:focus_excavation","BE-1-1-1-1-1"),
                entry("thaumicbases:focus_flux","FX-1-1-1-1-1"),
                entry("thaumichorizons:focus_liquefaction","BE-1-1-1-1-1")));
        assertEquals(List.of("thaumcraft:focus_excavation","thaumichorizons:focus_liquefaction"),duplicates.get("BE-1-1-1-1-1"));
        assertEquals(1,duplicates.size());
    }

    @Test void sameItemRegisteredOnceIsNotADuplicate(){
        assertTrue(FocusKeyValidation.duplicates(List.of(entry("thaumcraft:focus_fire","AF-1-1-1-1-1"))).isEmpty());
    }

    @Test void repeatedItemIdUnderOneKeyStaysAccepted(){
        // The same focus can be reached through several containers; FocusSelection intentionally keeps one entry.
        assertTrue(FocusKeyValidation.duplicates(List.of(
                entry("thaumcraft:focus_fire","AF-1-1-1-1-1"),
                entry("thaumcraft:focus_fire","AF-1-1-1-1-1"))).isEmpty());
    }

    @Test void upgradeRanksKeepDifferentFocusesApart(){
        assertTrue(FocusKeyValidation.duplicates(List.of(
                entry("thaumcraft:focus_fire","AF1-1-1-1-1"),
                entry("thaumcraft:focus_fire","AF-1-1-1-1-1"))).isEmpty());
    }

    @Test void messageNamesKeyItemsAndTheContract(){
        var message=FocusKeyValidation.describe(Map.of("BE-1-1-1-1-1",List.of(
                "thaumcraft:focus_excavation","thaumichorizons:focus_liquefaction")));
        assertTrue(message.contains("BE-1-1-1-1-1"),message);
        assertTrue(message.contains("thaumcraft:focus_excavation"),message);
        assertTrue(message.contains("thaumichorizons:focus_liquefaction"),message);
        assertTrue(message.contains("sortingPrefix"),"Message must point at the contract owners have to fix: "+message);
    }
}
