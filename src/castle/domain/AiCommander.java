package castle;

import java.util.*;
import static castle.Engine.*;

/** Chooses enemy deployment and spell orders while the match retains its state and RNG. */
final class AiCommander {
    private final Engine match;

    AiCommander(Engine match) { this.match = match; }

    float aiDelay(){
        if(match.campaign.strategicAI)return Math.max(2.2f,4.8f-(match.stage-31)*.065f)+match.aiRandom.nextFloat()*1.6f;
        float progress=match.stage/30f;
        return 10-5*progress+match.aiRandom.nextFloat()*5;
    }
    static float aiDepartureThreshold(Fort fort){return fort.kind==ARCHERY?capacity(fort)*.5f:500;}
    Fort nearestOther(Fort origin,int side){
        Fort target=null;double nearest=Double.POSITIVE_INFINITY;
        for(Fort fort:match.forts)if(fort.side!=side){
            double distance=Math.hypot(fort.x-origin.x,fort.y-origin.y);
            if(distance<nearest){nearest=distance;target=fort;}
        }
        return target;
    }
    void takeTurn(int side){
        if(match.campaign.strategicAI){takeFrontierTurn(side);tryAISpell(side);return;}
        ArrayList<Fort> owned=new ArrayList<>();
        for(Fort fort:match.forts)if(fort.side==side)owned.add(fort);
        for(Fort anchor:owned){
            Fort target=nearestOther(anchor,side);
            if(target==null)continue;
            // Original chooses 1 .. (1 + 3*stage/30), upper bound exclusive.
            int upper=1+(int)(3*Math.min(30,match.stage)/30f);
            int count=1+(upper>1?match.aiRandom.nextInt(upper-1):0);
            ArrayList<Fort> strongest=new ArrayList<>(owned);
            strongest.sort((a,b)->Float.compare(b.n,a.n));
            for(int i=0;i<Math.min(count,strongest.size());i++){
                Fort source=strongest.get(i);
                if(source.fight==null&&source.n>=aiDepartureThreshold(source))match.send(source,target);
            }
        }
        if(!owned.isEmpty())tryAISpell(side);
    }
    float incoming(Fort target,int side,boolean friendly){
        float n=0;
        for(March unit:match.marches)if(unit.b==target&&unit.n>0&&((unit.side==side)==friendly))n+=unit.n;
        if(target.fight!=null)for(int team=1;team<=3;team++)if((team==side)==friendly)n+=target.fight.troops[team];
        return n;
    }
    void takeFrontierTurn(int side){
        ArrayList<Fort> sources=new ArrayList<>();
        for(Fort fort:match.forts)if(fort.side==side&&fort.fight==null&&fort.n>=250)sources.add(fort);
        sources.sort((a,b)->Float.compare(b.n,a.n));
        int orders=0,limit=Math.min(4,2+(match.stage-31)/12);
        for(Fort source:sources){
            Fort best=null;double value=-Double.MAX_VALUE;
            for(Fort target:match.forts){
                if(target==source)continue;
                float friendly=incoming(target,side,true),hostile=incoming(target,side,false);
                double score;
                if(target.side==side){
                    float danger=hostile-target.n-friendly;
                    if(danger<50)continue;
                    score=650+Math.min(800,danger);
                }else{
                    float defending=target.n+hostile*.25f;
                    if(friendly>defending*1.5f+150)continue;
                    score=source.n*.5+friendly*.6-defending*.65;
                    if(target.side==0)score+=100;
                    if(target.kind==CRYSTAL)score+=180;
                    if(target.kind==GENERATOR)score+=100;
                    if(target.kind==ARCHERY&&target.side!=0)score-=100;
                    if(target.fight!=null&&friendly>0)score+=140;
                }
                score-=Math.hypot(source.x-target.x,source.y-target.y)*.7;
                // Poison costs troops; slower routes expose attackers to counters and spells.
                for(int sample=1;sample<10;sample++)for(Marsh zone:match.marshes)
                    if(zone.contains(source.x+(target.x-source.x)*sample/10,source.y+(target.y-source.y)*sample/10))score-=zone.dark?9:24;
                score+=match.aiRandom.nextFloat()*35;
                if(score>value){value=score;best=target;}
            }
            if(best!=null&&(value>-130||source.n>=capacity(source)*.75f)&&match.send(source,best))orders++;
            if(orders>=limit)break;
        }
    }
    float hostileAreaValue(int side,float x,float y,float radius){
        float score=0;
        for(Fort target:match.forts)if(Math.abs(target.x-x)<radius&&Math.abs(target.y-y)<radius){
            if(target.side!=0&&target.side!=side)score+=target.n;
            if(target.fight!=null)for(int team=1;team<=3;team++)if(team!=side)score+=target.fight.troops[team];
        }
        for(March target:match.marches)if(target.side!=side&&target.n>0&&Math.abs(target.x()-x)<radius&&Math.abs(target.y()-y)<radius)score+=target.n;
        return score;
    }
    boolean tryAISpell(int side){
        if(side>1&&!match.campaign.strategicAI)return false;
        boolean owns=false;for(Fort fort:match.forts)if(fort.side==side)owns=true;
        if(!owns)return false;
        Fort relief=null;float danger=100;
        for(Fort fort:match.forts)if(fort.side==side&&fort.fight!=null){
            float pressure=incoming(fort,side,false)-fort.n;
            if(pressure>danger){danger=pressure;relief=fort;}
        }
        if(relief!=null&&match.canUseSpell(side,3)&&match.power(side,3,relief,relief.x,relief.y))return true;
        // Stop dangerous approaching armies, without consuming crystals on empty ground.
        March approaching=null;float threat=0;
        for(March unit:match.marches)if(unit.side!=side&&unit.n>=250&&unit.encounter==null&&unit.b.side==side){
            float score=unit.n-(float)Math.hypot(unit.x()-unit.b.x,unit.y()-unit.b.y);
            if(score>threat){threat=score;approaching=unit;}
        }
        if(approaching!=null&&approaching.confusedTime<=0&&match.canUseSpell(side,2))
            if(match.power(side,2,null,approaching.x(),approaching.y()))return true;
        Fort fortress=null;float sustained=500;
        for(Fort fort:match.forts)if(fort.side!=0&&fort.side!=side){
            float score=hostileAreaValue(side,fort.x,fort.y,25);
            if(score>sustained){sustained=score;fortress=fort;}
        }
        if(fortress!=null&&match.canUseSpell(side,1))return match.power(side,1,fortress,fortress.x,fortress.y);
        float best=250,bx=0,by=0;
        for(Fort fort:match.forts){float score=hostileAreaValue(side,fort.x,fort.y,50);if(score>best){best=score;bx=fort.x;by=fort.y;}}
        for(March unit:match.marches)if(unit.side!=side&&unit.n>0){float score=hostileAreaValue(side,unit.x(),unit.y(),50);if(score>best){best=score;bx=unit.x();by=unit.y();}}
        return best>250&&match.canUseSpell(side,0)&&match.power(side,0,null,bx,by);
    }
}
