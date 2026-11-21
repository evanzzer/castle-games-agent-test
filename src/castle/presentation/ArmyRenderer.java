package castle;

import android.graphics.Canvas;

/** Renders armies, flags and siege effects from domain values without issuing game commands. */
final class ArmyRenderer {
    private final GameResources resources;
    private final CanvasPainter painter;

    ArmyRenderer(GameResources resources, CanvasPainter painter) {
        this.resources = resources;
        this.painter = painter;
    }

    String fortArt(Engine.Fort fort){return fort.kind==Engine.GENERATOR?"house_"+fort.size:fort.kind==Engine.ARCHERY?"tower_"+fort.size:"mine";}

    // Preserve the artwork pivots and invert the original vertical coordinate.
    float[] shadowOffset(Engine.Fort fort){
        if(fort.kind==Engine.CRYSTAL)return new float[]{10.78f,16.37f};
        if(fort.kind==Engine.ARCHERY)return fort.size=='b'?new float[]{42.5f,71.78f}:fort.size=='m'?new float[]{39.72f,62.97f}:new float[]{34,62};
        return fort.size=='b'?new float[]{18.59f,26.58f}:fort.size=='m'?new float[]{18.13f,31.33f}:new float[]{27.25f,40.7f};
    }

    float[] flagOffset(Engine.Fort fort){
        if(fort.kind==Engine.CRYSTAL)return new float[]{6.28f,23.46f};
        if(fort.kind==Engine.ARCHERY)return fort.size=='b'?new float[]{15,74}:fort.size=='m'?new float[]{10.72f,59.22f}:new float[]{9,40.59f};
        return fort.size=='b'?new float[]{15.09f,35}:fort.size=='m'?new float[]{11.16f,32.55f}:new float[]{8.98f,23.77f};
    }

    float fortNumberY(Engine.Fort fort){
        GameResources.SpriteFrame body=resources.sprite("grby".charAt(fort.side)+"_"+fortArt(fort));
        float roof=fort.visualY-(body==null?40:body.frame.height()*.65f/2);
        if(fort.side!=0){float[] flag=flagOffset(fort);char size=fort.kind==Engine.CRYSTAL?'m':fort.size;
            GameResources.SpriteFrame cloth=resources.sprite("flag_"+"grby".charAt(fort.side)+"_"+size+"1");
            if(cloth!=null)roof=Math.min(roof,fort.visualY-flag[1]*.65f-cloth.frame.height()*.65f/2);
        }
        return roof-5;
    }

    void drawFort(Canvas canvas, Engine.Fort fort,boolean selected,float time) {
        String kind=fortArt(fort);float[] shadow=shadowOffset(fort);
        painter.natural(canvas,kind+"_s",fort.visualX+shadow[0]*.65f,fort.visualY+shadow[1]*.65f,.65f);
        painter.natural(canvas,"grby".charAt(fort.side)+"_"+kind+(selected?"_d":""),fort.visualX,fort.visualY,.65f);
        if(fort.side!=0){
            float[] flag=flagOffset(fort);char size=fort.kind==Engine.CRYSTAL?'m':fort.size;
            int frames=size=='s'?5:6;
            painter.natural(canvas,"flag_"+"grby".charAt(fort.side)+"_"+size+(1+(int)(time*9)%frames),
                fort.visualX+flag[0]*.65f,fort.visualY-flag[1]*.65f,.65f);
        }
        if(fort.fight==null)painter.troopNumber(canvas,(int)fort.n,fort.visualX,fortNumberY(fort),15,fort.side);
    }

    float formationHeight(int n){return 10+(float)Math.ceil(Math.sqrt(Engine.soldierCount(n)))*3;}

    float selectionRadius(int n){return 14+(float)Math.ceil(Math.sqrt(Engine.soldierCount(n)))*3;}

    void drawArmy(Canvas canvas,int side,int kind,int n,float x,float y,String direction,float time){
        int count=Engine.soldierCount(n),columns=(int)Math.ceil(Math.sqrt(count));
        int rows=columns==0?0:(count+columns-1)/columns;
        int variant=soldierVariant(kind);
        for(int row=0,index=0;row<rows;row++)for(int col=0;col<columns&&index<count;col++,index++){
            int frame=1+(int)(time*12+index*1.7f)%(variant==3?4:8);
            float dx=(col-(columns-1)/2f)*6+(row%2)*2;
            float dy=(row-(rows-1)/2f)*4;
            painter.natural(canvas,"soldier_"+"brby".charAt(side)+"_"+variant+"_"+direction+frame,x+dx,y+dy-11,.48f);
        }
    }

    void drawMutiny(Canvas canvas,Engine.March unit,float x,float y){
        if(unit.mutinyAge>=1.35f)return;
        int layer=canvas.saveLayerAlpha(null,(int)(255*Math.min(1,(1.35f-unit.mutinyAge)/.45f)));
        painter.natural(canvas,"skill_mutiny"+Math.min(8,1+(int)(unit.mutinyAge*8)),x,y,.55f);
        canvas.restoreToCount(layer);
    }

    void drawPickupBuff(Canvas canvas, Engine.March unit, float x, float y) {
        String name=unit.buff==1 ? "buffer_immunity" : "buffer_def";
        int frames=unit.buff==1 ? 5 : 4;
        // Play once, retaining the last frame as a status icon until expiry.
        painter.natural(canvas, name + Math.min(frames, 1+(int)(unit.buffAge*8)), x, y, .6f);
    }

    int soldierVariant(int kind) {
        return kind == Engine.GENERATOR ? 1 : kind == Engine.CRYSTAL ? 2 : 3;
    }

    void drawFight(Canvas canvas, Engine.Fort fort,float time) {
        String cloudName="battle"+(1+(int)(time*10)%4);
        GameResources.SpriteFrame cloud=resources.sprite(cloudName);
        if(cloud!=null){
            float width=cloud.frame.width()*.7f,height=cloud.frame.height()*.9f;
            painter.sprite(canvas,cloudName,fort.x-width/2,fort.y-16-height/2,width,height);
        }
        int count=0;for(int side=1;side<4;side++)if(fort.fight.troops[side]>0)count++;
        float y=fortNumberY(fort)-count*15;
        for(int side=1;side<4;side++)if(fort.fight.troops[side]>0){
            painter.troopNumber(canvas,(int)fort.fight.troops[side],fort.visualX,y,14,side);y+=15;
        }
        painter.troopNumber(canvas,(int)fort.n,fort.visualX,y,14,fort.side);
    }

    void drawEncounter(Canvas canvas, Engine.Encounter encounter,float time) {
        painter.natural(canvas,"battle_troop"+(1+(int)(time*10)%4),encounter.x,encounter.y-5,.65f);
        int index=0,teams=encounter.teams();
        for(int side=1;side<4;side++)if(encounter.total(side)>0){
            painter.troopNumber(canvas,(int)encounter.total(side),encounter.x,encounter.y-38-(teams-1-index)*15,13,side);
            for(Engine.March unit:encounter.units)if(unit.side==side&&unit.n>0){
                if(unit.buff!=0)drawPickupBuff(canvas,unit,encounter.x+(index-(teams-1)/2f)*20,encounter.y-25);
                drawMutiny(canvas,unit,encounter.x,encounter.y-38);break;
            }
            index++;
        }
    }
}
