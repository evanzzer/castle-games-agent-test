package castle;
import java.util.Locale;
/** Authored campaign copy, separate from imported original map geometry. */
final class CampaignInfo {
 static final String[] ORIGINAL_NAMES={
  "First Foothold","Twin Standards","Crossfire","Crystal Gambit","Divided Marsh",
  "Reed Crossing","Twin Venom","Venom Watch","Angel's Reach","Shield Crossing",
  "Bitter Basin","Three Banners","Tower Circuit","Poisoned Crown","Angel's Passage",
  "Ring of Steel","Broken Causeway","Venom Wings","Slow Divide","Guardian Roads",
  "Toxic Belt","Four Venoms","Shielded Narrows","Angel's Detour","Three Marshes",
  "Venom Chain","Mire and Venom","Threefold Poison","Swamp Rivals","Last Banners"
 };
 static final String[] ORIGINAL_BRIEFS={
  "Capture the neutral castle, then combine your forces against Blue.",
  "Use both starting castles to secure the centre before challenging Yellow.",
  "Claim the crystal and use the two towers to control the crossing.",
  "Control crystal income and stage your attack through the middle castle.",
  "Two swamps guard the outer approaches. Use the clear central gap.",
  "The broad swamp slows your advance. Secure nearby castles before pushing.",
  "Poison threatens both flanks. Reinforce from the nearest safe castle.",
  "Capture support castles around the poison before contesting the central tower.",
  "Time an angel pickup to protect your advance toward the central tower.",
  "Shield altars protect the assault, while swamp delays your reinforcements.",
  "Central poison makes long sieges costly. Use the shield altars to push through.",
  "Blue and Yellow both expand. Defend your rear while the rival armies clash.",
  "A network of towers guards the routes. Capture staging castles between them.",
  "Poison covers opposite approaches. Build strength along the safer centre.",
  "Use an angel pickup before committing troops to the long swamp crossing.",
  "Towers surround the centre. Advance through friendly castles in short steps.",
  "Slow ground guards opposite corners. Prepare reinforcements on both flanks.",
  "Poisoned diagonals punish shortcuts. Time the angel pickup for your assault.",
  "The middle crossing is slow. Secure the crystal supply before pushing through.",
  "Swamp divides the routes. Use the angel altars and nearby staging castles.",
  "A poison belt cuts across the centre. Use angel protection to cross safely.",
  "Four poison regions constrain movement. Contest the angel routes and clear gaps.",
  "Slow ground surrounds the central gap. Claim a shield before the main assault.",
  "Poison guards the inner approaches. Time the angel pickup with your attack.",
  "Three swamp approaches expose armies to tower fire. Coordinate your arrivals.",
  "Poison breaks up the centre. Keep a reserve outside the main siege.",
  "Both swamp and poison matter. Redirect armies as the front changes.",
  "Three nations compete across poison. Reinforce through the nearest safe castle.",
  "Three nations and slow approaches reward careful timing and short supply routes.",
  "Three banners and six swamps. Hold staging castles to support every front."
 };
 static void validate(int stage){if(stage<1||stage>Engine.TOTAL_STAGES)throw new IllegalArgumentException("Unknown stage");}
 static String name(int stage){
  validate(stage);return (stage<=30?ORIGINAL_NAMES:stage<=60?Frontier.NAMES:Wilds.NAMES)[(stage-1)%30];
 }
 static String brief(int stage){
  validate(stage);return (stage<=30?ORIGINAL_BRIEFS:stage<=60?Frontier.BRIEFS:Wilds.BRIEFS)[(stage-1)%30];
 }
 static String title(int stage){return String.format(Locale.US,"%02d · %s",stage,name(stage));}
}
