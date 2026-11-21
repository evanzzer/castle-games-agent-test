package castle;
import android.app.Instrumentation;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.graphics.*;
import java.lang.reflect.Field;
/** Visual checks for the same title/description presentation across campaigns. */
public final class CampaignInfoUiCheck extends Instrumentation {
 void capture(String name)throws Exception{
  SystemClock.sleep(750);runOnMainSync(()->{});SystemClock.sleep(200);
  Bitmap shot=getUiAutomation().takeScreenshot();
  try(java.io.FileOutputStream out=new java.io.FileOutputStream(new java.io.File(getTargetContext().getExternalFilesDir(null),name))){shot.compress(Bitmap.CompressFormat.PNG,100,out);}
 }
 @Override public void onCreate(Bundle args){super.onCreate(args);start();}
 @Override public void onStart(){Bundle result=new Bundle();try{
  Main main=(Main)startActivitySync(new Intent(Intent.ACTION_MAIN).setClassName("game.castlewars.rebuilt","castle.Main").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
  waitForIdleSync();Field field=Main.class.getDeclaredField("board");field.setAccessible(true);
  GameView board=(GameView)field.get(main);int saved=board.profile.completedStages();
  runOnMainSync(()->{
   CanvasPainter labels=new CanvasPainter(board.resources());
   Paint paint=new Paint(3);paint.setTypeface(Typeface.create("serif",Typeface.BOLD));
   for(int stage=1;stage<=120;stage++){
    paint.setTextSize(11);if(paint.measureText(CampaignInfo.title(stage))>460)throw new AssertionError("heading overflows at stage "+stage);
    float size=labels.fittedTextSize(CampaignInfo.brief(stage),8,460);
    if(size<7)throw new AssertionError("description too small at stage "+stage);
    paint.setTextSize(size);if(paint.measureText(CampaignInfo.brief(stage))>460.01f)throw new AssertionError("description overflows at stage "+stage);
   }
  });
  for(int stage:new int[]{1,31,61,91,102}){
   final int selected=stage;
   runOnMainSync(()->{ProfileFixtures.completed(board,selected-1);board.engine=null;board.screen="campaign";board.mission=selected;board.campaignPage=(selected-1)/30;board.centreCampaignSelection();board.invalidate();});
   capture("campaign-info-"+stage+".png");
  }
  runOnMainSync(()->{ProfileFixtures.completed(board,saved);board.mission=1;board.campaignPage=0;board.screen="menu";board.save();});
  result.putString("stream","Campaign map UI checks passed: all 120 headings/descriptions fit; four campaign previews and stage-102 regression captured.\n");finish(-1,result);
 }catch(Throwable failure){result.putString("stream",android.util.Log.getStackTraceString(failure));finish(0,result);}}
}
