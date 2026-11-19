package automagy.logic;

import tc4revival.aspect.AspectCatalog;
import tc4revival.research.PlayerKnowledge;

/** Rebuild only the selected scan set using public knowledge operations; no private state access. */
public final class ScanReset {
 public static PlayerKnowledge objects(PlayerKnowledge old,AspectCatalog catalog,String identity){
  var next=new PlayerKnowledge(catalog);
  old.knownAspects().forEach((aspect,amount)->{
   // Pools may exceed a short in memory. Avoid truncating them through the persistence codec.
   if(amount<=Short.MAX_VALUE)next.setPool(aspect,amount.shortValue());
   else {next.setPool(aspect,(short)0);int left=amount;while(left>0){short step=(short)Math.min(Short.MAX_VALUE,left);next.changePool(aspect,step);left-=step;}}
  });
  old.researchMarkers().forEach(next::rememberResearch);
  for(var kind:PlayerKnowledge.ScanKind.values())for(var key:old.scannedKeys(kind)){
   boolean remove=kind==PlayerKnowledge.ScanKind.OBJECT&&(identity==null||key.equals("@"+identity)||key.equals("#"+identity));
   if(!remove)next.rememberScan(kind,key);
  }
  for(var kind:PlayerKnowledge.WarpKind.values())next.setWarp(kind,old.warp(kind));
  next.setWarpCounter(old.warpCounter());next.researchPreferences(old.researchPreferences());return next;
 }
 private ScanReset(){}
}
