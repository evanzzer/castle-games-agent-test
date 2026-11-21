package castle;
import java.util.*;
public final class WildsCheck {
 static void check(boolean okay,String message){if(!okay)throw new AssertionError(message);}
 public static void main(String[] args){
  check(Engine.TOTAL_STAGES==90&&Wilds.NAMES.length==30,"ninety stages with third campaign");
  Set<String> names=new HashSet<>(),layouts=new HashSet<>(),shapes=new HashSet<>();int count=0;
  for(int stage=61;stage<=90;stage++){
   Engine engine=new Engine(stage,0);names.add(Wilds.NAMES[stage-61]);layouts.add(Wilds.BUILDINGS[stage-61]);shapes.add(Wilds.MARSHES[stage-61]);
   check(engine.forts.stream().filter(f->f.side==1).count()==3,"three starting player castles");
   check(engine.forts.stream().allMatch(f->f.n==500&&f.x>20&&f.x<940&&f.y>20&&f.y<620),"valid starting counts and positions");
   for(int i=0;i<engine.forts.size();i++)for(int j=i+1;j<engine.forts.size();j++)
    check(Math.hypot(engine.forts.get(i).x-engine.forts.get(j).x,engine.forts.get(i).y-engine.forts.get(j).y)>50,"building targets remain separated");
   for(Engine.Marsh area:engine.marshes){
    check(area.points.length>=90,"new organic boundaries rather than reused map polygons");count++;
    boolean found=false;float px=0,py=0;
    for(int y=20;y<620&&!found;y+=6)for(int x=20;x<940&&!found;x+=6)
     if(area.contains(x,y)&&area.contains(x+3,y)&&area.contains(x-3,y)){px=x;py=y;found=true;}
    check(found,"new region has interior");
    for(int side=1;side<=3;side++){
     Engine probe=new Engine(1,0);probe.think=10000;probe.marshes.clear();probe.marshes.add(new Engine.Marsh(area.points,area.dark));
     Engine.March unit=new Engine.March(new Engine.Fort(px,py,0,side,Engine.GENERATOR),new Engine.Fort(px+1000,py,0,side,0),1000);
     if(!area.dark)unit.confusedTime=10;
     probe.marches.add(unit);probe.tick(.01f);
     if(area.dark)check(unit.slowed&&Math.abs(unit.x()-px-.25f)<.02f,"new slow region halves speed for every team");
     else {probe.tick(.99f);check(unit.poisoned&&unit.n==800,"new poison region damages every team");}
    }
   }
   for(int tick=0;tick<1200&&engine.result==0;tick++)engine.tick(.05f);
   for(Engine.Fort fort:engine.forts)check(Float.isFinite(fort.n)&&fort.n>=0,"valid third-campaign simulation");
   check(engine.attackLevels[2]<=5&&engine.defenseLevels[2]<=5&&engine.aiDelay()>=2.2,"bounded late-game difficulty");
  }
  check(names.size()==30&&layouts.size()==30&&shapes.size()==30,"thirty unique named layouts and terrain shapes");
  Engine price=new Engine(90,0);Arrays.fill(price.skillLevels,5);price.nationEnergy[2]=322;
  check(price.power(2,0,null,100,100)&&price.meteors.get(0).damage==52,"third-campaign AI casts level-two spells at 1.4x cost");
  System.out.println("Wilds checks passed: 30 unique levels, "+count+" newly shaped regions tested for all teams; prior spell rules retained.");
 }
}
