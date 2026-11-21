package automagy.logic;
import java.util.*;
import java.util.function.BiPredicate;
/** A recipe grid consumes one unit per occupied cell, even when identical cells share a stack. */
public final class CraftingAllocation {
 public record Allocation(int[] gridSources,int[] slotUse){
  public Allocation{gridSources=gridSources.clone();slotUse=slotUse.clone();}
  @Override public int[] gridSources(){return gridSources.clone();}
  @Override public int[] slotUse(){return slotUse.clone();}
 }
 public static <T> Optional<Allocation> allocate(List<T> template,List<T> inventory,int[] counts,BiPredicate<T,T> matches){
  if(counts.length!=inventory.size())throw new IllegalArgumentException("Inventory counts mismatch");
  if(template.size()>9||inventory.size()>9)throw new IllegalArgumentException("Workbench has a nine-cell grid and nine inventory slots");
  var units=new ArrayList<Integer>();for(int slot=0;slot<counts.length;slot++)for(int n=0;n<Math.min(template.size(),Math.max(0,counts[slot]));n++)units.add(slot);
  int[] owners=new int[units.size()];Arrays.fill(owners,-1);
  for(int cell=0;cell<template.size();cell++)if(template.get(cell)!=null&&!assign(cell,template,inventory,matches,units,owners,new boolean[units.size()]))return Optional.empty();
  int[] use=new int[counts.length],sources=new int[template.size()];Arrays.fill(sources,-1);
  for(int unit=0;unit<owners.length;unit++)if(owners[unit]>=0){int slot=units.get(unit);sources[owners[unit]]=slot;use[slot]++;}
  return Optional.of(new Allocation(sources,use));
 }
 /** Augmenting paths solve overlapping predicates without factorial recipe backtracking. */
 private static <T> boolean assign(int cell,List<T> template,List<T> inventory,BiPredicate<T,T> matches,List<Integer> units,int[] owners,boolean[] seen){
  for(int unit=0;unit<units.size();unit++){if(seen[unit]||!matches.test(template.get(cell),inventory.get(units.get(unit))))continue;seen[unit]=true;if(owners[unit]<0||assign(owners[unit],template,inventory,matches,units,owners,seen)){owners[unit]=cell;return true;}}return false;
 }
 private CraftingAllocation(){}
}
