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
   board.click(240,15);check(board.campaignPage==2&&board.mission==61&&!board.unlocked(61),"third campaign is sequentially locked");
   board.click(400,295);check(board.engine==null,"cannot start stage 61 early");
   board.chooseCampaign(1);board.mission=60;board.startMission();board.engine.result=1;board.rewardGiven=false;board.resultStarted=SystemClock.uptimeMillis()-1100;board.resultDialog=false;board.battleClick(200,150);board.battleClick(280,190);
   check(board.profile.completedStages()==60&&board.mission==61&&board.engine.stage==61&&board.campaignPage==2,"clearing stage 60 unlocks the third campaign");
   board.engine=null;board.screen="campaign";board.invalidate();
  });capture("wilds-map.png");
  runOnMainSync(()->{ProfileFixtures.completed(board,150);board.mission=65;board.startMission();board.engine.think=10000;
   check(board.world().overlay.image.equals("terrain/wild/terrain65.png"),"new PNG terrain overlay is loaded");board.invalidate();
  });capture("wilds-battle.png");
  runOnMainSync(()->{
   ProfileFixtures.completed(board,89);board.engine=null;board.screen="campaign";board.mission=61;board.campaignPage=2;board.campaignSelection[3]=0;
   board.click(330,15);check(board.campaignPage==3&&board.mission==91&&!board.unlocked(91),"fourth tab stays locked before clearing stage 90");
   board.click(400,295);check(board.engine==null,"cannot start stage 91 early");
   board.chooseCampaign(2);board.mission=90;board.startMission();board.engine.result=1;board.rewardGiven=false;board.resultStarted=SystemClock.uptimeMillis()-1100;board.resultDialog=false;
   board.battleClick(200,150);board.battleClick(280,190);
   check(board.profile.completedStages()==90&&board.mission==91&&board.engine.stage==91&&board.campaignPage==3,"stage 90 unlocks Citadel");
   check(board.resources().worldCount()==150&&board.resources().markers(Campaign.CITADEL).size()==30&&board.resources().markers(Campaign.ZENITH).size()==30,"five campaign resources loaded");
   check(board.world().overlay.image.equals("terrain/citadel/terrain91.png")&&board.resources().image(board.world().overlay.image)!=null,"Citadel terrain PNG decodes on Android");
   board.paused=true;board.invalidate();
  });capture("citadel-battle.png");
  runOnMainSync(()->{board.engine=null;board.screen="campaign";board.invalidate();});capture("citadel-map.png");
  runOnMainSync(()->{
   ProfileFixtures.completed(board,119);board.engine=null;board.screen="campaign";board.mission=119;board.campaignPage=3;board.campaignSelection[4]=0;
   board.click(430,15);check(board.campaignPage==4&&board.mission==121&&!board.unlocked(121),"fifth tab stays locked before clearing stage 120");
   board.chooseCampaign(3);board.mission=120;board.startMission();board.engine.result=1;board.rewardGiven=false;board.resultStarted=SystemClock.uptimeMillis()-1100;board.resultDialog=false;
   board.battleClick(200,150);board.battleClick(280,190);
   check(board.profile.completedStages()==120&&board.mission==121&&board.engine.stage==121&&board.campaignPage==4,"stage 120 unlocks Zenith");
   check(board.world().overlay.image.equals("terrain/zenith/terrain121.png")&&board.resources().image(board.world().overlay.image)!=null,"Zenith terrain PNG decodes on Android");
   check(board.world().background.equals("map/backgrounds/map1.png")&&board.resources().image(board.world().background)!=null,"stage background decodes");
   board.paused=true;board.invalidate();
  });capture("zenith-battle.png");
  runOnMainSync(()->{ProfileFixtures.completed(board,150);board.mission=136;board.startMission();
   check(board.world().background.equals("map/backgrounds/map3.png")&&board.resources().image(board.world().background)!=null,"alternating Zenith background decodes");board.paused=true;board.invalidate();
  });capture("zenith-battle-alt.png");
  runOnMainSync(()->{board.engine=null;board.mission=150;board.startMission();board.engine.result=1;board.rewardGiven=false;board.resultStarted=SystemClock.uptimeMillis()-1100;board.resultDialog=false;board.battleClick(200,150);board.battleClick(280,190);
   check(board.mission==150&&board.engine.result==1,"final stage cannot advance past 150");
   ProfileFixtures.completed(board,saved);ProfileFixtures.points(board,savedPoints);board.engine=null;board.screen="menu";board.mission=1;board.campaignPage=0;board.save();
  });
  result.putString("stream","Wilds Android checks passed: five tabs, 60/90/120 unlocks, stage-150 boundary, per-stage backgrounds and Wild/Citadel/Zenith terrain PNGs.\n");finish(-1,result);
 }catch(Throwable failure){result.putString("stream",android.util.Log.getStackTraceString(failure));finish(0,result);}}
}
