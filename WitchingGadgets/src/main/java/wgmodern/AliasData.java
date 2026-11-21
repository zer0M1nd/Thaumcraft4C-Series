package wgmodern;
import java.util.*;import static wgmodern.WGData.*;
/** Visual references keep original gameplay prerequisites and never own completion markers. */
public class AliasData {
 @SuppressWarnings("unchecked") public static void add(List<Object> research,List<Object> pages){
  var aliases=new HashSet<String>();ResearchAliases.ALL.forEach(a->aliases.add(a.original()));
  for(Object raw:research){var row=(Map<String,Object>)raw;if(row.get("parents") instanceof List<?> parents){var links=new LinkedHashMap<String,String>();for(var parent:parents)if(aliases.contains(parent.toString()))links.put(parent.toString(),ResearchAliases.key(parent.toString()));if(!links.isEmpty())row.put("parentMirrors",links);}}
  for(var alias:ResearchAliases.ALL){var entry=research("WGFAKE"+alias.original(),"calculator",alias.x(),alias.y(),1,Map.of(),List.of(),map("isStub",true,"isSpecial",alias.special()),0);entry.put("mirrorOf",alias.original());research.add(entry);}
 }
}
