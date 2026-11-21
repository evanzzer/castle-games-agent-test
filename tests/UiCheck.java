package castle;

import android.app.Instrumentation;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.MotionEvent;
import java.lang.reflect.Field;

/** Runs real Board touch handlers inside the Android process. */
public final class UiCheck extends Instrumentation {
 static void check(boolean okay,String message){if(!okay)throw new AssertionError(message);}
 void touch(Main.Board board,int action,float x,float y){
  MotionEvent event=MotionEvent.obtain(SystemClock.uptimeMillis(),SystemClock.uptimeMillis(),action,
   board.left+x*board.scale,board.top+y*board.scale,0);
  board.onTouchEvent(event);event.recycle();
 }
 @Override public void onCreate(Bundle arguments){super.onCreate(arguments);start();}
 @Override public void onStart(){
  Bundle result=new Bundle();
  try{
   Main main=(Main)startActivitySync(new Intent(Intent.ACTION_MAIN).setClassName("game.castlewars.rebuilt","castle.Main").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
   waitForIdleSync();
   runOnMainSync(()->{
    try{
     Field field=Main.class.getDeclaredField("board");field.setAccessible(true);
     Main.Board board=(Main.Board)field.get(main);
     int savedCompleted=board.completed,savedPoints=board.points;
     board.completed=0;board.engine=null;board.screen="campaign";board.mission=2;
     board.startMission();check(board.engine==null,"locked stage cannot start through play handler");
     board.mission=1;
     org.json.JSONArray stageTwo=board.markers.optJSONArray(1);
     board.click((float)stageTwo.optDouble(0)-board.mapScroll,board.campaignY((float)stageTwo.optDouble(1)));
     check(board.mission==1,"locked marker cannot be selected");
     board.startMission();board.engine.result=2;
     board.battleClick(280,190);
     check(board.mission==1&&board.engine.result==2&&board.completed==0,"defeat cannot advance or unlock next stage");
     board.engine.result=1;board.rewardGiven=false;board.resultStarted=SystemClock.uptimeMillis()-1100;board.resultDialog=false;board.battleClick(200,150);
     board.battleClick(280,190);
     check(board.mission==2&&board.engine.stage==2&&board.completed==1,"victory unlocks exactly the next stage");
     check(board.unlocked(1)&&board.unlocked(2)&&!board.unlocked(3),"cleared stages replay while future stages stay locked");
     board.engine=null;board.completed=29;board.chooseCampaign(1);board.mission=31;
     board.startMission();check(board.engine==null,"second campaign stays locked until stage 30 is cleared");
     board.chooseCampaign(0);board.mission=30;board.startMission();board.engine.result=1;board.rewardGiven=false;board.resultStarted=SystemClock.uptimeMillis()-1100;board.resultDialog=false;board.battleClick(200,150);
     board.battleClick(280,190);
     check(board.completed==30&&board.mission==31&&board.engine.stage==31&&board.campaignPage==1,
      "stage 30 victory transitions to second map and stage 31");
     check(board.worlds.length()==90&&board.frontierMarkers.length()==30,"two maps contain sixty levels");
     board.engine=null;board.completed=89;board.mission=90;board.startMission();
     board.engine.result=1;board.rewardGiven=false;board.resultStarted=SystemClock.uptimeMillis()-1100;board.resultDialog=false;board.battleClick(200,150);board.battleClick(280,190);
     check(board.completed==90&&board.mission==90&&board.engine.result==1,"final stage cannot advance beyond campaign");
     board.completed=savedCompleted;board.points=savedPoints;board.save();board.mission=1;board.campaignPage=0;
     board.engine=new Engine(1,0);board.engine.think=10000;board.paused=false;board.helpPage=-1;
     board.cameraX=board.cameraY=0;board.zoom=1;board.spell=-1;
     Engine engine=board.engine;engine.forts.clear();
     Engine.Fort a=new Engine.Fort(100,100,1000,1,Engine.GENERATOR);
     Engine.Fort b=new Engine.Fort(100,200,800,1,Engine.CRYSTAL);
     Engine.Fort enemy=new Engine.Fort(400,100,1000,2,Engine.GENERATOR);
     engine.forts.add(a);engine.forts.add(b);engine.forts.add(enemy);
     board.selected.add(a);board.selected.add(b);
     touch(board,MotionEvent.ACTION_DOWN,100,100);
     touch(board,MotionEvent.ACTION_MOVE,400,100);
     touch(board,MotionEvent.ACTION_UP,400,100);
     check(engine.marches.size()==2&&a.n==500&&b.n==400,"drag deploys every selected castle");
     Engine.March first=engine.marches.get(0),second=engine.marches.get(1);
     board.battleClick(first.x(),first.y()-9);
     board.battleClick(second.x(),second.y()-9);
     check(board.selectedTroops.size()==2,"tap selects multiple moving armies");
     touch(board,MotionEvent.ACTION_DOWN,first.x(),first.y()-9);
     touch(board,MotionEvent.ACTION_MOVE,250,200);
     touch(board,MotionEvent.ACTION_UP,250,200);
     check(first.b==enemy&&second.b==enemy&&board.selectedTroops.size()==2,"ground drag leaves armies and selection unchanged");
     touch(board,MotionEvent.ACTION_DOWN,first.x(),first.y()-9);
     touch(board,MotionEvent.ACTION_MOVE,100,200);
     touch(board,MotionEvent.ACTION_UP,100,200);
     check(first.b==b&&second.b==b,"drag redirects all selected armies to a building");
     check(first.n==500&&second.n==400,"redirection retains entire moving armies");
     engine.marches.clear();board.selectedTroops.clear();engine.energy=0;
     for(int slot=0;slot<4;slot++){
      board.battleClick(290+slot*51,295);
      check(board.spellInfo==board.spellSlots[slot]&&board.spell==-1,"grey spell tooltip without arming");
      check(!engine.canUseSpell(board.spellSlots[slot]),"grey spell remains unavailable");
     }
     board.battleClick(100,100);
     check(board.spellInfo==-1,"castle tap has no specification tooltip");
     engine.skillLevels[3]=5;
     board.battleClick(443,295);
     check(engine.spellDetails(3).startsWith("800 troops"),"upgraded reinforcement description");
     board.lastTime=0;board.invalidate();
    }catch(ReflectiveOperationException problem){throw new RuntimeException(problem);}
   });
   SystemClock.sleep(200);
   android.graphics.Bitmap screenshot=getUiAutomation().takeScreenshot();
   java.io.File file=new java.io.File(getTargetContext().getExternalFilesDir(null),"ui-check.png");
   try(java.io.FileOutputStream output=new java.io.FileOutputStream(file)){
    screenshot.compress(android.graphics.Bitmap.CompressFormat.PNG,100,output);
   }
   result.putString("stream","UI checks passed: three campaign maps, stage-30-to-31 unlock, sequential locks, victory/defeat/final-stage controls, spell descriptions, group deployment and building-only army redirection.\n");
   finish(-1,result);
  }catch(Throwable problem){result.putString("stream",android.util.Log.getStackTraceString(problem));finish(0,result);}
 }
}
