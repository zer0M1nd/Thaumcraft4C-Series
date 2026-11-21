package tc4verification;

import java.util.Objects;

public final class Checks {
    private Checks() {}
    public static void assertTrue(boolean value,String message){if(!value)throw new AssertionError(message);}
    public static void assertSame(Object expected,Object actual){assertTrue(expected==actual,"Reference mismatch");}
    public static void assertNull(Object actual){assertTrue(actual==null,"Expected null: "+actual);}
    public static void assertEquals(Object expected,Object actual){
        boolean equal=expected instanceof Number a&&actual instanceof Number b
            ?Double.compare(a.doubleValue(),b.doubleValue())==0:Objects.equals(expected,actual);
        assertTrue(equal,"Expected "+expected+", got "+actual);
    }
    public static void assertThrows(Class<? extends Throwable> expected,Runnable action){
        try{action.run();}catch(Throwable error){if(expected.isInstance(error))return;throw new AssertionError("Wrong exception",error);}
        throw new AssertionError("Missing "+expected.getName());
    }
    public static void fail(String message){throw new AssertionError(message);}
}
