package castle;
import android.app.Instrumentation;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import java.lang.reflect.Field;
/** Visual regression for the two original stage-five terrain regions. */
public final class TerrainUiCheck extends Instrumentation {
 @Override public void onCreate(Bundle args){super.onCreate(args);start();}
 @Override public void onStart(){
  Bundle result=new Bundle();
  try{
   Main main=(Main)startActivitySync(new Intent(Intent.ACTION_MAIN).setClassName("game.castlewars.rebuilt","castle.Main").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
   waitForIdleSync();
   runOnMainSync(()->{
    try{
     Field field=Main.class.getDeclaredField("board");field.setAccessible(true);
     Main.Board board=(Main.Board)field.get(main);
     board.mission=5;board.engine=new Engine(5,0);board.engine.think=10000;
     board.cameraX=board.cameraY=0;board.zoom=1;board.spell=board.spellInfo=-1;
     board.paused=false;board.helpPage=-1;board.selected.clear();board.selectedTroops.clear();
     for(float y:new float[]{60,250}){
      Engine.March unit=new Engine.March(new Engine.Fort(240,y,0,1,Engine.GENERATOR),board.engine.waypoint(340,y),1000);
      board.engine.marches.add(unit);
     }
     board.engine.tick(.1f);
     for(Engine.March unit:board.engine.marches){
      if(!unit.slowed||Math.abs(unit.x()-242.5f)>.001f)throw new AssertionError("stage 5 region did not halve movement");
      unit.confusedTime=100;
     }
     board.lastTime=0;board.invalidate();
    }catch(ReflectiveOperationException failure){throw new RuntimeException(failure);}
   });
   SystemClock.sleep(200);
   android.graphics.Bitmap screenshot=getUiAutomation().takeScreenshot();
   java.io.File file=new java.io.File(getTargetContext().getExternalFilesDir(null),"stage5-terrain.png");
   try(java.io.FileOutputStream output=new java.io.FileOutputStream(file)){
    screenshot.compress(android.graphics.Bitmap.CompressFormat.PNG,100,output);
   }
   result.putString("stream","Stage 5 Android checks passed: both upper and lower regions halve movement and show slowdown.\n");finish(-1,result);
  }catch(Throwable failure){result.putString("stream",android.util.Log.getStackTraceString(failure));finish(0,result);}
 }
}
