package castle;
public final class PresentationCheck {
 static void check(boolean b,String message){if(!b)throw new AssertionError(message);}
 public static void main(String[]args){
  int[] totals={100,240,280,480,540,800};
  check(Engine.soldierCount(0)==0&&Engine.soldierCount(1)==1&&Engine.soldierCount(60)==1&&Engine.soldierCount(61)==2&&Engine.soldierCount(1500)==25&&Engine.soldierCount(10000)==25,"army silhouette scales and caps");
  for(int level=0;level<=5;level++)for(int side=0;side<=2;side++){
   Engine e=new Engine(1,0);e.forts.clear();e.marshes.clear();e.think=10000;
   Engine.Fort target=new Engine.Fort(250,180,500,side,Engine.CRYSTAL);e.forts.add(target);
   e.forts.add(new Engine.Fort(2000,2000,500,side==1?2:1,Engine.CRYSTAL));
   e.forts.add(new Engine.Fort(3000,3000,500,2,Engine.CRYSTAL));
   e.energy=10000;e.skillLevels[3]=level;
   check(e.power(3,target),"reinforcement cast");check(target.n==500&&target.fight==null,"reinforcement must travel before joining");
   check(e.marches.size()==Engine.reinforcementGroups(level),"original group count");
   check(e.marches.stream().mapToInt(m->m.n).sum()==totals[level],"original total per tier");
   for(Engine.March m:e.marches){check(Math.abs(m.d-100)<.001,"shortened reinforcement ring radius");check(m.n==100+20*level&&m.side==1&&m.b==target,"group identity and target");}
   check(e.audioCues.size()==1&&e.audioCues.get(0).name.equals("support"),"one reinforcement sound event");
   if(level==0){for(int i=0;i<43;i++)e.tick(.05f);check(e.marches.isEmpty(),"reinforcement reaches target by movement");check(side==1?target.n==600:target.fight!=null&&target.fight.troops[1]==100,"friendly arrival or hostile raid");}
  }
  Engine e=new Engine(1,0);e.think=10000;e.energy=10000;
  Engine.March m=new Engine.March(e.forts.get(1),e.forts.get(0),600);e.marches.add(m);
  check(e.power(2,null,m.x(),m.y())&&m.mutinyAge==0&&m.confusedTime==5,"mutiny starts one-shot animation and stop timer");
  float x=m.x(),y=m.y();for(int i=0;i<30;i++)e.tick(.05f);
  check(m.mutinyAge>1.35f&&m.confusedTime>3&&m.x()==x&&m.y()==y,"mutiny animation fades before stun expires");
  check(e.audioCues.size()==1&&e.audioCues.get(0).name.equals("hypnoses"),"mutiny sound does not repeat on tick");
  e.power(0,null,m.x(),m.y());e.power(1,null,m.x(),m.y());
  check(e.audioCues.stream().anyMatch(c->c.name.equals("fireball"))&&e.audioCues.stream().anyMatch(c->c.name.equals("twister")),"meteor and storm sounds");
  for(int i=0;i<30;i++)e.tick(.05f);
  check(e.audioCues.stream().filter(c->c.name.equals("explosion")).count()==5,"one impact sound per fireball");
  check(!e.deaths.isEmpty(),"area spell casualties produce original death effects");
  System.out.println("Presentation mechanics passed: formations, all reinforcement tiers and arrivals, mutiny fade timer, one-shot spell audio, casualty effects.");
 }
}
