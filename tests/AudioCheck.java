package castle;
public final class AudioCheck {
 static void check(boolean b,String s){if(!b)throw new AssertionError(s);}
 static long count(Engine e,String name,boolean stop){return e.audioCues.stream().filter(c->c.name.equals(name)&&c.stop==stop).count();}
 public static void main(String[]args){
  Engine e=new Engine(1,0);e.think=10000;e.energy=10000;e.forts.clear();e.marshes.clear();
  e.forts.add(new Engine.Fort(0,0,500,1,1));e.forts.add(new Engine.Fort(1000,1000,500,2,1));
  check(e.power(0,null,400,400),"meteor casts");
  check(count(e,"fireball",false)==5&&count(e,"explosion",false)==0,"each spawned fireball has launch sound before any impact");
  check(e.audioCues.stream().allMatch(c->c.owner instanceof Engine.Meteor&&!c.loop),"launch sounds are owned one-shots");
  check(e.power(1,null,400,400),"storm casts");Engine.Storm storm=e.storms.get(0);
  check(e.audioCues.stream().anyMatch(c->c.owner==storm&&c.loop&&!c.stop),"twister begins its owned loop");
  for(int i=0;i<30;i++)e.tick(.05f);
  check(count(e,"explosion",false)==5&&count(e,"fireball",true)==5,"impact stops every launch sound and plays explosion once");
  check(count(e,"twister",false)==1&&count(e,"twister",true)==0,"storm does not start sound again on ticks");
  for(int i=0;i<172;i++)e.tick(.05f);
  check(count(e,"twister",true)==1&&e.storms.isEmpty(),"storm ends its audio loop once at expiry");
  e.audioCues.clear();e.forts.clear();e.marches.clear();
  Engine.Fort tower=new Engine.Fort(100,100,500,1,2),enemy=new Engine.Fort(300,100,500,2,0);
  e.forts.add(tower);e.forts.add(enemy);Engine.March invader=new Engine.March(enemy,tower,500);invader.t=.75f;e.marches.add(invader);
  e.tick(.01f);check(count(e,"arrow",false)==1&&e.arrows.size()==1,"tower volley has its own arrow sound");
  e.tick(.01f);check(count(e,"arrow",false)==1,"tower sound waits for its next volley");
  System.out.println("Audio events passed: 5 launches, 5 impacts, owned tornado start/stop, one arrow sound per volley.");
 }
}
