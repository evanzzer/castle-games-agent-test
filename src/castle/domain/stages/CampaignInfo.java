package castle;
import java.util.Locale;
/** Shared stage-label lookup and heading formatting for every campaign. */
final class CampaignInfo {
 static String name(int stage){
  return StageDefinition.get(stage).name;
 }
 static String brief(int stage){
  return StageDefinition.get(stage).brief;
 }
 static String title(int stage){return String.format(Locale.US,"%02d · %s",stage,name(stage));}
}
