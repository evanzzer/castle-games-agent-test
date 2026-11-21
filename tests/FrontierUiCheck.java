package castle;
import android.app.Instrumentation;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import java.lang.reflect.Field;
public final class FrontierUiCheck extends Instrumentation {
 static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
 void capture(String name)throws Exception{
  SystemClock.sleep(750);
  runOnMainSync(()->{});
  SystemClock.sleep(200);
  android.graphics.Bitmap shot=getUiAutomation().takeScreenshot();
  java.io.File file=new java.io.File(getTargetContext().getExternalFilesDir(null),name);
  try(java.io.FileOutputStream output=new java.io.FileOutputStream(file)){shot.compress(android.graphics.Bitmap.CompressFormat.PNG,100,output);}
 }
 @Override public void onCreate(Bundle args){super.onCreate(args);start();}
 @Override public void onStart(){
  Bundle result=new Bundle();
  try{
   Main main=(Main)startActivitySync(new Intent(Intent.ACTION_MAIN).setClassName("game.castlewars.rebuilt","castle.Main").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
   waitForIdleSync();Field field=Main.class.getDeclaredField("board");field.setAccessible(true);
   Main.Board board=(Main.Board)field.get(main);int saved=board.completed;
   runOnMainSync(()->{
    board.completed=29;board.engine=null;board.screen="campaign";board.mission=1;board.campaignPage=0;board.campaignSelection[0]=board.campaignSelection[1]=0;
    board.click(240,15);
    check(board.campaignPage==1&&board.mission==31&&!board.unlocked(31),"second map previews while locked");
    board.click(400,295);check(board.engine==null,"cannot enter campaign two early");
    board.completed=30;board.click(240,15);
    check(board.unlocked(31)&&!board.unlocked(32),"only first Frontier stage unlocks after stage 30");
    board.click(137-board.mapScroll,board.campaignY(36));check(board.mission==31,"locked Frontier marker cannot be selected");
    board.completed=45;board.chooseCampaign(0);board.campaignSelection[1]=0;board.chooseCampaign(1);
    check(board.mission==46,"Frontier opens at next unlocked stage instead of restarting");
    board.mission=40;board.chooseCampaign(0);board.chooseCampaign(1);
    check(board.mission==40,"campaign switching remembers selection");
    board.completed=30;board.mission=31;board.campaignSelection[1]=31;board.centreCampaignSelection();
    board.invalidate();
   });
   capture("frontier-map.png");
   runOnMainSync(()->{
    board.completed=60;board.mission=60;board.startMission();
    check(board.engine.stage==60&&board.worlds.length()==90&&board.world().optJSONArray("overlay")!=null,"last original level loads on second map");
    Engine engine=board.engine;engine.nationEnergy[2]=402.5f;
    Engine.Fort player=engine.forts.stream().filter(f->f.side==1).findFirst().get();
    check(engine.power(2,0,null,player.x,player.y)&&engine.nationEnergy[2]==0,"AI spell costs exactly 402.5 crystals in Android");
    check(board.world().optJSONArray("overlay").optString(0).equals("terrain/frontier/terrain60.png"),"restored Frontier PNG overlay is loaded");
    check(board.bitmap("terrain/frontier/terrain60.png")!=null,"textured Frontier terrain asset exists");
    check(engine.meteors.size()==5&&engine.meteors.get(0).damage==40,"enemy Meteor stays at base strength in Android");
    board.lastTime=0;board.invalidate();
   });
   capture("frontier-battle.png");
   runOnMainSync(()->{board.completed=saved;board.engine=null;board.mission=1;board.campaignPage=0;board.screen="menu";board.save();});
   result.putString("stream","Frontier Android checks passed: map tabs, locks, 90 worlds, final stage and 1.75x-cost base-strength enemy Meteor.\n");finish(-1,result);
  }catch(Throwable failure){result.putString("stream",android.util.Log.getStackTraceString(failure));finish(0,result);}
 }
}
