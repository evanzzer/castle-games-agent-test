package castle;
import java.util.*;
public final class CitadelCheck {
 static void check(boolean okay,String message){if(!okay)throw new AssertionError(message);}
 public static void main(String[] args){
  check(Citadel.NAMES.length==30&&Citadel.BUILDINGS.length==30,"thirty authored citadel stages");
  Set<String> layouts=new HashSet<>(),terrains=new HashSet<>();Set<Integer> counts=new HashSet<>();
  for(int stage=91;stage<=120;stage++){
   Engine e=new Engine(stage,0);layouts.add(Citadel.BUILDINGS[stage-91]);
   check(e.campaign==Campaign.CITADEL&&e.marshes.size()>=2&&e.altars.size()>=4,"authored terrain and altars");
   boolean red=false,enemy=false;
   for(Engine.Fort f:e.forts){check(f.x>20&&f.x<940&&f.y>20&&f.y<620&&f.n==500,"site inside map");red|=f.side==1;enemy|=f.side==2;}
   check(red&&enemy&&e.forts.size()>=14&&e.forts.size()<=40,"playable building count with both factions");counts.add(e.forts.size());
   for(int side=2;side<=3;side++)check(e.attackLevels[side]==5&&e.defenseLevels[side]==4&&e.nationEnergy[side]==350,"enemy tier");
   for(int s=0;s<4;s++)check(Math.abs(e.spellCost(2,s)-Engine.COST[s]*1.25f)<.001&&e.spellCost(1,s)==Engine.COST[s],"1.25x enemy spell cost");
   int[] h=new int[4];for(Engine.Fort f:e.forts)if(f.kind==Engine.GENERATOR)h[f.side]++;
   check(h[1]==h[2]&&h[1]>=3&&h[1]<=5,"red and blue start with the same 3-5 houses");
   // Every stage is exactly symmetric under an x-mirror or a 180-degree turn (D2 stages satisfy both).
   boolean mirror=true,turn=true;
   for(Engine.Fort f:e.forts){boolean m=false,t=false;
    for(Engine.Fort g:e.forts){
     if(Math.abs(g.x-(960-f.x))<.01&&Math.abs(g.y-f.y)<.01&&g.kind==f.kind)m=true;
     if(Math.abs(g.x-(960-f.x))<.01&&Math.abs(g.y-(640-f.y))<.01&&g.kind==f.kind)t=true;}
    mirror&=m;turn&=t;}
   check(mirror||turn,"stage "+stage+" is mirror or point symmetric");
   terrains.add(Citadel.MARSHES[stage-91]);
  }
  check(terrains.size()==30,"every stage has its own terrain");
  check(counts.size()>=8,"building counts differ between stages");
  check(layouts.size()==30,"every stage has a distinct ownership ledger");
  Engine e=new Engine(91,0);e.think=100000;float before=e.nationEnergy[2];
  e.tick(6);check(e.nationEnergy[2]>before,"enemy tier-3 crystal income");
  check(UpgradeRules.spellCooldown(0,3)<10,"tier-3 cooldown");
  System.out.println("Citadel checks passed: 30 unique symmetric stages with their own terrain; enemy 5/4 combat, tier-3 economy, 1.25x spells.");
 }
}
