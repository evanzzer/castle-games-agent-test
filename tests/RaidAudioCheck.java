package castle;
import java.util.*;
public final class RaidAudioCheck {
 static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
 static Engine.Fort fort(int side){return new Engine.Fort(200,150,500,side,0);}
 public static void main(String[]args){
  Engine e=new Engine(1,0);e.forts.clear();e.marches.clear();e.marshes.clear();e.think=10000;
  Engine.Fort a=fort(1),b=fort(1),target=fort(2);e.forts.add(a);e.forts.add(b);e.forts.add(target);
  check(e.command(Arrays.asList(a,b),Collections.emptyList(),target,200,150)==2,"combined command deploys two groups");
  Engine.March first=e.marches.get(0),second=e.marches.get(1);
  check(first.raidWave==second.raidWave,"one command shares its wave identity");
  e.arrive(first);e.time+=3;e.arrive(second);
  check(e.raidSerial==1&&target.fight.troops[1]==500,"staggered combined arrivals sound once and retain all troops");
  int previous=e.raidSerial;float before=target.n;
  e.arrive(new Engine.March(fort(2),target,250));
  check(e.raidSerial==previous&&target.n==before+250,"friendly reinforcement during a raid is silent and adds troops");
  e.arrive(new Engine.March(a,target,100));check(e.raidSerial==2,"independent later raider sounds again");
  Engine.Encounter encounter=new Engine.Encounter(200,150);
  Engine.March one=new Engine.March(a,target,300),two=new Engine.March(b,target,400);
  encounter.add(one);encounter.add(two);one.encounter=null;two.encounter=null;
  check(one.raidWave==two.raidWave,"survivors of a combined field battle share identity");
  e.arrive(one);int serial=e.raidSerial;e.time+=5;e.arrive(two);
  check(e.raidSerial==serial&&target.fight.troops[1]==1300,"combined field survivors add strength without duplicated arrival audio");
  target.fight=null;e.arrive(one);check(e.raidSerial==serial+1,"a new siege can sound even for a previously heard wave");
  e.energy=10000;e.skillLevels[3]=5;e.power(3,target);int n=e.marches.size();
  Object wave=e.marches.get(n-1).raidWave;
  for(int i=n-4;i<n;i++)check(e.marches.get(i).raidWave==wave,"all reinforcement groups form one raid wave");
  System.out.println("Raid audio passed: combined commands, staggered field-battle survivors, independent attacks, fresh sieges and reinforcement waves.");
 }
}
