package castle;
import java.util.*;
public final class ZenithCheck {
 static void check(boolean okay,String message){if(!okay)throw new AssertionError(message);}
 public static void main(String[] args){
  check(Zenith.NAMES.length==30&&Zenith.BUILDINGS.length==30&&Campaign.ZENITH.firstStage==121&&Campaign.ZENITH.lastStage==150,"thirty authored zenith stages");
  Set<String> layouts=new HashSet<>(),terrains=new HashSet<>();Set<Integer> counts=new HashSet<>();
  for(int stage=121;stage<=150;stage++){
   Engine e=new Engine(stage,0);layouts.add(Zenith.BUILDINGS[stage-121]);terrains.add(Zenith.MARSHES[stage-121]);counts.add(e.forts.size());
   check(e.campaign==Campaign.ZENITH&&e.marshes.size()>=2&&e.altars.size()>=4,"authored terrain and altars");
   int[] sides=new int[4];for(Engine.Fort f:e.forts){check(f.x>20&&f.x<940&&f.y>20&&f.y<620&&f.n==500,"site inside map");sides[f.side]++;}
   check(sides[1]>=3&&sides[2]>=3&&sides[3]>=3&&e.forts.size()>=14&&e.forts.size()<=44,"three active factions at the start");
   for(int side=2;side<=3;side++)check(e.attackLevels[side]==5&&e.defenseLevels[side]==5&&e.nationEnergy[side]==UpgradeRules.initialCrystals(4),"enemy tier 4 start");
   for(int s=0;s<4;s++)check(Math.abs(e.spellCost(2,s)-Engine.COST[s]*1.1f)<.001&&Math.abs(e.spellCost(3,s)-Engine.COST[s]*1.1f)<.001&&e.spellCost(1,s)==Engine.COST[s],"1.1x enemy spell cost");
   check(e.aiSpellLevel()==4,"enemy spells are level 4");
   int[] h=new int[4];for(Engine.Fort f:e.forts)if(f.kind==Engine.GENERATOR)h[f.side]++;
   check(h[1]==h[2]&&h[1]>=3&&h[1]<=5,"red and blue start with the same 3-5 houses");check(h[3]>=3&&h[3]<=5,"yellow starts with 3-5 houses");
   boolean mirror=true,turn=true;
   for(Engine.Fort f:e.forts){boolean m=false,t=false;
    for(Engine.Fort g:e.forts){
     if(Math.abs(g.x-(960-f.x))<.01&&Math.abs(g.y-f.y)<.01&&g.kind==f.kind)m=true;
     if(Math.abs(g.x-(960-f.x))<.01&&Math.abs(g.y-(640-f.y))<.01&&g.kind==f.kind)t=true;}
    mirror&=m;turn&=t;}
   check(mirror||turn,"stage "+stage+" is mirror or point symmetric");
  }
  check(layouts.size()==30&&terrains.size()==30,"every stage has its own layout and terrain");
  check(counts.size()>=8,"building counts differ between stages");
  Engine e=new Engine(121,0);e.think=100000;float before=e.nationEnergy[2];e.tick(6);
  check(e.nationEnergy[2]>before,"enemy tier-4 crystal income");
  check(UpgradeRules.spellCooldown(0,4)<UpgradeRules.spellCooldown(0,3)&&UpgradeRules.castleGeneration(4)>UpgradeRules.castleGeneration(3),"tier 4 improves cooldowns and castle generation");
  System.out.println("Zenith checks passed: 30 unique symmetric three-faction stages; enemy tier 4, level-4 spells, 1.1x cost, 5/5 combat.");
 }
}
