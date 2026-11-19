package automagy.logic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PortableCraftingCostsTest {
 @Test void independentlyDiscountsRecipeAndFocus(){assertEquals(1190,PortableCraftingCosts.centivis(7,700,.9f,.8f));}
 @Test void gratisRemovesOnlyFocusFee(){assertEquals(630,PortableCraftingCosts.centivis(7,0,.9f,.8f));}
 @Test void roundsEachPaymentBeforeAdding(){assertEquals(105,PortableCraftingCosts.centivis(1,7,.999f,.999f));}
 @Test void zeroCostIsFree(){assertEquals(0,PortableCraftingCosts.centivis(0,0,1,1));}
 @Test void rejectsOverflowAndInvalidCosts(){
  assertThrows(ArithmeticException.class,()->PortableCraftingCosts.centivis(Integer.MAX_VALUE,0,1,1));
  assertThrows(IllegalArgumentException.class,()->PortableCraftingCosts.centivis(-1,0,1,1));
  assertThrows(IllegalArgumentException.class,()->PortableCraftingCosts.centivis(1,1,Float.NaN,1));
 }
}
