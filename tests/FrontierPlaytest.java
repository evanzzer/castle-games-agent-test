package castle;
public final class FrontierPlaytest {
 public static void main(String[] args){int wins=0,losses=0,unfinished=0;
  for(int stage=31;stage<=60;stage++){
   Engine e=new Engine(stage,0,2026+stage);e.attackLevels[1]=3;e.defenseLevels[1]=3;e.speedLevel=3;e.crystalLevel=3;
   java.util.Arrays.fill(e.skillLevels,3);float command=0;
   for(int step=0;step<6000&&e.result==0;step++){
    if(command<=0){e.takeFrontierTurn(1);e.tryAISpell(1);command=3;}
    e.tick(.05f);command-=.05f;
   }
   if(e.result==1)wins++;else if(e.result==2)losses++;else unfinished++;
   System.out.println(stage+","+e.result+","+Math.round(e.time)+","+(e.aiCasts[2]+e.aiCasts[3]));
  }
  System.out.println("Automated player with tier-3 upgrades: "+wins+" wins, "+losses+" losses, "+unfinished+" unresolved at 300s.");
 }
}
