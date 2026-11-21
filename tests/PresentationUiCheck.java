package castle;
import android.app.Instrumentation;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.graphics.*;
import java.lang.reflect.Field;
public final class PresentationUiCheck extends Instrumentation {
 void check(boolean b,String text){if(!b)throw new AssertionError(text);}
 void tap(GameView board){
  long time=SystemClock.uptimeMillis();
  for(int action:new int[]{android.view.MotionEvent.ACTION_DOWN,android.view.MotionEvent.ACTION_UP}){
   android.view.MotionEvent event=android.view.MotionEvent.obtain(time,time,action,200*board.scale+board.left,150*board.scale+board.top,0);
   board.onTouchEvent(event);event.recycle();
  }
 }
 void capture(String name)throws Exception{
  SystemClock.sleep(350);runOnMainSync(()->{});SystemClock.sleep(150);
  Bitmap shot=getUiAutomation().takeScreenshot();
  try(java.io.FileOutputStream out=new java.io.FileOutputStream(new java.io.File(getTargetContext().getExternalFilesDir(null),name))){shot.compress(Bitmap.CompressFormat.PNG,100,out);}
 }
 @Override public void onCreate(Bundle args){super.onCreate(args);start();}
 @Override public void onStart(){Bundle result=new Bundle();try{
  Main main=(Main)startActivitySync(new Intent(Intent.ACTION_MAIN).setClassName("game.castlewars.rebuilt","castle.Main").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
  waitForIdleSync();Field field=Main.class.getDeclaredField("board");field.setAccessible(true);GameView board=(GameView)field.get(main);
  runOnMainSync(()->{
   int completed=board.profile.completedStages();ProfileFixtures.completed(board,14);board.mission=15;board.startMission();ProfileFixtures.completed(board,completed);board.rewardGiven=true;board.engine.think=10000;
   Engine e=board.engine;e.forts.clear();e.marches.clear();e.altars.clear();e.marshes.clear();
   for(int i=0;i<7;i++){
    int kind=i<3?0:i<6?2:1;Engine.Fort f=new Engine.Fort(45+i*63,100,500+i*100,1+i%3,kind);
    f.size="bmsbmsm".charAt(i);f.visualY=kind==2?73:90;e.forts.add(f);
   }
   int[] strengths={60,240,600,1500,3000};
   for(int i=0;i<strengths.length;i++){
    Engine.Fort origin=new Engine.Fort(55+i*88,190,0,1+i%3,0);
    Engine.Fort target=new Engine.Fort(origin.x+10,250,0,0,0);
    Engine.March m=new Engine.March(origin,target,strengths[i]);m.age=3;e.marches.add(m);
    if(i==1){m.confusedTime=5;m.mutinyAge=.3f;}
    if(i==2){m.buff=1;m.buffTime=10;}
    if(i==3){m.buff=2;m.buffTime=10;}
   }
   board.cameraX=board.cameraY=0;board.zoom=1;board.lastTime=0;board.paused=false;
   // Freeze simulation while continuing to render the effects.
   e.result=1;board.resultStarted=SystemClock.uptimeMillis()+100000;board.resultDialog=false;
   for(Engine.Fort f:e.forts){check(board.unitRenderer().fortNumberY(f)<f.visualY,"building count above artwork");}
   for(String name:new String[]{"flag_r_b6","flag_b_m6","flag_y_s5","battle1","battle_troop1","soldier_die1","skill_mutiny8","bg_victory","bg_defeat"})check((board.resources().sprite(name)!=null),"missing original frame "+name);
   Engine.Fort big=e.forts.get(3),small=e.forts.get(5);
   check(board.unitRenderer().shadowOffset(big)[0]==42.5f&&board.unitRenderer().shadowOffset(big)[1]==71.78f,"big archer original shadow anchor");
   check(board.unitRenderer().shadowOffset(small)[0]==34&&board.unitRenderer().shadowOffset(small)[1]==62,"small archer original shadow anchor");
   check(board.unitRenderer().soldierVariant(0)==1&&board.unitRenderer().soldierVariant(1)==2&&board.unitRenderer().soldierVariant(2)==3,"original source soldier sets");
   board.invalidate();
  });
  capture("formations-v015.png");
  runOnMainSync(()->{
   Engine e=board.engine;Engine.Fort f=e.forts.get(1);
   e.arrive(new Engine.March(e.forts.get(0),f,850));
   e.arrive(new Engine.March(e.forts.get(2),f,640));
   e.casualtyEffect(f.x,f.y,400);
   board.invalidate();
  });capture("siege-v015.png");
  runOnMainSync(()->{board.engine.result=1;board.resultStarted=SystemClock.uptimeMillis();board.resultDialog=false;tap(board);check(!board.resultDialog,"result delay ignores taps");board.invalidate();});
  SystemClock.sleep(1100);capture("victory-splash-v015.png");
  runOnMainSync(()->{tap(board);check(board.resultDialog,"splash tap opens dialog");board.invalidate();});capture("victory-dialog-v015.png");
  runOnMainSync(()->{board.engine.result=2;board.resultStarted=SystemClock.uptimeMillis()-1100;board.resultDialog=false;board.invalidate();});capture("defeat-splash-v015.png");
  runOnMainSync(()->{
   Engine e=board.engine;e.result=0;e.forts.clear();e.marches.clear();e.deaths.clear();
   for(int kind=0;kind<3;kind++){
    Engine.Fort source=new Engine.Fort(95+kind*145,100,1200,1,kind);source.size='m';source.visualY=kind==2?68:90;e.forts.add(source);
    Engine.Fort target=new Engine.Fort(source.x,255,500,2,0);target.visualY=245;e.forts.add(target);
    check(e.send(source,target),"real deployment from source building");
    Engine.March unit=e.marches.get(kind);unit.t=.4f;unit.age=3;
    check(unit.originKind==kind&&unit.n==600,"source kind persists in equal-strength army");
    String frame="soldier_r_"+board.unitRenderer().soldierVariant(unit.originKind)+"_s1";
    check((board.resources().sprite(frame)!=null),"original source-specific animation exists");
   }
   e.result=1;board.resultStarted=SystemClock.uptimeMillis()+100000;board.resultDialog=false;board.invalidate();
  });capture("troop-origins-v015.png");
  runOnMainSync(()->{board.engine=null;board.screen="menu";board.invalidate();});
  result.putString("stream","Presentation UI passed: original frames, above-building numbers, one-second delay, tap-through splash, six previews including real castle/crystal/archer deployments.\n");finish(-1,result);
 }catch(Throwable failure){result.putString("stream",android.util.Log.getStackTraceString(failure));finish(0,result);}}
}
