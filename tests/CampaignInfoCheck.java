package castle;
public final class CampaignInfoCheck {
 static void check(boolean okay,String message){if(!okay)throw new AssertionError(message);}
 public static void main(String[] args){
  check(Original.NAMES.length==30&&Original.BRIEFS.length==30,"all original stages have authored information");
  for(int stage=1;stage<=150;stage++){
   check(!CampaignInfo.name(stage).isEmpty()&&!CampaignInfo.brief(stage).isEmpty(),"every stage has a name and description");
   check(CampaignInfo.title(stage).startsWith(String.format(java.util.Locale.US,"%02d · ",stage)),"consistent heading format");
  }
  for(int stage=1;stage<=120;stage++){
   Engine engine=new Engine(stage,0);
   int attack=stage<=30?0:stage<=45?1:stage<=60?2:stage<=75?3:stage<=90?4:5;
   int defense=Math.max(0,attack-1);
   for(int side=2;side<=3;side++)check(engine.attackLevels[side]==attack&&engine.defenseLevels[side]==defense,
    "exact AI combat tiers at stage "+stage+" for team "+side);
  }
  check(CampaignInfo.title(1).equals("01 · First Foothold"),"modern original stage heading");
  check(CampaignInfo.brief(1).contains("neutral castle"),"intro strategy matches first map");
  check(CampaignInfo.name(31).equals(Frontier.NAMES[0])&&CampaignInfo.name(61).equals(Wilds.NAMES[0]),"later campaign metadata preserved");
  for(int stage=1;stage<=30;stage++)for(int side=2;side<=3;side++){
   Engine e=new Engine(stage,0);e.nationEnergy[side]=100000;
   for(int spell=0;spell<4;spell++){
    check(!e.canUseSpell(side,spell),"intro AI spell availability disabled");
    check(!e.power(side,spell,e.forts.get(0),100,100),"intro direct casting disabled");
   }
   check(!e.tryAISpell(side)&&e.aiCasts[side]==0&&e.nationEnergy[side]==100000,"intro AI decisions cannot cast or spend crystals");
   e.energy=1000;check(e.power(0,null,100,100),"player can learn spells during intro");
  }
  for(int stage:new int[]{31,60,61,90})for(int side=2;side<=3;side++){
   Engine e=new Engine(stage,0);e.think=10000;e.forts.clear();e.marshes.clear();e.altars.clear();
   Engine.Fort player=new Engine.Fort(100,100,1000,1,Engine.CRYSTAL),home=new Engine.Fort(600,100,1000,side,Engine.CRYSTAL);
   e.forts.add(player);e.forts.add(home);java.util.Arrays.fill(e.skillLevels,5);
   float[] expected=stage<=60?new float[]{402.5f,525,350,787.5f}:new float[]{322,420,280,630};
   for(int spell=0;spell<4;spell++){
    check(Math.abs(e.spellCost(side,spell)-expected[spell])<.001f,"exact campaign spell price");
    check(e.spellCost(1,spell)==Engine.COST[spell],"player prices retained");
    e.nationEnergy[side]=expected[spell]-.01f;check(!e.canUseSpell(side,spell),"cannot cast below exact price");
   }
   int level=stage<=60?0:2;
   e.nationEnergy[side]=expected[0];check(e.power(side,0,null,100,100)&&e.nationEnergy[side]==0,"meteor spends campaign price");
   check(Math.abs(e.meteors.get(0).damage-(level==0?40:52))<.001,"campaign meteor upgrade level");
   e.nationEnergy[side]=expected[1];check(e.power(side,1,null,100,100),"storm can cast");
   check(Math.abs(e.storms.get(0).damage-(level==0?50:65))<.001&&Math.abs(e.storms.get(0).duration-(level==0?10:13))<.001,"campaign storm damage and duration");
   Engine.March march=new Engine.March(player,home,500);e.marches.add(march);
   e.nationEnergy[side]=expected[2];check(e.power(side,2,null,100,100),"mutiny can cast");
   check(Math.abs(march.confusedTime-(level==0?5:6.5f))<.001,"campaign mutiny duration");
   float before=home.n;e.nationEnergy[side]=expected[3];check(e.power(side,3,home,home.x,home.y),"reinforcement can cast");
   check(home.n==before&&e.marches.stream().filter(m->m.side==home.side&&m.b==home).mapToInt(m->m.n).sum()==(level==0?100:280),"campaign reinforcement troop amount");
   check(e.energy==200,"enemy spending never charges player");
  }
  System.out.println("Campaign information and AI spell rules passed: 150 headings/descriptions and exact combat tiers; intro disabled; 1.75x Lv0 / 1.4x Lv2.");
 }
}
