package castle;
import android.app.Instrumentation;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import java.lang.reflect.Field;
import java.util.*;
public final class AudioUiCheck extends Instrumentation {
 void check(boolean b,String s){if(!b)throw new AssertionError(s);}
 Object field(Object o,String name)throws Exception{Field f=o.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(o);}
 int starts(Map<String,Integer> counts,String file){return counts.getOrDefault("effects/"+file+".wav",0);}
 @Override public void onCreate(Bundle args){super.onCreate(args);start();}
 @Override public void onStart(){Bundle result=new Bundle();try{
  Main main=(Main)startActivitySync(new Intent(Intent.ACTION_MAIN).setClassName("game.castlewars.rebuilt","castle.Main").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
  waitForIdleSync();GameView board=(GameView)field(main,"board");
  Random audioRandom=(Random)field(field(main,"audio"),"audioRandom");
  SystemClock.sleep(1200);
  Map<String,Integer> ids=(Map<String,Integer>)field(field(main,"audio"),"soundIds");Set<Integer> ready=(Set<Integer>)field(field(main,"audio"),"readySounds");
  Map<String,Integer> counts=(Map<String,Integer>)field(field(main,"audio"),"soundStarts");Map<Object,Integer> streams=(Map<Object,Integer>)field(field(main,"audio"),"effectStreams");
  runOnMainSync(()->{
   for(String name:new String[]{"move1","move2","attack1","attack2","arrow","fireball","explosion","twister","hypnoses","support"})check(ids.containsKey("effects/"+name+".wav")&&ready.contains(ids.get("effects/"+name+".wav")),"sample loaded successfully: "+name);
   board.mission=1;board.startMission();board.zoom=1;board.cameraX=board.cameraY=0;board.profile.setSoundEnabled(true);board.engine.think=10000;
   Engine e=board.engine;e.forts.clear();e.marshes.clear();e.altars.clear();
   e.forts.add(new Engine.Fort(20,20,1000,1,1));e.forts.add(new Engine.Fort(450,250,1000,2,1));e.energy=10000;
   audioRandom.setSeed(410784366L);counts.clear();e.power(0,null,200,150);e.power(1,null,200,150);board.invalidate();
  });
  SystemClock.sleep(350);
  runOnMainSync(()->{check(starts(counts,"fireball")==5,"five fireball streams accepted by SoundPool");check(starts(counts,"twister")==1&&streams.size()==6,"one tornado loop alongside launches");});
  SystemClock.sleep(1400);
  runOnMainSync(()->{check(starts(counts,"explosion")==5&&streams.size()==1,"launch streams stopped at five impacts; tornado remains");board.paused=true;board.invalidate();});
  SystemClock.sleep(250);
  runOnMainSync(()->{try{check((Boolean)field(field(main,"audio"),"effectsPaused"),"SFX paused with game");}catch(Exception ex){throw new RuntimeException(ex);}board.paused=false;board.lastTime=0;board.invalidate();});
  SystemClock.sleep(9000);
  runOnMainSync(()->{
   check(board.engine.storms.isEmpty()&&streams.isEmpty()&&starts(counts,"twister")==1,"tornado ends without repeated starts or leaked loop");
   Engine e=board.engine;Engine.Fort a=e.forts.get(0),b=e.forts.get(1);
   for(int i=0;i<12;i++){a.n=1000;board.selected.add(a);board.deploySelection(b.x,b.y);}
   e.raidSerial+=12;board.invalidate();
  });SystemClock.sleep(250);
  runOnMainSync(()->{
   check(starts(counts,"move1")>0&&starts(counts,"move2")>0&&starts(counts,"move1")+starts(counts,"move2")==12,"seeded RNG chooses both movement variants, once per command");
   check(starts(counts,"attack1")>0&&starts(counts,"attack2")>0&&starts(counts,"attack1")+starts(counts,"attack2")==12,"seeded RNG chooses both battle variants, once per wave");
   board.engine=null;board.screen="menu";board.invalidate();
  });
  result.putString("stream","Audio UI passed: all ten samples ready, five launch and impact streams, tornado lifetime/pause, seeded random deployment/battle variants.\n");finish(-1,result);
 }catch(Throwable failure){result.putString("stream",android.util.Log.getStackTraceString(failure));finish(0,result);}}
}
