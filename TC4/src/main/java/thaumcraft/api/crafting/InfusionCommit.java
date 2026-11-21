package thaumcraft.api.crafting;

import java.util.*;
import java.util.function.Supplier;

/** Coordinates only the final infusion commit, not essentia/ingredients already consumed while crafting. */
public final class InfusionCommit {
    public interface Operation {
        boolean validate();
        /** False or an exception requests reverse-order rollback, including this operation. */
        boolean commit();
        /** Idempotent, including when commit was never called. Must restore any partial writes. */
        void abort();
        /** Irreversible effects belong here, after every resource and the final output committed. */
        default void completed() {}
    }
    private InfusionCommit() {}
    public static boolean execute(List<? extends Supplier<? extends Operation>> prepare,Operation delivery){
        var operations=new ArrayList<Operation>();boolean success=false;
        try {
            for(var factory:prepare)operations.add(Objects.requireNonNull(factory.get()));
            operations.add(Objects.requireNonNull(delivery));
            for(var operation:operations)if(!operation.validate())return false;
            for(var operation:operations)if(!operation.commit())return false;
            success=true;
        }catch(RuntimeException failure){com.mojang.logging.LogUtils.getLogger().error("Infusion commit aborted",failure);return false;}
        finally {
            if(!success)for(int i=operations.size()-1;i>=0;i--)try{operations.get(i).abort();}
                catch(RuntimeException failure){com.mojang.logging.LogUtils.getLogger().error("Infusion rollback failed",failure);}
        }
        for(var operation:operations)try{operation.completed();}
            catch(RuntimeException failure){com.mojang.logging.LogUtils.getLogger().error("Infusion completion observer failed; output remains committed",failure);}
        return true;
    }
}
