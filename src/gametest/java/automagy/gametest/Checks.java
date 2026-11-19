package automagy.gametest;

/** Assertions inside the Forge runtime without adding JUnit to the game module path. */
public class Checks {
    public static void assertEquals(Object expected,Object actual) {
        boolean equal=expected instanceof Number a && actual instanceof Number b
                ? new java.math.BigDecimal(a.toString()).compareTo(new java.math.BigDecimal(b.toString()))==0
                : java.util.Objects.equals(expected,actual);
        if(!equal)throw new AssertionError("Expected "+expected+" but got "+actual);
    }
    public static void assertTrue(boolean value){if(!value)throw new AssertionError("Expected true");}
    public static void assertFalse(boolean value){if(value)throw new AssertionError("Expected false");}
    public static void assertNull(Object value){if(value!=null)throw new AssertionError("Expected null: "+value);}
    public static void fail(String message){throw new AssertionError(message);}
    public static void fail(){throw new AssertionError("Unexpected callback");}
    public static void assertThrows(Class<? extends Throwable> type,Runnable action){
        try{action.run();}catch(Throwable failure){if(type.isInstance(failure))return;throw new AssertionError("Wrong exception",failure);}
        throw new AssertionError("Expected "+type.getName());
    }
}
