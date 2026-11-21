package castle;
import java.util.*;
/** A platform-free match aggregate that owns simulation state and gameplay rules. */
final class Engine {
 static final float RECRUIT_INTERVAL=1.25f, COMBAT_INTERVAL=1f;
 static final float MARCH_SPEED=50f, ARCHERY_MARCH_SPEED=60f;
 static final float ANGEL_DURATION=15f, SHIELD_DURATION=30f;
 static final float REINFORCEMENT_RADIUS=100f;
 static final int RECRUIT_AMOUNT=50, TROOP_SCALE=100, TOTAL_STAGES=Campaign.TOTAL_STAGES;
 static final int GENERATOR=0, CRYSTAL=1, ARCHERY=2;
 static final String[] BUILDINGS={"Castle","Crystal","Archer tower"};
 static final String[] ITEMS={"Meteor","Tornado","Mutiny","Reinforce"};
 static final int[] COST={230,300,200,450};
 String spellDetails(int k){
  int level=skillLevels[k];
  switch(k){
   case 0:return String.format(Locale.US,"5 fireballs × %.0f damage · enemies in a 100×100 area",40*(1+.15f*level));
   case 1:return String.format(Locale.US,"%.0f damage / 0.4s · %.1fs · moving 50×50 area",50*(1+.15f*level),10*(1+.15f*level));
   case 2:return String.format(Locale.US,"Stops enemy troops for %.2fs · 100×100 area",5*(1+.15f*level));
   default:return String.format(Locale.US,"%d troops · %d groups × %d · target any castle",reinforcementAmount(),reinforcementGroups(level),reinforcementPerGroup(level));
  }
 }
 int reinforcementAmount(){return reinforcementAmount(skillLevels[3]);}
 static int reinforcementGroups(int level){return 1+(level+1)/2;}
 static int reinforcementPerGroup(int level){return 100+20*level;}
 static int reinforcementAmount(int level){return reinforcementGroups(level)*reinforcementPerGroup(level);}
 static int soldierCount(int troops){return Math.min(25,Math.max(0,(troops+59)/60));}
 float spellCooldown(int k){return new int[]{10,8,15,8}[k]/(1+.1f*cooldownLevel);}
 static int capacity(Fort f){
  if(f.kind==CRYSTAL)return 1000;
  if(f.kind==ARCHERY)return f.size=='b'?1500:f.size=='s'?500:1000;
  return f.size=='b'?3000:f.size=='s'?1000:2000;
 }
 static int arrowDamage(Fort f){
  return Math.max(1,Math.round(Math.min(f.n,capacity(f))*.175f));
 }
 /** A building garrison with its own recruitment clock and optional siege. */
 static class Fort {
  boolean waypoint;
  Fight fight;
  float x,y,n,visualX,visualY; int side,kind; char size='m';
  float recruitTime, reload, flash, shotX, shotY;
  Fort(float x,float y,float n,int side,int kind){this.x=x;this.y=y;visualX=x;visualY=y;this.n=n;this.side=side;this.kind=kind;}
 }
 /** A moving army retaining its origin type, destination, buffs and raid identity. */
 static class March {
  Encounter encounter;
  Object raidWave=new Object();
  float previousX,previousY,poisonFlash;
  Fort a,b; final int originKind; int side,n; int buff;
  float buffTime,buffAge,age,mutinyAge=100;
  float t,d,poisonTime,confusedTime;
  boolean poisoned, slowed;
  float x(){return encounter!=null?encounter.x:a.x+(b.x-a.x)*t;}
  float y(){return encounter!=null?encounter.y:a.y+(b.y-a.y)*t;}
  March(Fort a,Fort b,int n){this.a=a;this.b=b;this.n=n;side=a.side;originKind=a.kind;d=Math.max(.01f,(float)Math.hypot(b.x-a.x,b.y-a.y));}
 }
 /** A multi-team siege that computes each tick from the pre-tick strengths. */
 static final class Fight {
  final Set<Object> raidWaves=Collections.newSetFromMap(new IdentityHashMap<Object,Boolean>());
  final float[] troops=new float[4];
  final int[] originKinds=new int[4],buffs=new int[4];
  final float[] buffTimes=new float[4];
  float elapsed;
  int attacker;
  boolean fromRight;

  Fight(March first){
   attacker=first.side;
   fromRight=first.a.x>first.b.x;
   originKinds[first.side]=first.originKind;
  }

  void selectAttacker(){
   if(troops[attacker]>0)return;
   for(int side=1;side<troops.length;side++){
    if(troops[side]>0){attacker=side;return;}
   }
  }

  // Siege::TroopBattle, ARMv7 0x2957c. Every attack uses pre-tick strength.
  void advance(Fort fort,float dt,int[] attackLevels,int[] defenseLevels){
   for(int side=1;side<4;side++){
    buffTimes[side]=Math.max(0,buffTimes[side]-dt);
    if(buffTimes[side]==0)buffs[side]=0;
   }
   elapsed+=dt;
   while(elapsed>=COMBAT_INTERVAL&&fort.fight==this){
    elapsed-=COMBAT_INTERVAL;
    int defender=fort.side;
    fort.n+=troops[defender];
    troops[defender]=0;
    float garrison=fort.n;
    float incoming=0;
    for(int side=1;side<troops.length;side++){
     if(troops[side]<=0)continue;
     float outgoing=siegeAttack(troops[side],originKinds[side],attackLevels[side]);
     float losses=buffs[side]==1?0:garrison*.1f/(1+.2f*defenseLevels[side]+(buffs[side]==2?.5f:0));
     troops[side]=Math.max(0,troops[side]-losses);
     incoming+=outgoing;
    }
    fort.n=Math.max(0,garrison-incoming/(1+.2f*defenseLevels[defender]));
    selectAttacker();
    if(fort.n==0&&troops[attacker]>0){
     fort.side=attacker;
     fort.n=troops[attacker];
     troops[attacker]=0;
     selectAttacker();
    }
    if(troops[attacker]<=0){fort.fight=null;fort.recruitTime=0;}
   }
  }
 }
 static float siegeAttack(float troops,int originKind,int attackLevel){
  float base=troops<50?troops:Math.max(50,troops*.1f);
  float originBonus=originKind==GENERATOR?1:1.2f;
  return base*originBonus*(1+.2f*attackLevel);
 }

 /** A shared field battle that holds armies in place and combines survivor identity. */
 static final class Encounter {
  final Object combinedWave=new Object();
  final Set<Object> raidWaves=Collections.newSetFromMap(new IdentityHashMap<Object,Boolean>());
  final float x,y;
  final ArrayList<March> units=new ArrayList<>();
  float clock;
  Encounter(float x,float y){this.x=x;this.y=y;}
  void add(March unit){
   // Battle::AddUnit replaces the same nation's Troop_Prop with the incoming one.
   // Counts combine; the arriving group's buff and remaining timer take precedence.
   for(March present:units)if(present.side==unit.side){
    present.buff=unit.buff;present.buffTime=unit.buffTime;present.buffAge=unit.buffAge;
   }
   raidWaves.add(unit.raidWave);
   units.add(unit);unit.encounter=this;unit.raidWave=combinedWave;
  }
  float total(int side){
   float count=0;
   for(March unit:units)if(unit.side==side)count+=unit.n;
   return count;
  }
  int teams(){
   int count=0;
   for(int side=1;side<4;side++)if(total(side)>0)count++;
   return count;
  }
  void tick(float dt,int[] attackLevels,int[] defenseLevels){
   clock+=dt;
   while(clock>=COMBAT_INTERVAL&&teams()>1){
    clock-=COMBAT_INTERVAL;
    int[] losses=new int[units.size()];
    for(int i=0;i<units.size();i++){
     March target=units.get(i);
     if(target.n<=0)continue;
     float damage=0;
     for(March attacker:units)if(attacker.side!=target.side&&attacker.n>0){
      float base=attacker.n<50?attacker.n:Math.max(10,attacker.n*.1f);
      float type=attacker.originKind==GENERATOR?1:1.2f;
      damage+=base*type*(1+.2f*attackLevels[attacker.side]);
     }
     float defense=1+.2f*defenseLevels[target.side]+(target.originKind==GENERATOR?0:.2f)+(target.buff==2?.5f:0);
     if(target.buff==1)damage=0;
     losses[i]=Math.min(target.n,Math.round(damage/defense));
    }
    for(int i=0;i<units.size();i++)units.get(i).n-=losses[i];
   }
  }
 }
 static final class Altar {
  final float x,y;
  final int buff;
  float cooldown;
  Altar(float x,float y,int buff){this.x=x;this.y=y;this.buff=buff;}
 }
 final ArrayList<Altar> altars=new ArrayList<>();
 /** A polygonal slow or poison region with an independent damage clock. */
 static final class Marsh {
  final float[] points;
  final boolean dark;
  float clock;

  Marsh(float[] points,boolean dark){this.points=points;this.dark=dark;}
  Marsh(float x,float y,float rx,float ry,boolean dark){
   this(new float[]{x-rx,y-ry,x+rx,y-ry,x+rx,y+ry,x-rx,y+ry},dark);
  }
  boolean contains(float x,float y){
   boolean inside=false;
   for(int i=0,j=points.length-2;i<points.length;j=i,i+=2){
    float xi=points[i],yi=points[i+1],xj=points[j],yj=points[j+1];
    float dx=xj-xi,dy=yj-yi,length=dx*dx+dy*dy;
    float projection=length==0?-1:((x-xi)*dx+(y-yi)*dy)/length;
    if(projection>=0&&projection<=1&&Math.abs((x-xi)*dy-(y-yi)*dx)<.0001f)return true;
    if((yi>y)!=(yj>y)&&x<(xj-xi)*(y-yi)/(yj-yi)+xi)inside=!inside;
   }
   return inside;
  }
 }
 final ArrayList<Encounter> encounters=new ArrayList<>();
 final ArrayList<Marsh> marshes=new ArrayList<>();
 final ArrayList<Fort> forts=new ArrayList<>();
 static final class Meteor {
  final int side;
  final float areaX,areaY,impactX,impactY,duration,damage;
  float age;
  boolean exploded;
  Meteor(int side,float x,float y,float impactX,float impactY,float duration,float damage){
   this.side=side;areaX=x;areaY=y;this.impactX=impactX;this.impactY=impactY;
   this.duration=duration;this.damage=damage;
  }
 }
 static final class Storm {
  final int side;
  final float centerX,centerY,duration,damage;
  float age,clock;
  Storm(int side,float x,float y,int level){
   this.side=side;centerX=x;centerY=y;duration=10*(1+.15f*level);damage=50*(1+.15f*level);
  }
  float x(){return centerX+(float)Math.sin(age*2)*15;}
  float y(){return centerY+(float)Math.cos(age*2)*15;}
 }
 final ArrayList<Meteor> meteors=new ArrayList<>();
 final ArrayList<Storm> storms=new ArrayList<>();
 static final class Arrow {
  final float fromX,fromY,toX,toY;
  float elapsed;
  Arrow(Fort tower,March target){
   fromX=tower.x;fromY=tower.y-30;
   toX=target.x();toY=target.y()-10;
  }
 }
 final ArrayList<Arrow> arrows=new ArrayList<>();
 static final class CastNotice {
  final int side,spell;final float x,y;float age;
  CastNotice(int side,int spell,float x,float y){this.side=side;this.spell=spell;this.x=x;this.y=y;}
 }
 final ArrayList<CastNotice> castNotices=new ArrayList<>();
 /** A platform-free playback event; the Android audio adapter owns the actual stream. */
 static final class AudioCue {
  final String name;final Object owner;final boolean loop,stop;
  AudioCue(String name){this(name,null,false,false);}
  AudioCue(String name,Object owner,boolean loop,boolean stop){this.name=name;this.owner=owner;this.loop=loop;this.stop=stop;}
 }
 final ArrayList<AudioCue> audioCues=new ArrayList<>();
 static final class Death {final float x,y,drift;final int variant;float age;
  Death(float x,float y,float drift,int variant){this.x=x;this.y=y;this.drift=drift;this.variant=variant;}}
 final ArrayList<Death> deaths=new ArrayList<>();
 final Random effectRandom=new Random(410784366L);
 void casualtyEffect(float x,float y,float loss){
  if(loss<=0||deaths.size()>=64)return;
  for(int i=0;i<Math.min(3,Math.max(1,(int)Math.ceil(loss/200)));i++)
   deaths.add(new Death(x-14+effectRandom.nextFloat()*28,y-6+effectRandom.nextFloat()*12,
    -10+effectRandom.nextFloat()*20,1+effectRandom.nextInt(2)));
 }
 final float[] nationEnergy={0,0,200,200};
 final float[][] nationCooldown=new float[4][4];
 final int[] aiCasts=new int[4];
 int raidSerial;
 float crystalClock;
 final ArrayList<March> marches=new ArrayList<>();
 final int[] skillLevels=new int[4];
 int speedLevel, crystalLevel, cooldownLevel;
 final int[] attackLevels=new int[4],defenseLevels=new int[4];
 final float[] cooldown=new float[4],aiClocks=new float[4];
 final int stage,rank;
 final Campaign campaign;
 final Random aiRandom,spellRandom;
 float time,think=2.5f,energy=200; int result;
 Engine(int stage,int rank){
  this(stage,rank,0xCA571E00L+stage);
 }
 Engine(int stage,int rank,long seed){
  StageDefinition definition=StageDefinition.get(stage);
  campaign=definition.campaign;
  this.stage=stage;this.rank=rank;
  aiRandom=new Random(seed^0x4D41524348L);
  spellRandom=new Random(seed^0x5350454c4cL);
  aiClocks[2]=aiDelay();aiClocks[3]=aiDelay();think=Math.min(aiClocks[2],aiClocks[3]);
  if(campaign.strategicAI)for(int side=2;side<=3;side++){
   attackLevels[side]=Math.min(5,1+(stage-31)/12);defenseLevels[side]=Math.min(5,(stage-31)/12);
  }
  definition.populate(this);
 }

 boolean send(Fort a,Fort b){
  if(result!=0||a==null||b==null||b.waypoint||a==b||a.side==0||a.n<2||a.fight!=null)return false;
  int n=(int)a.n/2;a.n-=n;marches.add(new March(a,b,n));return true;
 }
 float spellCost(int side,int spell){return side==1?COST[spell]:COST[spell]*campaign.enemySpellCostMultiplier;}
 int aiSpellLevel(){return campaign.enemySpellLevel;}
 float crystals(int side){return side==1?energy:nationEnergy[side];}
 boolean canUseSpell(int spell){return canUseSpell(1,spell);}
 boolean canUseSpell(int side,int spell){
  if(side>1&&!campaign.strategicAI)return false;
  return side>=1&&side<=3&&spell>=0&&spell<COST.length&&result==0&&
   (side==1?cooldown[spell]:nationCooldown[side][spell])<=0&&crystals(side)>=spellCost(side,spell);
 }
 boolean power(int k,Fort b){return b!=null&&power(k,b,b.x,b.y);}
 boolean power(int k,Fort b,float x,float y){return power(1,k,b,x,y);}
 boolean power(int side,int k,Fort b,float x,float y){
  if(!canUseSpell(side,k))return false;
  // Intro has no AI spells; later campaigns use their fixed spell tiers.
  int level=side==1?skillLevels[k]:aiSpellLevel();
  if(k==0&&Float.isFinite(x)&&Float.isFinite(y)){
   for(int i=0;i<5;i++){
    Meteor meteor=new Meteor(side,x,y,x-5+spellRandom.nextFloat()*10,
     y-5+spellRandom.nextFloat()*10,1+spellRandom.nextFloat()*.28f,40*(1+.15f*level));
    meteors.add(meteor);audioCues.add(new AudioCue("fireball",meteor,false,false));
   }
  }else if(k==1&&Float.isFinite(x)&&Float.isFinite(y)){
   Storm storm=new Storm(side,x,y,level);storms.add(storm);
   audioCues.add(new AudioCue("twister",storm,true,false));
  }else if(k==2){
   boolean affected=false;
   for(March march:marches)if(march.side!=side&&march.n>0&&Math.abs(march.x()-x)<50&&Math.abs(march.y()-y)<50){
    march.confusedTime=5*(1+.15f*level);march.mutinyAge=0;affected=true;
   }
   if(!affected)return false;
  }else if(k==3&&b!=null&&!b.waypoint){
   // Skill_Reinforce::Attack (0x2b2c8): 1+(level+1)/2 groups,
   // 100+20*level each. The requested spawn ring is now 100 units.
   float rotation=spellRandom.nextFloat()*(float)(Math.PI*2);
   Object wave=new Object();
   for(int group=0;group<reinforcementGroups(level);group++){
    double angle=rotation+group*Math.PI*2/reinforcementGroups(level);
    Fort origin=new Fort(b.x+REINFORCEMENT_RADIUS*(float)Math.cos(angle),b.y+REINFORCEMENT_RADIUS*(float)Math.sin(angle),0,side,GENERATOR);
    March unit=new March(origin,b,reinforcementPerGroup(level));unit.raidWave=wave;marches.add(unit);
   }
  }else return false;
  if(side==1){energy-=COST[k];cooldown[k]=spellCooldown(k);}
  else{
   nationEnergy[side]-=spellCost(side,k);
   nationCooldown[side][k]=new int[]{10,8,15,8}[k];
   aiCasts[side]++;castNotices.add(new CastNotice(side,k,x,y));
  }
  if(k>=2)audioCues.add(new AudioCue(k==2?"hypnoses":"support"));
  return true;
 }
 // Recovered Nation::AI / GetNpcAI / AIToArchi / AIToTroop deployment rules.
 float aiDelay(){
  if(campaign.strategicAI)return Math.max(2.2f,4.8f-(stage-31)*.065f)+aiRandom.nextFloat()*1.6f;
  float progress=stage/30f;
  return 10-5*progress+aiRandom.nextFloat()*5;
 }
 static float aiDepartureThreshold(Fort fort){return fort.kind==ARCHERY?capacity(fort)*.5f:500;}
 Fort nearestOther(Fort origin,int side){
  Fort target=null;double nearest=Double.POSITIVE_INFINITY;
  for(Fort fort:forts)if(fort.side!=side){
   double distance=Math.hypot(fort.x-origin.x,fort.y-origin.y);
   if(distance<nearest){nearest=distance;target=fort;}
  }
  return target;
 }
 void takeTurn(int side){
  if(campaign.strategicAI){takeFrontierTurn(side);tryAISpell(side);return;}
  ArrayList<Fort> owned=new ArrayList<>();
  for(Fort fort:forts)if(fort.side==side)owned.add(fort);
  for(Fort anchor:owned){
   Fort target=nearestOther(anchor,side);
   if(target==null)continue;
   // Original chooses 1 .. (1 + 3*stage/30), upper bound exclusive.
   int upper=1+(int)(3*Math.min(30,stage)/30f);
   int count=1+(upper>1?aiRandom.nextInt(upper-1):0);
   ArrayList<Fort> strongest=new ArrayList<>(owned);
   strongest.sort((a,b)->Float.compare(b.n,a.n));
   for(int i=0;i<Math.min(count,strongest.size());i++){
    Fort source=strongest.get(i);
    if(source.fight==null&&source.n>=aiDepartureThreshold(source))send(source,target);
   }
  }
  if(!owned.isEmpty())tryAISpell(side);
 }
 float incoming(Fort target,int side,boolean friendly){
  float n=0;
  for(March unit:marches)if(unit.b==target&&unit.n>0&&((unit.side==side)==friendly))n+=unit.n;
  if(target.fight!=null)for(int team=1;team<=3;team++)if((team==side)==friendly)n+=target.fight.troops[team];
  return n;
 }
 void takeFrontierTurn(int side){
  ArrayList<Fort> sources=new ArrayList<>();
  for(Fort fort:forts)if(fort.side==side&&fort.fight==null&&fort.n>=250)sources.add(fort);
  sources.sort((a,b)->Float.compare(b.n,a.n));
  int orders=0,limit=Math.min(4,2+(stage-31)/12);
  for(Fort source:sources){
   Fort best=null;double value=-Double.MAX_VALUE;
   for(Fort target:forts){
    if(target==source)continue;
    float friendly=incoming(target,side,true),hostile=incoming(target,side,false);
    double score;
    if(target.side==side){
     float danger=hostile-target.n-friendly;
     if(danger<50)continue;
     score=650+Math.min(800,danger);
    }else{
     float defending=target.n+hostile*.25f;
     if(friendly>defending*1.5f+150)continue;
     score=source.n*.5+friendly*.6-defending*.65;
     if(target.side==0)score+=100;
     if(target.kind==CRYSTAL)score+=180;
     if(target.kind==GENERATOR)score+=100;
     if(target.kind==ARCHERY&&target.side!=0)score-=100;
     if(target.fight!=null&&friendly>0)score+=140;
    }
    score-=Math.hypot(source.x-target.x,source.y-target.y)*.7;
    // Poison costs troops; slower routes expose attackers to counters and spells.
    for(int sample=1;sample<10;sample++)for(Marsh zone:marshes)
     if(zone.contains(source.x+(target.x-source.x)*sample/10,source.y+(target.y-source.y)*sample/10))score-=zone.dark?9:24;
    score+=aiRandom.nextFloat()*35;
    if(score>value){value=score;best=target;}
   }
   if(best!=null&&(value>-130||source.n>=capacity(source)*.75f)&&send(source,best))orders++;
   if(orders>=limit)break;
  }
 }
 float hostileAreaValue(int side,float x,float y,float radius){
  float score=0;
  for(Fort target:forts)if(Math.abs(target.x-x)<radius&&Math.abs(target.y-y)<radius){
   if(target.side!=0&&target.side!=side)score+=target.n;
   if(target.fight!=null)for(int team=1;team<=3;team++)if(team!=side)score+=target.fight.troops[team];
  }
  for(March target:marches)if(target.side!=side&&target.n>0&&Math.abs(target.x()-x)<radius&&Math.abs(target.y()-y)<radius)score+=target.n;
  return score;
 }
 boolean tryAISpell(int side){
  if(side>1&&!campaign.strategicAI)return false;
  boolean owns=false;for(Fort fort:forts)if(fort.side==side)owns=true;
  if(!owns)return false;
  Fort relief=null;float danger=100;
  for(Fort fort:forts)if(fort.side==side&&fort.fight!=null){
   float pressure=incoming(fort,side,false)-fort.n;
   if(pressure>danger){danger=pressure;relief=fort;}
  }
  if(relief!=null&&canUseSpell(side,3)&&power(side,3,relief,relief.x,relief.y))return true;
  // Stop dangerous approaching armies, without consuming crystals on empty ground.
  March approaching=null;float threat=0;
  for(March unit:marches)if(unit.side!=side&&unit.n>=250&&unit.encounter==null&&unit.b.side==side){
   float score=unit.n-(float)Math.hypot(unit.x()-unit.b.x,unit.y()-unit.b.y);
   if(score>threat){threat=score;approaching=unit;}
  }
  if(approaching!=null&&approaching.confusedTime<=0&&canUseSpell(side,2))
   if(power(side,2,null,approaching.x(),approaching.y()))return true;
  Fort fortress=null;float sustained=500;
  for(Fort fort:forts)if(fort.side!=0&&fort.side!=side){
   float score=hostileAreaValue(side,fort.x,fort.y,25);
   if(score>sustained){sustained=score;fortress=fort;}
  }
  if(fortress!=null&&canUseSpell(side,1))return power(side,1,fortress,fortress.x,fortress.y);
  float best=250,bx=0,by=0;
  for(Fort fort:forts){float score=hostileAreaValue(side,fort.x,fort.y,50);if(score>best){best=score;bx=fort.x;by=fort.y;}}
  for(March unit:marches)if(unit.side!=side&&unit.n>0){float score=hostileAreaValue(side,unit.x(),unit.y(),50);if(score>best){best=score;bx=unit.x();by=unit.y();}}
  return best>250&&canUseSpell(side,0)&&power(side,0,null,bx,by);
 }
 Fort waypoint(float x,float y){Fort point=new Fort(x,y,0,0,GENERATOR);point.waypoint=true;return point;}
 boolean redirect(March unit,Fort target){
  if(result!=0||unit==null||target==null||target.waypoint||!forts.contains(target)||unit.n<=0||unit.encounter!=null||!marches.contains(unit))return false;
  float x=unit.x(),y=unit.y();
  unit.a=new Fort(x,y,0,unit.side,unit.originKind);unit.b=target;unit.t=0;
  unit.d=Math.max(.01f,(float)Math.hypot(target.x-x,target.y-y));
  unit.previousX=x;unit.previousY=y;return true;
 }
 int command(Collection<Fort> buildings,Collection<March> troops,Fort target,float x,float y){
  if(result!=0||!Float.isFinite(x)||!Float.isFinite(y))return 0;
  if(target==null||target.waypoint||!forts.contains(target))return 0;
  int count=0;Object wave=new Object();
  for(March unit:new ArrayList<>(troops))if(unit.side==1&&redirect(unit,target)){unit.raidWave=wave;count++;}
  for(Fort fort:new ArrayList<>(buildings))if(fort.side==1&&send(fort,target)){marches.get(marches.size()-1).raidWave=wave;count++;}
  return count;
 }
 void arrive(March unit){
  Fort target=unit.b;
  if(target.side==unit.side){
   float reinforced=target.n+unit.n;
   target.n=target.fight!=null?reinforced:Math.min(capacity(target)*1.5f,reinforced);
  }
  else{
   if(target.fight==null){target.fight=new Fight(unit);target.recruitTime=0;}
   if(target.fight.raidWaves.add(unit.raidWave))raidSerial++;
   target.fight.troops[unit.side]+=unit.n;
   target.fight.originKinds[unit.side]=unit.originKind;
   target.fight.buffs[unit.side]=unit.buff;
   target.fight.buffTimes[unit.side]=unit.buffTime;
  }
 }
 void damageArea(float x,float y,float halfExtent,float damage){damageArea(1,x,y,halfExtent,damage);}
 void damageArea(int caster,float x,float y,float halfExtent,float damage){
  for(Fort fort:forts){
   if(Math.abs(fort.x-x)>=halfExtent||Math.abs(fort.y-y)>=halfExtent)continue;
   if(fort.side!=caster)fort.n=Math.max(0,fort.n-damage);
   if(fort.fight!=null)for(int side=1;side<4;side++){
    if(side==caster)continue;
    if(fort.fight.buffs[side]==1)continue;
    fort.fight.troops[side]=Math.max(0,fort.fight.troops[side]-
     damage/(fort.fight.buffs[side]==2?1.5f:1));
   }
  }
  for(March unit:marches)if(unit.side!=caster&&Math.abs(unit.x()-x)<halfExtent&&Math.abs(unit.y()-y)<halfExtent)
   damageMarch(unit,damage);
 }
 void tickSpells(float dt){
  for(Iterator<Meteor> it=meteors.iterator();it.hasNext();){
   Meteor meteor=it.next();meteor.age+=dt;
   if(!meteor.exploded&&meteor.age>=meteor.duration){
    meteor.exploded=true;audioCues.add(new AudioCue("fireball",meteor,false,true));audioCues.add(new AudioCue("explosion"));damageArea(meteor.side,meteor.areaX,meteor.areaY,50,meteor.damage);
   }
   if(meteor.age>meteor.duration+.65f)it.remove();
  }
  for(Iterator<Storm> it=storms.iterator();it.hasNext();){
   Storm storm=it.next();
   float active=Math.min(dt,Math.max(0,storm.duration-storm.age));
   storm.age+=active;storm.clock+=active;
   while(storm.clock>=.4f-.00001f){
    storm.clock-=.4f;damageArea(storm.side,storm.x(),storm.y(),25,storm.damage);
   }
   if(storm.age>=storm.duration-.00001f){audioCues.add(new AudioCue("twister",storm,true,true));it.remove();}
  }
 }
 static void damageMarch(March target,float damage){
  if(target.buff==1)return;
  target.n=Math.max(0,Math.round(target.n-damage/(target.buff==2?1.5f:1)));
 }
 void interceptMarches(){
  for(March unit:marches){
   if(unit.encounter!=null||unit.n<=0)continue;
   for(Encounter encounter:encounters){
    if(Math.hypot(unit.x()-encounter.x,unit.y()-encounter.y)<=14){
     boolean newWave=!encounter.raidWaves.contains(unit.raidWave);
     encounter.add(unit);if(newWave)raidSerial++;break;
    }
   }
  }
  for(int i=0;i<marches.size();i++){
   March first=marches.get(i);
   if(first.encounter!=null||first.n<=0)continue;
   for(int j=i+1;j<marches.size();j++){
    March second=marches.get(j);
    if(second.encounter!=null||second.n<=0||first.side==second.side)continue;
    float rx=first.previousX-second.previousX,ry=first.previousY-second.previousY;
    float vx=first.x()-first.previousX-second.x()+second.previousX;
    float vy=first.y()-first.previousY-second.y()+second.previousY;
    float length=vx*vx+vy*vy;
    float t=length==0?0:Math.max(0,Math.min(1,-(rx*vx+ry*vy)/length));
    if(Math.hypot(rx+vx*t,ry+vy*t)>12)continue;
    float x=(first.previousX+(first.x()-first.previousX)*t+
             second.previousX+(second.x()-second.previousX)*t)/2;
    float y=(first.previousY+(first.y()-first.previousY)*t+
             second.previousY+(second.y()-second.previousY)*t)/2;
    Encounter encounter=new Encounter(x,y);
    encounter.add(first);encounter.add(second);
    encounters.add(encounter);raidSerial++;break;
   }
  }
 }
 void updateTerrainStatus(March unit){
  unit.poisoned=false;unit.slowed=false;
  for(Marsh area:marshes)if(area.contains(unit.x(),unit.y())){
   if(area.dark)unit.slowed=true;else unit.poisoned=true;
  }
 }
 void tick(float dt){
  if(result!=0)return;
  time+=dt;think-=dt;
  for(Iterator<Death> it=deaths.iterator();it.hasNext();){Death death=it.next();death.age+=dt;if(death.age>1.1f)it.remove();}
  IdentityHashMap<March,Integer> beforeMarch=new IdentityHashMap<>();
  for(March unit:marches)beforeMarch.put(unit,unit.n);
  IdentityHashMap<Fort,Float> beforeSiege=new IdentityHashMap<>();
  for(Fort fort:forts)if(fort.fight!=null){float strength=fort.n;for(float n:fort.fight.troops)strength+=n;beforeSiege.put(fort,strength);}
  for(int side=2;side<=3;side++)aiClocks[side]-=dt;
  for(March unit:marches){
   unit.previousX=unit.x();unit.previousY=unit.y();
   updateTerrainStatus(unit);
   unit.buffAge+=dt;unit.age+=dt;unit.mutinyAge+=dt;
   unit.confusedTime=Math.max(0,unit.confusedTime-dt);
   unit.buffTime=Math.max(0,unit.buffTime-dt);
   if(unit.buffTime==0)unit.buff=0;
   unit.poisonFlash=Math.max(0,unit.poisonFlash-dt);
  }
  for(Altar altar:altars){
   altar.cooldown=Math.max(0,altar.cooldown-dt);
   if(altar.cooldown>0)continue;
   for(March unit:marches)if(unit.n>0&&unit.buff==0&&Math.hypot(unit.x()-altar.x,unit.y()-altar.y)<35){
    unit.buff=altar.buff;unit.buffTime=altar.buff==1?ANGEL_DURATION:SHIELD_DURATION;unit.buffAge=0;
    altar.cooldown=30;break;
   }
  }
  for(Iterator<Encounter> it=encounters.iterator();it.hasNext();){
   Encounter encounter=it.next();
   encounter.tick(dt,attackLevels,defenseLevels);
   if(encounter.teams()<=1){
    for(March unit:encounter.units){
     // Resume from the meeting point toward the original destination.
     Fort origin=new Fort(encounter.x,encounter.y,0,unit.side,unit.originKind);
     unit.a=origin;unit.t=0;
     unit.d=Math.max(.01f,(float)Math.hypot(unit.b.x-origin.x,unit.b.y-origin.y));
     unit.encounter=null;
    }
    it.remove();
   }
  }
  crystalClock+=dt;
  while(crystalClock>=6){
   crystalClock-=6;
   for(int side=1;side<=3;side++){
    int formations=0;boolean owns=false;
    for(Fort fort:forts)if(fort.side==side){owns=true;if(fort.kind==CRYSTAL)formations++;}
    if(side==1)energy+=(25+40*formations)*(1+.2f*crystalLevel);
    else if(owns)nationEnergy[side]+=25+40*formations;
   }
  }
  for(Iterator<Arrow> it=arrows.iterator();it.hasNext();){
   Arrow arrow=it.next();arrow.elapsed+=dt;
   if(arrow.elapsed>=.2f)it.remove();
  }
  for(int i=0;i<4;i++){
   cooldown[i]=Math.max(0,cooldown[i]-dt);
   for(int side=2;side<=3;side++)nationCooldown[side][i]=Math.max(0,nationCooldown[side][i]-dt);
  }
  for(Iterator<CastNotice> it=castNotices.iterator();it.hasNext();){CastNotice notice=it.next();notice.age+=dt;if(notice.age>2.5f)it.remove();}
  for(Fort f:forts){
   f.flash=Math.max(0,f.flash-dt);
   f.reload=Math.max(0,f.reload-dt);
   if(f.fight!=null)f.fight.advance(f,dt,attackLevels,defenseLevels);
   else if(f.side!=0){
    f.recruitTime+=dt;
    while(f.recruitTime>=RECRUIT_INTERVAL){
     f.recruitTime-=RECRUIT_INTERVAL;
     int cap=capacity(f);
     if(f.n>cap)f.n=Math.max(cap,f.n-RECRUIT_AMOUNT);
     else if(f.kind==GENERATOR)f.n=Math.min(cap,f.n+RECRUIT_AMOUNT);
    }
   }
   if(f.side==0||f.kind!=ARCHERY||f.n<1||f.reload>0)continue;
   March target=null;
   double range=85;
   for(March m:marches)if(m.side!=f.side&&m.n>0){
    double distance=Math.hypot(m.x()-f.x,m.y()-f.y);
    if(distance<range){range=distance;target=m;}
   }
   if(target!=null){
    arrows.add(new Arrow(f,target));audioCues.add(new AudioCue("arrow"));
    damageMarch(target,arrowDamage(f));
    f.shotX=target.x();f.shotY=target.y();f.flash=.23f;f.reload=2.5f-1.25f*Math.min(f.n,capacity(f))/capacity(f);
   }
  }
  tickSpells(dt);
  for(Marsh marsh:marshes){
   marsh.clock+=dt;
   if(marsh.clock<1)continue;
   marsh.clock-=1;
   if(marsh.dark)continue;
   for(March march:marches)if(marsh.contains(march.x(),march.y())){
    damageMarch(march,march.encounter==null?Math.round(march.n*.2f):50);
    march.poisonFlash=.7f;
   }
   for(Fort fort:forts)if(fort.fight!=null&&marsh.contains(fort.x,fort.y)){
    fort.n=Math.max(0,fort.n-50);
    for(int side=1;side<4;side++)
     fort.fight.troops[side]=Math.max(0,fort.fight.troops[side]-50);
   }
  }
  for(Iterator<March> it=marches.iterator();it.hasNext();){
   March m=it.next();
   if(m.n==0){it.remove();continue;}
   if(m.encounter!=null)continue;
   if(m.t<1){
    if(m.confusedTime>0){
     continue;
    }
    float speed=(m.originKind==ARCHERY?ARCHERY_MARCH_SPEED:MARCH_SPEED)*(m.side==1?1+.1f*speedLevel:1);
    // Distance substeps prevent narrow polygon edges from being skipped.
    float remaining=dt;
    while(remaining>.000001f&&m.t<1){
     float slice=Math.min(remaining,1/speed);
     boolean slow=false;
     float sample=Math.min(1,m.t+slice*speed/m.d*.5f);
     for(Marsh marsh:marshes)if(marsh.dark&&marsh.contains(
       m.a.x+(m.b.x-m.a.x)*sample,m.a.y+(m.b.y-m.a.y)*sample))slow=true;
     m.t=Math.min(1,m.t+slice*speed/m.d*(slow?.5f:1));
     remaining-=slice;
    }
    updateTerrainStatus(m);
   }
  }
  for(Map.Entry<March,Integer> entry:beforeMarch.entrySet()){
   March unit=entry.getKey();casualtyEffect(unit.x(),unit.y(),entry.getValue()-unit.n);
  }
  for(Map.Entry<Fort,Float> entry:beforeSiege.entrySet()){
   Fort fort=entry.getKey();float strength=fort.n;
   if(fort.fight!=null)for(float n:fort.fight.troops)strength+=n;
   casualtyEffect(fort.x,fort.y,entry.getValue()-strength);
  }
  interceptMarches();
  for(Iterator<March> it=marches.iterator();it.hasNext();){
   March m=it.next();
   if(m.n<=0){it.remove();continue;}
   if(m.encounter!=null||m.t<1)continue;
   if(m.b.waypoint)continue;
   arrive(m);
   it.remove();
  }
  if(think<=0){
   for(int side=2;side<=3;side++)if(aiClocks[side]<=0){
    takeTurn(side);aiClocks[side]=aiDelay();
   }
   think=Math.min(aiClocks[2],aiClocks[3]);
  }
  boolean player=false,enemy=false;
  for(Fort f:forts){
   player|=f.side==1;enemy|=f.side>1;
   if(f.fight!=null){
    player|=f.fight.troops[1]>0;
    enemy|=f.fight.troops[2]>0||f.fight.troops[3]>0;
   }
  }
  for(March m:marches){player|=m.side==1;enemy|=m.side>1;}
  if(!player)result=2;else if(!enemy)result=1;
 }
}
