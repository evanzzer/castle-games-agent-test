package castle;
import java.util.*;
public final class FrontierCheck {
 static void check(boolean okay,String message){if(!okay)throw new AssertionError(message);}
 static Engine field(int stage){Engine e=new Engine(stage,0,123);e.think=100000;e.forts.clear();e.marshes.clear();e.altars.clear();
  e.forts.add(new Engine.Fort(100,100,1000,1,Engine.CRYSTAL));
  e.forts.add(new Engine.Fort(400,100,1000,2,Engine.CRYSTAL));
  e.forts.add(new Engine.Fort(700,100,1000,3,Engine.CRYSTAL));return e;}
 public static void main(String[] args){
  check(Frontier.BUILDINGS.length==30&&Frontier.NAMES.length==30,"thirty authored levels");
  Set<String> layouts=new HashSet<>(),names=new HashSet<>();int zones=0,altars=0;
  for(int stage=31;stage<=60;stage++){
   Engine e=new Engine(stage,0);names.add(Frontier.NAMES[stage-31]);layouts.add(Frontier.BUILDINGS[stage-31]);
   boolean red=false,enemy=false;
   for(Engine.Fort f:e.forts){
    check(f.x>20&&f.x<940&&f.y>20&&f.y<620,"building inside new map");
    check(f.n==500,"new stages retain original starting garrison");red|=f.side==1;enemy|=f.side>1;
   }
   check(red&&enemy,"playable starting factions");
   for(int i=0;i<e.forts.size();i++)for(int j=i+1;j<e.forts.size();j++)
    check(Math.hypot(e.forts.get(i).x-e.forts.get(j).x,e.forts.get(i).y-e.forts.get(j).y)>50,"distinct building hit areas");
   check(!e.marshes.isEmpty(),"every new stage has terrain strategy");zones+=e.marshes.size();altars+=e.altars.size();
   for(Engine.Marsh m:e.marshes)for(int i=0;i<m.points.length;i+=2)
    check(m.points[i]>=0&&m.points[i]<=960&&m.points[i+1]>=0&&m.points[i+1]<=640,"terrain inside map");
   for(int tick=0;tick<1200&&e.result==0;tick++)e.tick(.05f);
   for(Engine.Fort f:e.forts)check(Float.isFinite(f.n)&&f.n>=0,"valid new-stage simulation");
   for(float energy:e.nationEnergy)check(energy>=0,"AI never overspends");
  }
  check(names.size()==30&&layouts.size()==30&&zones>=45&&altars>=20,"varied authored campaign");
  float[] costs={402.5f,525,350,787.5f};
  Engine prices=field(31);
  for(int spell=0;spell<4;spell++){
   check(prices.spellCost(2,spell)==costs[spell]&&prices.spellCost(3,spell)==costs[spell],"exact 1.75x AI cost");
   check(prices.spellCost(1,spell)==Engine.COST[spell],"player cost unchanged");
  }
  for(int side=2;side<=3;side++){
   Engine e=field(60);Arrays.fill(e.skillLevels,5);e.cooldownLevel=5;
   e.nationEnergy[side]=402.49f;
   check(!e.power(side,0,null,100,100)&&e.meteors.isEmpty(),"AI cannot cast below increased price");
   e.nationEnergy[side]=402.5f;
   check(e.power(side,0,null,100,100)&&e.nationEnergy[side]==0,"AI spends full increased price");
   check(e.energy==200&&e.nationEnergy[side==2?3:2]==200,"independent nation wallets");
   check(e.meteors.size()==5&&e.meteors.get(0).damage==40,"AI meteor has base effect despite player upgrades");
   check(e.nationCooldown[side][0]==10,"AI uses base cooldown despite player upgrades");
   e.tick(2);
   check(e.forts.get(0).n==800&&e.forts.get(side-1).n==1000,"AI damages player and spares own castle");
   e.nationEnergy[side]=525;
   check(e.power(side,1,null,100,100),"AI can cast storm");
   check(e.storms.get(0).damage==50&&e.storms.get(0).duration==10,"AI storm is not upgraded");
   Engine.March playerMarch=new Engine.March(e.forts.get(0),e.forts.get(side-1),500);e.marches.add(playerMarch);
   e.nationEnergy[side]=350;
   check(e.power(side,2,null,playerMarch.x(),playerMarch.y())&&playerMarch.confusedTime==5,"AI mutiny base duration");
   e.nationEnergy[side]=787.5f;
   Engine.Fort friendly=e.forts.get(side-1);float before=friendly.n;
   check(e.power(side,3,friendly,friendly.x,friendly.y)&&friendly.n==before&&e.marches.stream().filter(m->m.side==friendly.side&&m.b==friendly).mapToInt(m->m.n).sum()==100,"AI reinforcement has base count");
   check(e.nationEnergy[side]==0&&e.energy==200,"AI spell spending never charges player");
  }
  Engine income=field(31);income.tick(6);
  check(income.nationEnergy[2]==265&&income.nationEnergy[3]==265,"AI earns crystals from owned mines on same six-second timer");
  Engine decision=field(31);decision.nationEnergy[2]=402.5f;decision.forts.get(0).n=400;
  check(decision.tryAISpell(2)&&decision.aiCasts[2]==1&&decision.nationEnergy[2]==0,"AI actually decides to cast a paid spell");
  check(!decision.tryAISpell(2)&&decision.aiCasts[2]==1,"AI cannot recast for free");
  Engine relief=field(31);relief.nationEnergy[2]=787.5f;
  Engine.Fort home=relief.forts.get(1);home.n=100;
  Engine.March invasion=new Engine.March(relief.forts.get(0),home,1000);relief.arrive(invasion);
  check(relief.tryAISpell(2)&&home.n==100&&relief.marches.stream().anyMatch(m->m.side==2&&m.b==home&&m.n==100)&&relief.nationEnergy[2]==0,"AI reinforces a threatened castle");
  Engine clustered=field(31);clustered.nationEnergy[3]=525;
  check(clustered.tryAISpell(3)&&clustered.storms.size()==1,"AI selects storm for a high-value stationary target");
  Engine stop=field(31);stop.nationEnergy[2]=350;
  Engine.March assault=new Engine.March(stop.forts.get(0),stop.forts.get(1),700);stop.marches.add(assault);
  check(stop.tryAISpell(2)&&assault.confusedTime==5,"AI stops an approaching enemy army");
  Engine harder=new Engine(60,0),original=new Engine(30,0);
  check(harder.aiDelay()<original.aiDelay()&&harder.attackLevels[2]>original.attackLevels[2],"second campaign difficulty increases");
  Engine first=new Engine(42,0,77),second=new Engine(42,0,77);
  for(int i=0;i<600;i++){first.tick(.05f);second.tick(.05f);}
  for(int i=0;i<first.forts.size();i++)check(first.forts.get(i).n==second.forts.get(i).n,"seeded AI remains deterministic");
  System.out.println("Frontier checks passed: 30 unique levels, "+zones+" regions, "+altars+" altars; 1.75x paid AI spells at base strength.");
 }
}
