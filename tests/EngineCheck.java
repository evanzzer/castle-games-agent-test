package castle;

public final class EngineCheck {
 static void check(boolean condition,String message){
  if(!condition)throw new AssertionError(message);
 }
 static boolean summonAndArrive(Engine e,Engine.Fort target){
  int previous=e.marches.size();if(!e.power(3,target))return false;
  while(e.marches.size()>previous){Engine.March unit=e.marches.remove(previous);e.arrive(unit);}return true;
 }
 public static void main(String[] args){
  int[] savedLevels={8,6,-1,3};
  check(UpgradeRules.normalizeAndRefund(savedLevels)==4,"refund saved levels beyond tier five");
  check(savedLevels[0]==5&&savedLevels[1]==5&&savedLevels[2]==0&&savedLevels[3]==3,"normalize saved tiers");
  check(UpgradeRules.normalizeAndRefund(savedLevels)==0,"refund is not repeated");
  check(!UpgradeRules.canBuy(5,100)&&UpgradeRules.canBuy(4,1)&&!UpgradeRules.canBuy(4,0),"purchase and display share tier limit");
  Engine spells=new Engine(1,0);
  spells.energy=1000;
  spells.think=100;
  Engine.Fort enemy=spells.forts.get(1);
  enemy.kind=Engine.CRYSTAL;
  check(spells.power(0,enemy)&&enemy.n==500&&spells.meteors.size()==5,"five original meteors with delayed impact");
  spells.tick(2);
  check(enemy.n==300,"five impacts each deal forty damage");
  check(summonAndArrive(spells,spells.forts.get(0))&&spells.forts.get(0).n==650,
   "base reinforcement group");
  check(spells.power(1,enemy),"storm starts");
  spells.tick(.4f);
  check(enemy.n==250,"original storm damage and interval");
  Engine poison=new Engine(7,0);
  check(poison.marshes.stream().anyMatch(m->!m.dark),"poison polygon imported");
  Engine slow=new Engine(5,0);
  check(slow.marshes.stream().anyMatch(m->m.dark),"slow polygon imported");
  for(Engine b:new Engine[]{poison,slow}){
   b.think=100;
   Engine.Marsh area=b.marshes.get(0);
   float x=0,y=0;
   for(int i=0;i<area.points.length;i+=2){x+=area.points[i];y+=area.points[i+1];}
   x/=area.points.length/2;y/=area.points.length/2;
   check(area.contains(x,y),"polygon point containment");
   Engine.Fort from=new Engine.Fort(x,y,1000,1,Engine.GENERATOR);
   Engine.Fort to=new Engine.Fort(x+1000,y,1000,2,Engine.GENERATOR);
   b.send(from,to);
   Engine.March march=b.marches.get(0);
   b.tick(.01f);
   check(area.dark?march.slowed:march.poisoned,"actual stage polygon affects troop");
  }
  Engine b=new Engine(1,0);
  b.think=100;
  b.marshes.clear();
  b.marshes.add(new Engine.Marsh(100,100,500,500,false));
  Engine.Fort from=new Engine.Fort(100,100,1000,1,Engine.GENERATOR);
  Engine.Fort to=new Engine.Fort(1000,100,1000,2,Engine.GENERATOR);
  b.send(from,to);
  Engine.March troop=b.marches.get(0);
  b.tick(.5f);
  check(troop.n==500,"no immediate poison damage");
  b.tick(.5f);
  check(troop.n==400,"poison removes twenty percent per terrain second");
  Engine economy=new Engine(1,0);
  economy.think=100;
  check(economy.energy==200,"original starting crystals");
  check(!economy.canUseSpell(0)&&economy.canUseSpell(2),"resource availability matches original spell costs");
  check(!economy.power(0,null,200,200)&&economy.meteors.isEmpty(),"insufficient crystals cannot launch a spell");
  economy.energy=1000;economy.cooldown[0]=1;
  check(!economy.canUseSpell(0),"cooldown disables spell");
  economy.energy=200;economy.cooldown[0]=0;
  economy.tick(6);
  check(economy.energy==225,"base crystal tick");
  check(Engine.COST[0]==230&&Engine.COST[1]==300&&Engine.COST[2]==200&&Engine.COST[3]==450,
   "original spell costs by engine order");
  Engine raids=new Engine(1,0);
  raids.think=100;
  Engine.Fort red=raids.forts.get(0), blue=raids.forts.get(1);
  raids.send(red,blue);
  raids.marches.get(0).t=1;
  raids.tick(.01f);
  check(raids.raidSerial==1,"one raid event on arrival");
  raids.tick(1);
  check(raids.raidSerial==1,"combat tick emits no additional raid event");
  raids.send(red,blue);
  raids.marches.get(0).t=1;
  raids.tick(.01f);
  check(raids.raidSerial==2,"a new raider emits one additional event");
  Engine shots=new Engine(3,0);
  shots.think=100;
  Engine.Fort tower=shots.forts.stream().filter(f->f.kind==Engine.ARCHERY).findFirst().get();
  tower.side=2;
  Engine.Fort arrowSource=new Engine.Fort(tower.x-10,tower.y,1000,1,Engine.GENERATOR);
  shots.send(arrowSource,tower);
  shots.tick(.01f);
  check(shots.arrows.size()==1,"tower produces an arrow animation");
  shots.tick(.2f);
  check(shots.arrows.isEmpty(),"arrow animation expires");

  int[] noUpgrade=new int[4];
  Engine.Fort gate=new Engine.Fort(0,0,1000,2,Engine.GENERATOR);
  Engine.Fort source=new Engine.Fort(100,0,2000,1,Engine.GENERATOR);
  gate.fight=new Engine.Fight(new Engine.March(source,gate,2000));
  gate.fight.troops[1]=2000;
  gate.fight.advance(gate,1,noUpgrade,noUpgrade);
  check(gate.n==800&&gate.fight.troops[1]==1900,"original 2000 vs 1000 siege casualties");
  check(Engine.siegeAttack(200,Engine.GENERATOR,0)==50,"minimum siege damage");
  check(Engine.siegeAttack(20,Engine.GENERATOR,0)==20,"small army deals only remaining strength");
  check(Engine.siegeAttack(500,Engine.ARCHERY,0)>59.99f,"tower origin bonus");
  check(Engine.siegeAttack(1000,Engine.GENERATOR,2)==140,"attack upgrade scaling");
  gate.n=1000;gate.fight.troops[1]=1000;gate.fight.troops[3]=500;
  gate.fight.advance(gate,1,noUpgrade,noUpgrade);
  check(gate.n==850&&gate.fight.troops[1]==900&&gate.fight.troops[3]==400,
   "both attackers damage castle simultaneously and each receives its retaliation");
  int[] defenses={0,0,2,0};
  gate.n=1000;gate.fight.troops[1]=1000;gate.fight.troops[3]=0;
  gate.fight.advance(gate,1,noUpgrade,defenses);
  check(Math.abs(gate.n-(1000-100/1.4f))<.01f,"defender upgrade scaling");
  Engine road=new Engine(1,0);
  road.think=100;
  road.forts.clear();
  Engine.Fort left=new Engine.Fort(100,100,1000,1,Engine.GENERATOR);
  Engine.Fort right=new Engine.Fort(500,100,1000,2,Engine.GENERATOR);
  road.forts.add(left);road.forts.add(right);
  road.send(left,right);road.send(right,left);
  for(int i=0;i<180&&road.encounters.isEmpty();i++)road.tick(.05f);
  check(road.encounters.size()==1,"opposing armies meet on the road");
  Engine.Encounter encounter=road.encounters.get(0);
  check(encounter.x>280&&encounter.x<320,"encounter stays at meeting point");
  road.tick(1);
  check(encounter.total(1)==450&&encounter.total(2)==450,"simultaneous open field casualties");
  check(road.marches.get(0).x()==encounter.x,"engaged troop stops marching");
  road.marches.get(1).n=10;
  road.tick(1);
  check(road.encounters.isEmpty()&&road.marches.stream().anyMatch(m->m.side==1),"winner resumes route");
  check(Engine.capacity(tower)==500,"small arrow tower cap");
  tower.size='m';check(Engine.capacity(tower)==1000,"medium arrow tower cap");
  tower.size='b';check(Engine.capacity(tower)==1500,"large arrow tower cap");
  check(Engine.capacity(new Engine.Fort(0,0,500,0,Engine.CRYSTAL))==1000,"crystal cap");

  Engine buffs=new Engine(9,0);
  check(buffs.altars.size()==2&&buffs.altars.get(0).buff==1,"stage nine angel import");
  buffs.think=100;
  Engine.Altar angel=buffs.altars.get(0);
  Engine.Fort origin=new Engine.Fort(angel.x,angel.y,1000,1,Engine.GENERATOR);
  Engine.Fort goal=new Engine.Fort(angel.x+500,angel.y,1000,2,Engine.GENERATOR);
  buffs.send(origin,goal);
  Engine.March protectedUnit=buffs.marches.get(0);
  buffs.tick(.01f);
  check(protectedUnit.buff==1&&angel.cooldown==30,"angel grants immunity and enters cooldown");
  Engine.damageMarch(protectedUnit,100);
  check(protectedUnit.n==500,"immunity blocks incoming damage");
  protectedUnit.buff=2;
  Engine.damageMarch(protectedUnit,150);
  check(protectedUnit.n==400,"shield adds defense");
  check(new Engine(10,0).altars.get(0).buff==2,"stage ten shield import");

  Engine areaSpell=new Engine(1,0);
  areaSpell.think=100;areaSpell.energy=1000;areaSpell.forts.clear();
  Engine.Fort friendly=new Engine.Fort(200,200,1000,1,Engine.CRYSTAL);
  Engine.Fort hostile=new Engine.Fort(215,200,1000,2,Engine.CRYSTAL);
  areaSpell.forts.add(friendly);areaSpell.forts.add(hostile);
  Engine.Fort remote=new Engine.Fort(900,200,1000,2,Engine.GENERATOR);
  Engine.March nearby=new Engine.March(hostile,remote,500);
  nearby.confusedTime=10;
  areaSpell.marches.add(nearby);
  check(areaSpell.power(0,null,210,200),"meteor targets ground");
  areaSpell.tick(2);
  check(hostile.n==800&&nearby.n==300&&friendly.n==1000,"meteor affects nearby enemies and spares allies");
  check(areaSpell.power(1,null,210,200),"storm targets ground");
  areaSpell.tick(.4f);
  check(hostile.n==750&&nearby.n==250,"storm hits nearby building and marching troop");

  Engine fifth=new Engine(5,0);
  check(fifth.marshes.size()==2,"stage five imports BOTH upper and lower slow regions");
  check(fifth.marshes.stream().anyMatch(area->area.dark&&area.contains(240,60)),"upper stage five marsh exists");
  check(fifth.marshes.stream().anyMatch(area->area.dark&&area.contains(240,250)),"lower stage five marsh exists");
  check(fifth.marshes.stream().noneMatch(area->area.contains(240,155)),"stage five gap remains clear");
  for(int team=1;team<=3;team++)for(float y:new float[]{60,250}){
   Engine terrain=new Engine(5,0);terrain.think=10000;
   Engine.March unit=new Engine.March(new Engine.Fort(240,y,0,team,Engine.GENERATOR),terrain.waypoint(340,y),1000);
   terrain.marches.add(unit);terrain.tick(.1f);
   check(unit.slowed&&Math.abs(unit.x()-242.5f)<.001f,"stage five upper/lower actual half speed for team "+team);
  }
  Engine above=new Engine(5,0);above.think=10000;
  Engine.Fort fromRed=above.forts.get(0),topCrystal=above.forts.get(3);
  above.send(fromRed,topCrystal);Engine.March upperRoute=above.marches.get(0);int upperSteps=0;
  for(int step=0;step<1000&&upperRoute.t<1;step++){
   above.tick(.02f);if(upperRoute.slowed)upperSteps++;
  }
  check(upperSteps>0,"actual red castle to upper crystal route crosses upper marsh");
  System.out.println("Stage 5 upper route: "+upperSteps+" slowed frames");
  Engine nativePivots=new Engine(5,0);
  Engine.Fort redCastle=nativePivots.forts.get(0);
  check(redCastle.visualX==418&&redCastle.visualY==107&&redCastle.x==418&&redCastle.y==121,
   "stage five original artwork and ground positions remain distinct");
  Engine.Fort arrowTower=nativePivots.forts.stream().filter(f->f.kind==Engine.ARCHERY).findFirst().get();
  check(arrowTower.y==160&&arrowTower.visualY==123,"tower ground pivot");
  float expectedDistance=-1;
  for(int team=1;team<=3;team++){
   Engine route=new Engine(5,0);route.think=10000;
   Engine.Fort nativeDeparture=route.forts.get(0),destination=route.forts.get(1);
   Engine.Fort departure=new Engine.Fort(nativeDeparture.x,nativeDeparture.y,1000,team,Engine.GENERATOR);
   check(route.send(departure,destination),"team can depart on actual stage five route");
   Engine.March moving=route.marches.get(0);
   int slowFrames=0;
   for(int frame=0;frame<1500&&moving.t<1;frame++){
    route.tick(.02f);
    if(moving.slowed)slowFrames++;
   }
   check(slowFrames>0,"team "+team+" crosses actual stage five slowdown polygon");
   if(expectedDistance<0)expectedDistance=slowFrames;
   check(slowFrames==expectedDistance,"all teams receive equal slowdown on the same route");
   System.out.println("Stage 5 team "+team+": "+slowFrames+" slowed frames");
  }
  for(int team=1;team<=3;team++){
   Engine speed=new Engine(1,0);speed.think=10000;
   speed.marshes.clear();
   speed.marshes.add(new Engine.Marsh(200,100,100,100,true));
   Engine.Fort start=new Engine.Fort(200,100,1000,team,Engine.GENERATOR);
   Engine.Fort finish=new Engine.Fort(1000,100,1000,team==1?2:1,Engine.GENERATOR);
   speed.send(start,finish);
   Engine.March unit=speed.marches.get(0);
   speed.tick(.1f);
   check(Math.abs(unit.x()-202.5f)<.001f,"every team moves at half speed");
   if(team==1){
    speed.speedLevel=5;
    float previous=unit.x();
    speed.tick(.1f);
    check(Math.abs(unit.x()-previous-3.75f)<.001f,"red upgrades remain halved in marsh");
   }
  }

  for(int stage=1;stage<=30;stage++){
   Engine terrainTest=new Engine(stage,0);
   for(Engine.Marsh marsh:terrainTest.marshes){
    float insideX=0,insideY=0;boolean found=false;
    float minX=Float.MAX_VALUE,maxX=-Float.MAX_VALUE,minY=minX,maxY=maxX;
    for(int i=0;i<marsh.points.length;i+=2){
     minX=Math.min(minX,marsh.points[i]);maxX=Math.max(maxX,marsh.points[i]);
     minY=Math.min(minY,marsh.points[i+1]);maxY=Math.max(maxY,marsh.points[i+1]);
    }
    for(int yi=1;yi<20&&!found;yi++)for(int xi=1;xi<20&&!found;xi++){
     float x=minX+(maxX-minX)*xi/20,y=minY+(maxY-minY)*yi/20;
     if(marsh.contains(x,y)){insideX=x;insideY=y;found=true;}
    }
    check(found,"valid terrain polygon at stage "+stage);
    Engine probe=new Engine(1,0);probe.think=100;probe.marshes.clear();probe.marshes.add(marsh);
    Engine.Fort start=new Engine.Fort(insideX,insideY,1000,1,Engine.GENERATOR);
    Engine.Fort end=new Engine.Fort(insideX+1000,insideY,1000,2,Engine.GENERATOR);
    probe.send(start,end);probe.tick(.001f);
    check(marsh.dark?probe.marches.get(0).slowed:probe.marches.get(0).poisoned,"terrain status stage "+stage);
   }
  }
  Engine orders=new Engine(1,0);orders.think=10000;
  orders.forts.clear();
  Engine.Fort one=new Engine.Fort(0,0,1000,1,Engine.GENERATOR);
  Engine.Fort two=new Engine.Fort(0,100,800,1,Engine.CRYSTAL);
  Engine.Fort opponent=new Engine.Fort(1000,100,1000,2,Engine.GENERATOR);
  orders.forts.add(one);orders.forts.add(two);orders.forts.add(opponent);
  check(orders.command(java.util.Arrays.asList(one,two),java.util.Collections.emptyList(),null,200,200)==0,
   "selected castles cannot deploy to ground");
  check(one.n==1000&&two.n==800&&orders.marches.isEmpty(),"invalid destination preserves garrisons");
  check(orders.command(java.util.Arrays.asList(one,two),java.util.Collections.emptyList(),opponent,0,0)==2,
   "selected castles all deploy to a building");
  check(one.n==500&&two.n==400,"group command deploys half of each garrison");
  Engine.March redirected=orders.marches.get(0),second=orders.marches.get(1);
  orders.tick(.2f);float oldX=redirected.x(),oldY=redirected.y();
  redirected.buff=2;redirected.buffTime=20;
  check(orders.command(java.util.Collections.emptyList(),java.util.Arrays.asList(redirected,second),two,0,0)==2,
   "selected moving armies both redirect to a friendly building");
  check(redirected.x()==oldX&&redirected.y()==oldY&&redirected.b==two&&redirected.n==500&&redirected.buff==2,
   "redirect keeps position, count and active buffs");
  check(second.originKind==Engine.CRYSTAL,"redirection preserves origin troop type");
  Engine.Fort point=orders.waypoint(redirected.x(),redirected.y());
  check(!orders.redirect(redirected,point)&&redirected.b==two,"ground redirection is rejected without losing original destination");
  orders.energy=10000;orders.skillLevels[3]=5;
  check(summonAndArrive(orders,opponent)&&opponent.fight.troops[1]==800&&opponent.n==1000,
   "upgraded reinforcement starts enemy siege without instant damage");
  orders.cooldown[3]=0;
  check(summonAndArrive(orders,opponent)&&opponent.fight.troops[1]==1600,"reinforcement joins existing enemy siege");
  orders.cooldown[3]=0;summonAndArrive(orders,one);
  check(one.n==1300,"friendly reinforcement can exceed cap");
  orders.cooldown[3]=0;
  Engine.Fort neutralTarget=new Engine.Fort(400,400,500,0,Engine.GENERATOR);
  check(summonAndArrive(orders,neutralTarget)&&neutralTarget.fight.troops[1]==800&&neutralTarget.n==500,
   "reinforcement can start siege at neutral castle");
  check(new Engine(20,0).aiClocks[2]!=new Engine(20,0).aiClocks[3],"nations have independent seeded turn timers");
  for(int level=0;level<=5;level++){
   java.util.Arrays.fill(orders.skillLevels,level);
   check(orders.spellDetails(3).startsWith(orders.reinforcementAmount()+" troops"),"reinforcement tooltip uses actual upgrade");
   check(orders.spellDetails(0).contains(String.format(java.util.Locale.US,"%.0f",40*(1+.15f*level))),"meteor tooltip uses actual damage");
   check(orders.spellDetails(1).contains(String.format(java.util.Locale.US,"%.1fs",10*(1+.15f*level))),"storm tooltip uses actual duration");
  }
  Engine originalAI=new Engine(30,0,123);originalAI.forts.clear();
  Engine.Fort strongest=new Engine.Fort(0,0,2000,2,Engine.GENERATOR);
  Engine.Fort weak=new Engine.Fort(100,0,400,2,Engine.GENERATOR);
  Engine.Fort nearest=new Engine.Fort(250,0,500,0,Engine.GENERATOR);
  Engine.Fort far=new Engine.Fort(500,0,1,1,Engine.GENERATOR);
  originalAI.forts.add(strongest);originalAI.forts.add(weak);originalAI.forts.add(nearest);originalAI.forts.add(far);
  originalAI.takeTurn(2);
  check(!originalAI.marches.isEmpty()&&originalAI.marches.get(0).a==strongest&&originalAI.marches.get(0).b==nearest,
   "recovered AI selects strongest source and nearest other castle");
  check(weak.n==400,"AI retains garrison below original threshold");
  for(int i=0;i<100;i++)check(originalAI.aiDelay()>=5&&originalAI.aiDelay()<=10,"late original AI timer range");
  Engine earlyAI=new Engine(1,0);
  for(int i=0;i<100;i++)check(earlyAI.aiDelay()>=9.8&&earlyAI.aiDelay()<15,"early original AI timer range");
  Engine restrictions=new Engine(1,0);restrictions.think=10000;
  Engine.Fort held=restrictions.forts.get(0),safe=restrictions.forts.get(2),enemyGate=restrictions.forts.get(1);
  safe.side=1;held.n=1200;safe.n=1000;
  held.fight=new Engine.Fight(new Engine.March(enemyGate,held,500));held.fight.troops[2]=500;
  check(!restrictions.send(held,enemyGate)&&held.n==1200,"besieged garrison cannot deploy");
  check(restrictions.command(java.util.Arrays.asList(held,safe),java.util.Collections.emptyList(),enemyGate,0,0)==1,
   "group order skips besieged source and dispatches healthy source");
  check(held.n==1200&&safe.n==500,"siege restriction preserves blocked garrison");
  for(int kind=0;kind<3;kind++)for(char size:new char[]{'s','m','b'}){
   Engine.Fort capped=new Engine.Fort(0,0,0,1,kind);capped.size=size;
   int capacity=Engine.capacity(capped);capped.n=capacity*2;
   Engine.March arrival=new Engine.March(new Engine.Fort(-100,0,0,1,0),capped,100);
   restrictions.arrive(arrival);
   check(capped.n==capacity*1.5f,"friendly arrival enforces 1.5x cap for every building type/size");
  }
  Engine.Fort conquered=new Engine.Fort(0,0,10,2,Engine.GENERATOR);conquered.size='s';
  Engine.March invading=new Engine.March(new Engine.Fort(100,0,0,1,0),conquered,10000);
  restrictions.arrive(invading);
  check(conquered.fight.troops[1]==10000,"invasion ignores reinforcement cap");
  conquered.fight.advance(conquered,1,new int[4],new int[4]);
  check(conquered.side==1&&conquered.n==9999,"capture result preserves survivors beyond 1.5x cap");
  restrictions.arrive(new Engine.March(new Engine.Fort(100,0,0,1,0),conquered,1));
  check(conquered.n==1500,"next friendly arrival enforces cap even after over-cap capture");
  conquered.n=1400;conquered.fight=new Engine.Fight(invading);conquered.fight.troops[2]=5000;
  restrictions.arrive(new Engine.March(new Engine.Fort(100,0,0,1,0),conquered,500));
  check(conquered.n==1900&&conquered.fight.troops[2]==5000,"friendly siege reinforcement bypasses cap without changing invaders");
  restrictions.arrive(new Engine.March(new Engine.Fort(100,0,0,1,0),conquered,1000));
  check(conquered.n==2900,"repeated friendly arrivals during raids remain uncapped");
  conquered.fight=null;
  restrictions.arrive(new Engine.March(new Engine.Fort(100,0,0,1,0),conquered,1));
  check(conquered.n==1500,"first post-raid friendly arrival enforces cap");
  conquered.fight=new Engine.Fight(invading);conquered.n=1500;
  restrictions.energy=10000;
  check(summonAndArrive(restrictions,conquered)&&conquered.n==1600,"reinforcement spell also bypasses cap during raid");
  for(int buff=1;buff<=2;buff++){
   Engine expiration=new Engine(1,0);expiration.think=10000;
   Engine.March unit=new Engine.March(new Engine.Fort(-1000,-1000,0,1,0),expiration.forts.get(1),1000);
   expiration.marches.add(unit);unit.buff=buff;unit.buffTime=buff==1?15:30;unit.confusedTime=100;
   expiration.tick(unit.buffTime-.1f);
   check(unit.buff==buff,"altar buff lasts until original deadline");
   expiration.tick(.11f);
   check(unit.buff==0&&unit.buffTime==0,"angel/shield buff expires after 15/30 seconds");
  }
  Engine merge=new Engine(1,0);merge.think=10000;
  Engine.March veteran=new Engine.March(merge.forts.get(0),merge.forts.get(1),1000);
  Engine.March hostileUnit=new Engine.March(merge.forts.get(1),merge.forts.get(0),1000);
  veteran.buff=1;veteran.buffTime=10;
  Engine.Encounter clash=new Engine.Encounter(0,0);clash.add(veteran);clash.add(hostileUnit);
  Engine.March ordinary=new Engine.March(merge.forts.get(0),merge.forts.get(1),300);clash.add(ordinary);
  check(veteran.buff==0&&veteran.buffTime==0,"ordinary reinforcements replace old encounter immunity");
  Engine.March shielded=new Engine.March(merge.forts.get(0),merge.forts.get(1),300);
  shielded.buff=2;shielded.buffTime=4;shielded.buffAge=26;clash.add(shielded);
  check(veteran.buff==2&&veteran.buffTime==4&&ordinary.buffTime==4,"new arrivals replace the whole team's buff with remaining duration");
  merge.encounters.add(clash);merge.marches.addAll(clash.units);merge.tick(.5f);
  check(veteran.buffTime==3.5f&&shielded.buffTime==3.5f,"merged buff timer continues without refresh");
  Engine.Fort siegeGate=merge.forts.get(1);siegeGate.fight=null;
  Engine.March angelRaider=new Engine.March(merge.forts.get(0),siegeGate,500);angelRaider.buff=1;angelRaider.buffTime=8;
  merge.arrive(angelRaider);
  check(siegeGate.fight.buffs[1]==1&&siegeGate.fight.buffTimes[1]==8,"siege receives only remaining buff duration");
  merge.arrive(new Engine.March(merge.forts.get(0),siegeGate,200));
  check(siegeGate.fight.buffs[1]==0&&siegeGate.fight.buffTimes[1]==0,"ordinary siege reinforcements clear immunity");
  for(int kind=0;kind<3;kind++){
   Engine pace=new Engine(1,0);pace.think=10000;
   Engine.March unit=new Engine.March(new Engine.Fort(0,0,0,1,kind),pace.forts.get(1),1000);
   unit.b=new Engine.Fort(1000,0,0,2,0);unit.d=1000;pace.marches.add(unit);pace.tick(.5f);
   check(Math.abs(unit.x()-(kind==Engine.ARCHERY?30:25))<.001f,"configured 50/60 map-unit movement speed");
  }
  Engine crossing=new Engine(1,0);crossing.think=10000;
  crossing.marshes.add(new Engine.Marsh(120,0,40,20,false));
  Engine.March traveler=new Engine.March(new Engine.Fort(0,0,0,1,0),new Engine.Fort(1000,0,0,2,0),1000);
  crossing.marches.add(traveler);
  for(int step=0;step<85;step++)crossing.tick(.05f);
  check(traveler.n<=640,"80-map-unit poison crossing now lasts long enough for at least two poison ticks");
  int verifiedAreas=0;
  for(int stage=1;stage<=30;stage++)for(Engine.Marsh area:new Engine(stage,0).marshes){
   float minX=Float.MAX_VALUE,maxX=-Float.MAX_VALUE,minY=minX,maxY=maxX;
   for(int i=0;i<area.points.length;i+=2){minX=Math.min(minX,area.points[i]);maxX=Math.max(maxX,area.points[i]);minY=Math.min(minY,area.points[i+1]);maxY=Math.max(maxY,area.points[i+1]);}
   float px=0,py=0;boolean found=false;
   for(int y=1;y<50&&!found;y++)for(int x=1;x<50&&!found;x++){
    float xx=minX+(maxX-minX)*x/50,yy=minY+(maxY-minY)*y/50;
    if(area.contains(xx,yy)){px=xx;py=yy;found=true;}
   }
   check(found,"terrain interior exists");
   for(int side=1;side<=3;side++){
    Engine probe=new Engine(1,0);probe.think=10000;probe.marshes.clear();probe.marshes.add(new Engine.Marsh(area.points,area.dark));
    Engine.March unit=new Engine.March(new Engine.Fort(px,py,0,side,Engine.GENERATOR),probe.waypoint(px+1000,py),1000);
    if(!area.dark)unit.confusedTime=10;
    probe.marches.add(unit);probe.tick(.01f);
    if(area.dark){
     check(unit.slowed,"all teams show slow in each original area");
     check(Math.abs(unit.x()-px-.25f)<.015f,"all original slow polygons actually halve movement");
    }else{
     probe.tick(.99f);
     check(unit.poisoned&&unit.n==800,"all original poison polygons damage each team at one second");
    }
   }
   verifiedAreas++;
  }
  System.out.println("Verified actual speed/damage in "+verifiedAreas+" original terrain polygons × 3 teams");
  for(int stage=1;stage<=30;stage++){
   Engine mission=new Engine(stage,0);
   for(int i=0;i<500;i++)mission.tick(.05f);
   for(Engine.Fort fort:mission.forts)check(Float.isFinite(fort.n)&&fort.n>=0,"valid garrison");
  }
  System.out.println("Recovered IPA rule checks passed");
 }
}
