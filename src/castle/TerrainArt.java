package castle;
import android.graphics.*;
import java.util.List;
/** Bakes original material artwork into NEW terrain silhouettes and feathered PNGs. */
final class TerrainArt {
 static Bitmap tile(Bitmap source,int x,int y,int w,int h,int base){
  Bitmap result=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);
  Canvas canvas=new Canvas(result);canvas.drawColor(base);
  canvas.drawBitmap(source,new Rect(x,y,x+w,y+h),new Rect(0,0,w,h),new Paint(3));
  return result;
 }
 static Bitmap bake(List<Engine.Marsh> zones,Bitmap marsh,Bitmap poison,int width,int height){
  Bitmap output=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);
  Canvas canvas=new Canvas(output);Paint paint=new Paint(3);int index=0;
  for(Engine.Marsh zone:zones){
   Path shape=new Path();
   for(int i=0;i<zone.points.length;i+=2){if(i==0)shape.moveTo(zone.points[i],zone.points[i+1]);else shape.lineTo(zone.points[i],zone.points[i+1]);}
   shape.close();
   paint.reset();paint.setAntiAlias(true);paint.setColor(0x88402c1a);
   paint.setMaskFilter(new BlurMaskFilter(11,BlurMaskFilter.Blur.NORMAL));canvas.drawPath(shape,paint);
   Bitmap layer=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);
   Canvas ink=new Canvas(layer);Bitmap material=zone.dark?marsh:poison;
   BitmapShader shader=new BitmapShader(material,Shader.TileMode.MIRROR,Shader.TileMode.MIRROR);
   Matrix placement=new Matrix();placement.setScale(1.1f+index*.025f,1.1f+index*.025f);placement.postTranslate(index*29,index*47);shader.setLocalMatrix(placement);
   paint.reset();paint.setAntiAlias(true);paint.setFilterBitmap(true);paint.setShader(shader);ink.drawRect(0,0,width,height,paint);
   Bitmap mask=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);Canvas stencil=new Canvas(mask);
   paint.reset();paint.setAntiAlias(true);paint.setColor(Color.WHITE);
   paint.setMaskFilter(new BlurMaskFilter(6,BlurMaskFilter.Blur.NORMAL));stencil.drawPath(shape,paint);
   paint.reset();paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));ink.drawBitmap(mask,0,0,paint);
   paint.reset();canvas.drawBitmap(layer,0,0,paint);mask.recycle();layer.recycle();index++;
  }
  return output;
 }
}
