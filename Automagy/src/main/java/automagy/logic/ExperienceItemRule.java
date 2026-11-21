package automagy.logic;
import java.util.Optional;
/** Registry names replace 1.7 metadata. 'phial' uses the separately configured phial value. */
public record ExperienceItemRule(String item,int experience,String remainder,boolean craftingRemainder){
 public static Optional<ExperienceItemRule> parse(String line,int phialValue){try{var pair=line.trim().split("=",-1);if(pair.length!=2||!id(pair[0]))return Optional.empty();var value=pair[1].split(",",-1);if(value.length<1||value.length>2)return Optional.empty();int xp=value[0].trim().equals("phial")?phialValue:Integer.parseInt(value[0].trim());if(xp<1||xp>32767)return Optional.empty();String remainder=value.length==2?value[1].trim():"";if(!remainder.isEmpty()&&!remainder.equals("*")&&!id(remainder))return Optional.empty();return Optional.of(new ExperienceItemRule(pair[0].trim(),xp,remainder.equals("*")?"":remainder,remainder.equals("*")));}catch(NumberFormatException e){return Optional.empty();}}
 private static boolean id(String value){return value.trim().matches("[a-z0-9_.-]+:[a-z0-9/._-]+");}
}
