package castle;
import android.app.Instrumentation;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.graphics.Bitmap;
import java.lang.reflect.Field;
import java.util.Map;
public final class RaidUiCheck extends Instrumentation {
 void check(boolean b,String text){if(!b)throw new AssertionError(text);}
 int battleSounds(Map<String,Integer> counts){return counts.getOrDefault("effects/attack1.wav",0)+counts.getOrDefault("effects/attack2.wav",0);}
 Object field(Object o,String name)throws Exception{Field f=o.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(o);}
 void capture(String name)throws Exception{
  SystemClock.sleep(250);runOnMainSync(()->{});SystemClock.sleep(100);
  Bitmap shot=getUiAutomation().takeScreenshot();
  try(java.io.FileOutputStream out=new java.io.FileOutputStream(new java.io.File(getTargetContext().getExternalFilesDir(null),name))){shot.compress(Bitmap.CompressFormat.PNG,100,out);}
 }
 @Override public void onCreate(Bundle args){super.onCreate(args);start();}
 @Override public void onStart(){Bundle result=new Bundle();try{
  Main main=(Main)startActivitySync(new Intent(Intent.ACTION_MAIN).setClassName("game.castlewars.rebuilt","castle.Main").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
  waitForIdleSync();Main.Board board=(Main.Board)field(main,"board");
  Map<String,Integer> counts=(Map<String,Integer>)field(main,"soundStarts");
  Engine.March[] incoming=new Engine.March[2];SystemClock.sleep(1000);
  runOnMainSync(()->{
   board.mission=1;board.startMission();board.rewardGiven=true;board.zoom=1;board.cameraX=board.cameraY=0;
   Engine e=board.engine;e.think=10000;e.forts.clear();e.marshes.clear();e.altars.clear();
   for(int kind=0;kind<3;kind++){
    Engine.Fort f=new Engine.Fort(85+kind*155,170,3000,2,kind);f.visualY=kind==2?128:150;f.size='m';e.forts.add(f);
   }
   Engine.Fort source=new Engine.Fort(250,250,3000,1,0);e.forts.add(source);
   Engine.Encounter combined=new Engine.Encounter(70,210);
   for(int i=0;i<2;i++){incoming[i]=new Engine.March(source,e.forts.get(0),2000);combined.add(incoming[i]);incoming[i].encounter=null;}
   counts.clear();e.arrive(incoming[0]);board.invalidate();
  });SystemClock.sleep(300);
  runOnMainSync(()->check(battleSounds(counts)==1,"first group starts one sound"));
  runOnMainSync(()->{Engine e=board.engine;e.arrive(new Engine.March(e.forts.get(1),e.forts.get(0),400));board.invalidate();});
  SystemClock.sleep(250);
  runOnMainSync(()->check(battleSounds(counts)==1,"friendly castle reinforcement emits no battle sound"));
  SystemClock.sleep(1100);
  runOnMainSync(()->{board.engine.arrive(incoming[1]);board.invalidate();});SystemClock.sleep(250);
  runOnMainSync(()->{
   check(battleSounds(counts)==1,"staggered survivor group does not double sound");
   Engine e=board.engine;e.arrive(new Engine.March(e.forts.get(3),e.forts.get(0),500));board.invalidate();
  });SystemClock.sleep(250);
  runOnMainSync(()->{
   check(battleSounds(counts)==2,"separate attacking wave sounds once again");
   Engine e=board.engine;
   for(int i=1;i<3;i++)e.arrive(new Engine.March(e.forts.get(3),e.forts.get(i),2200));
   e.result=1;board.resultStarted=SystemClock.uptimeMillis()+100000;board.resultDialog=false;
   for(Engine.Fort f:e.forts)if(f.fight!=null)e.casualtyEffect(f.x,f.y,200);
   board.invalidate();
  });
  for(int i=0;i<4;i++){final int frame=i;runOnMainSync(()->{board.engine.time=frame/10f;board.invalidate();});capture("siege-cloud-v016-"+(i+1)+".png");}
  runOnMainSync(()->{board.engine=null;board.screen="menu";board.invalidate();});
  result.putString("stream","Raid UI passed: combined survivors sound once across staggered arrivals; independent raid sounds again; all four raised cloud frames captured.\n");finish(-1,result);
 }catch(Throwable failure){result.putString("stream",android.util.Log.getStackTraceString(failure));finish(0,result);}}
}
