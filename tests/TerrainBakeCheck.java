package castle;
import android.app.Instrumentation;
import android.os.Bundle;
import android.graphics.*;
import java.io.*;
/** Export the actual game's new region art as separate transparent PNG assets. */
public final class TerrainBakeCheck extends Instrumentation {
 boolean frontier;
 @Override public void onCreate(Bundle args){super.onCreate(args);frontier=args!=null&&"frontier".equals(args.getString("campaign"));start();}
 @Override public void onStart(){Bundle result=new Bundle();try{
  Bitmap oldSlow,oldPoison;
  try(InputStream in=getTargetContext().getAssets().open("terrain/original/terrain5.png")){oldSlow=BitmapFactory.decodeStream(in);}
  try(InputStream in=getTargetContext().getAssets().open("terrain/original/terrain7.png")){oldPoison=BitmapFactory.decodeStream(in);}
  Bitmap slow=TerrainArt.tile(oldSlow,210,30,64,64,0xff4c3828);
  Bitmap poison=TerrainArt.tile(oldPoison,310,170,48,48,0xff31401e);
  File directory=getTargetContext().getExternalFilesDir(frontier?"frontier-terrain":"wilds-terrain");directory.mkdirs();
  org.json.JSONObject manifest=new org.json.JSONObject();
  for(int stage=frontier?31:61;stage<=(frontier?60:90);stage++){
   Engine engine=new Engine(stage,0);Bitmap overlay=TerrainArt.bake(engine.marshes,slow,poison,960,640);
   if(Color.alpha(overlay.getPixel(0,0))!=0)throw new AssertionError("overlay is not transparent outside terrain");
   for(Engine.Marsh zone:engine.marshes){boolean tested=false;
    for(int y=30;y<610&&!tested;y+=12)for(int x=30;x<930&&!tested;x+=12)
     if(zone.contains(x,y)&&zone.contains(x-10,y)&&zone.contains(x+10,y)&&zone.contains(x,y-10)&&zone.contains(x,y+10)){
      if(Color.alpha(overlay.getPixel(x,y))<180)throw new AssertionError("terrain art missing inside region "+stage);tested=true;
     }
    if(!tested)throw new AssertionError("region lacks a testable interior");
   }
   try(FileOutputStream out=new FileOutputStream(new File(directory,"terrain"+stage+".png"))){overlay.compress(Bitmap.CompressFormat.PNG,100,out);}
   byte[] digest=java.security.MessageDigest.getInstance("SHA-256").digest((frontier?Frontier.MARSHES[stage-31]:Wilds.MARSHES[stage-61]).getBytes("UTF-8"));
   StringBuilder hex=new StringBuilder();for(byte value:digest)hex.append(String.format(java.util.Locale.US,"%02x",value&255));
   manifest.put("terrain"+stage+".png",hex.toString());
   overlay.recycle();
  }
  try(FileOutputStream out=new FileOutputStream(new File(directory,"terrain-manifest.json"))){out.write(AssetMetadata.read(getTargetContext().getAssets(),"terrain/"+(frontier?"frontier":"wild")+"/manifest.json").put("overlays",manifest).toString(2).getBytes("UTF-8"));}
  result.putString("stream","Baked and checked 30 textured transparent terrain PNGs using original marsh/poison artwork.\n");finish(-1,result);
 }catch(Throwable failure){result.putString("stream",android.util.Log.getStackTraceString(failure));finish(0,result);}}
}
