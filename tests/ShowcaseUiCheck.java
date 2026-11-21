package castle;

import android.app.Instrumentation;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import java.io.*;
import java.lang.reflect.Field;
import java.util.*;

/** Captures reproducible documentation media without changing shipped gameplay. */
public final class ShowcaseUiCheck extends Instrumentation {
    private String scenario;
    private long started;
    private File directory;

    @Override public void onCreate(Bundle args) {
        super.onCreate(args);
        scenario=args.getString("scenario","battle");
        start();
    }

    private void at(float seconds,Runnable action) {
        long wait=started+(long)(seconds*1000)-SystemClock.uptimeMillis();
        if(wait>0)SystemClock.sleep(wait);
        runOnMainSync(action);
    }

    private void save(Bitmap bitmap,String name)throws IOException {
        try(FileOutputStream output=new FileOutputStream(new File(directory,name))) {
            bitmap.compress(Bitmap.CompressFormat.PNG,100,output);
        }
        bitmap.recycle();
    }

    private Engine.Fort target(Engine engine,Engine.Fort source,boolean hostile) {
        Engine.Fort closest=null;double distance=Double.MAX_VALUE;
        for(Engine.Fort fort:engine.forts)if(hostile?fort.side>1:fort.side!=1){
            double candidate=Math.hypot(fort.x-source.x,fort.y-source.y);
            if(candidate<distance){closest=fort;distance=candidate;}
        }
        return closest;
    }

    private void deploy(Main.Board board,Engine.Fort source,Engine.Fort destination) {
        board.selected.clear();board.selectedTroops.clear();board.selected.add(source);
        board.deploySelection((destination.x-board.cameraX)*board.zoom,(destination.y-board.cameraY)*board.zoom);
    }

    private void cast(Main.Board board,int slot,Engine.Fort destination) {
        board.battleClick(292+slot*51,295);
        board.battleClick((destination.x-board.cameraX)*board.zoom,(destination.y-board.cameraY)*board.zoom);
    }

    @Override public void onStart() {
        Bundle result=new Bundle();int resultCode=-1;Main.Board board=null;int savedCompleted=0,savedPoints=0;
        try {
            Main main=(Main)startActivitySync(new Intent(Intent.ACTION_MAIN)
                .setClassName("game.castlewars.rebuilt","castle.Main").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            waitForIdleSync();Field field=Main.class.getDeclaredField("board");field.setAccessible(true);
            board=(Main.Board)field.get(main);final Main.Board view=board;
            savedCompleted=board.completed;savedPoints=board.points;
            directory=getTargetContext().getExternalFilesDir(null);
            boolean spells=scenario.equals("spells");
            runOnMainSync(()->{
                view.completed=89;view.mission=spells?65:5;view.startMission();view.rewardGiven=true;
                if(spells){view.engine.energy=2000;view.engine.skillLevels[3]=5;}
            });
            SystemClock.sleep(800);
            Engine.Fort source=board.engine.forts.stream().filter(f->f.side==1).findFirst().get();
            Engine.Fort destination=target(board.engine,source,false);
            Engine.Fort hostile=target(board.engine,source,true);
            File video=new File(directory,"showcase-"+scenario+".mp4");
            ParcelFileDescriptor recording=getUiAutomation().executeShellCommand(
                "/system/bin/screenrecord --size 960x640 --bit-rate 1500000 --time-limit 18 "+video.getAbsolutePath());
            started=SystemClock.uptimeMillis();
            at(.7f,()->{view.selected.add(source);view.invalidate();});
            at(1.5f,()->deploy(view,source,destination));
            at(3.4f,()->deploy(view,source,destination));
            if(spells){
                at(3.8f,()->view.battleClick(445,295));
                at(4.5f,()->{view.spell=-1;view.spellInfo=-1;cast(view,3,hostile);});
                at(6.0f,()->view.battleClick(292,295));
                at(6.7f,()->{view.spell=-1;view.spellInfo=-1;cast(view,0,hostile);});
                at(8.3f,()->cast(view,2,hostile));
            }else{
                at(6.0f,()->deploy(view,source,destination));
                at(9.0f,()->deploy(view,source,destination));
            }
            at(10.0f,()->view.invalidate());
            save(getUiAutomation().takeScreenshot(),"showcase-"+scenario+".png");
            at(17.5f,()->{});
            ByteArrayOutputStream diagnostics=new ByteArrayOutputStream();
            try(InputStream status=new ParcelFileDescriptor.AutoCloseInputStream(recording)) {
                byte[] buffer=new byte[1024];for(int n;(n=status.read(buffer))!=-1;)diagnostics.write(buffer,0,n);
            }
            // Shell-owned recordings need read permission before the app can decode them.
            try(InputStream permissions=new ParcelFileDescriptor.AutoCloseInputStream(
                getUiAutomation().executeShellCommand("/system/bin/chmod 644 "+video.getAbsolutePath()))) {
                while(permissions.read()!=-1){}
            }
            if(!video.isFile())throw new IOException("Recorder failed: "+diagnostics.toString("UTF-8"));
            MediaMetadataRetriever media=new MediaMetadataRetriever();media.setDataSource(video.getAbsolutePath());
            long duration=Long.parseLong(media.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
            if(duration<16000||video.length()<100000)throw new AssertionError("Recording is incomplete");
            for(int second:new int[]{3,8,13}) {
                Bitmap frame=media.getFrameAtTime(second*1000000L,MediaMetadataRetriever.OPTION_CLOSEST);
                if(frame==null)throw new AssertionError("Cannot decode video frame");
                save(frame,"showcase-"+scenario+"-frame-"+second+".png");
            }
            media.release();
            MediaExtractor extractor=new MediaExtractor();extractor.setDataSource(video.getAbsolutePath());
            MediaFormat format=extractor.getTrackFormat(0);
            org.json.JSONObject metadata=new org.json.JSONObject().put("scenario",scenario).put("duration_ms",duration)
                .put("bytes",video.length()).put("width",format.getInteger(MediaFormat.KEY_WIDTH))
                .put("height",format.getInteger(MediaFormat.KEY_HEIGHT)).put("codec",format.getString(MediaFormat.KEY_MIME)).put("audio",false);
            extractor.release();
            try(FileOutputStream output=new FileOutputStream(new File(directory,"showcase-"+scenario+".json"))) {
                output.write(metadata.toString(2).getBytes("UTF-8"));
            }
            result.putString("stream","Captured "+scenario+": "+duration+"ms MP4, "+video.length()+" bytes, screenshot and three decoded QA frames.\n");
        }catch(Throwable failure){
            result.putString("stream",android.util.Log.getStackTraceString(failure));resultCode=0;
        }finally{
            if(board!=null){final Main.Board view=board;final int completed=savedCompleted,points=savedPoints;
                runOnMainSync(()->{view.completed=completed;view.points=points;view.engine=null;view.screen="menu";view.save();});}
        }
        finish(resultCode,result);
    }
}
