package castle;
import android.app.Instrumentation;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import java.lang.reflect.Field;
public final class WildsUiCheck extends Instrumentation {
 static void check(boolean okay,String message){if(!okay)throw new AssertionError(message);}
 void capture(String name)throws Exception{
  SystemClock.sleep(750);runOnMainSync(()->{});SystemClock.sleep(200);
  android.graphics.Bitmap bitmap=getUiAutomation().takeScreenshot();
  try(java.io.FileOutputStream out=new java.io.FileOutputStream(new java.io.File(getTargetContext().getExternalFilesDir(null),name))){
   bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out);
  }
 }
 @Override public void onCreate(Bundle args){super.onCreate(args);start();}
 @Override public void onStart(){Bundle result=new Bundle();try{
  Main main=(Main)startActivitySync(new Intent(Intent.ACTION_MAIN).setClassName("game.castlewars.rebuilt","castle.Main").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
  waitForIdleSync();Field field=Main.class.getDeclaredField("board");field.setAccessible(true);
  GameView board=(GameView)field.get(main);int saved=board.profile.completedStages();float savedPoints=board.profile.upgradePoints();
  runOnMainSync(()->{
   ProfileFixtures.completed(board,59);board.engine=null;board.mission=31;board.campaignPage=1;board.campaignSelection[2]=0;board.screen="campaign";
   board.click(400,15);check(board.campaignPage==2&&board.mission==61&&!board.unlocked(61),"third campaign is sequentially locked");
   board.click(400,295);check(board.engine==null,"cannot start stage 61 early");
   board.chooseCampaign(1);board.mission=60;board.startMission();board.engine.result=1;board.rewardGiven=false;board.resultStarted=SystemClock.uptimeMillis()-1100;board.resultDialog=false;board.battleClick(200,150);board.battleClick(280,190);
   check(board.profile.completedStages()==60&&board.mission==61&&board.engine.stage==61&&board.campaignPage==2,"clearing stage 60 unlocks the third campaign");
   board.engine=null;board.screen="campaign";board.invalidate();
  });capture("wilds-map.png");
  runOnMainSync(()->{ProfileFixtures.completed(board,90);board.mission=65;board.startMission();board.engine.think=10000;
   check(board.world().overlay.image.equals("terrain/wild/terrain65.png"),"new PNG terrain overlay is loaded");board.invalidate();
  });capture("wilds-battle.png");
  runOnMainSync(()->{board.mission=90;board.startMission();board.engine.result=1;board.rewardGiven=false;board.resultStarted=SystemClock.uptimeMillis()-1100;board.resultDialog=false;board.battleClick(200,150);board.battleClick(280,190);
   check(board.mission==90&&board.engine.result==1,"final stage cannot advance to 91");
   ProfileFixtures.completed(board,saved);ProfileFixtures.points(board,savedPoints);board.engine=null;board.screen="menu";board.mission=1;board.campaignPage=0;board.save();
  });
  result.putString("stream","Wilds Android checks passed: third map, stage 60-to-61 unlock, stage 90 boundary and newly shaped PNG overlay.\n");finish(-1,result);
 }catch(Throwable failure){result.putString("stream",android.util.Log.getStackTraceString(failure));finish(0,result);}}
}
