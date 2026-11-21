package castle;
public final class FrontierCurveCheck {
 static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
 public static void main(String[] args){int count=0;
  for(int stage=31;stage<=60;stage++)for(Engine.Marsh region:new Engine(stage,0).marshes){
   check(region.points.length>=128,"curved region has dense boundary samples");
   float px=0,py=0;boolean found=false;
   for(int y=15;y<625&&!found;y+=4)for(int x=15;x<945&&!found;x+=4)
    if(region.contains(x,y)&&region.contains(x-3,y)&&region.contains(x+3,y)&&region.contains(x,y-3)&&region.contains(x,y+3)){px=x;py=y;found=true;}
   check(found,"curved region has usable interior");
   for(int side=1;side<=3;side++){
    Engine engine=new Engine(1,0);engine.think=10000;engine.marshes.clear();engine.marshes.add(new Engine.Marsh(region.points,region.dark));
    Engine.March unit=new Engine.March(new Engine.Fort(px,py,0,side,0),new Engine.Fort(px+1000,py,0,side,0),1000);
    if(!region.dark)unit.confusedTime=10;
    engine.marches.add(unit);engine.tick(.01f);
    if(region.dark)check(unit.slowed&&Math.abs(unit.x()-px-.25f)<.02f,"curved marsh halves speed for every team");
    else{engine.tick(.99f);check(unit.poisoned&&unit.n==800,"curved poison deals correct tick damage for every team");}
   }
   count++;
  }
  check(count==59,"all restored region roles preserved");
  System.out.println("Curved Frontier effects passed: 59 regions x 3 teams.");
 }
}
