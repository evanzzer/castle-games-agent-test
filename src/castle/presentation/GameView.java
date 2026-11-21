package castle;

import android.content.Context;
import android.graphics.*;
import android.view.*;
import java.util.*;

/** Presentation controller: dispatches use cases and renders prepared match values. */
final class GameView extends View {
    private final GameAudio audio;
    private final ProfileRepository profiles;
    private final GameResources resources;
    private final MatchFactory matches;
    private final Runnable onExit;
    private final CanvasPainter painter;
    private final ArmyRenderer units;
    private final MatchAudioEvents audioEvents = new MatchAudioEvents();
    private final Paint paint;
    private boolean foreground;
    PlayerProfile profile;

    final Set<Engine.Fort> selected = new LinkedHashSet<>();
    final Set<Engine.March> selectedTroops = new LinkedHashSet<>();
    final String[] help = {"help1", "help2", "help3_1", "help3_2", "help4_1",
        "help4_2", "help5_1", "help5_2", "help6", "help7"};
    static final int MAX_UPGRADE_LEVEL = UpgradeRules.MAX_LEVEL;
    final String[] upgradeNames = {"atk", "def", "spd", "exp", "crys_init",
        "crys_add", "skill_meteor", "skill_storm", "skill_mutiny", "skill_reinf", "cd"};
    final int[] spellSlots = {0, 2, 1, 3};
    final String[] spellNames = {"Meteor", "Storm", "Mutiny", "Reinforce"};
    final int[] campaignSelection = new int[Campaign.values().length];
    Engine engine;
    String screen = "menu", battleMusic;
    int campaignPage, mission = 1, upgradeSelection, helpPage = -1, spell = -1, spellInfo = -1;
    boolean paused, rewardGiven;
    float scale, left, top, cameraX, cameraY, zoom = 1, mapScroll;
    float downX, downY, previousX, previousY, pointerX, pointerY, pinchDistance, pinchZoom;
    boolean moved, pinching;
    Engine.Fort dragSource, lastFort;
    Engine.March dragTroop;
    long lastTap, lastTime, resultStarted;
    boolean resultDialog;
    long resultElapsed(){return resultStarted==0?0:android.os.SystemClock.uptimeMillis()-resultStarted;}

    GameView(Context context, GameAudio audio, ProfileRepository profiles,
            GameResources resources, MatchFactory matches, Runnable onExit) {
        super(context);
        this.audio = audio;
        this.profiles = profiles;
        this.resources = resources;
        this.matches = matches;
        this.onExit = onExit;
        painter = new CanvasPainter(resources);
        units = new ArmyRenderer(resources,painter);
        paint = painter.paint();
        setFocusable(true);
        profile = profiles.load();
        save();
        mission = profile.nextStage();
        campaignPage = Campaign.forStage(mission).page;
        campaignSelection[campaignPage] = mission;
        centreCampaignSelection();
    }

    void resumeForeground() { foreground = true; invalidate(); }
    void pauseForBackground() { foreground = false; paused = engine != null; lastTime = 0; }
    GameResources resources() { return resources; }
    ArmyRenderer unitRenderer() { return units; }
    private void sound(String file) { if (profile.soundEnabled() && foreground) audio.play(file); }

    WorldLayout world() { return resources.world(mission); }
    float width() { return world().width; }
    float height() { return world().height; }
    void save() { profiles.save(profile); }
    void buffSprite(Canvas canvas, String name, int frames, float x, float y) {
        painter.natural(canvas, name + (1 + (int) (engine.time * 8) % frames), x, y, .55f);
    }

    @Override protected void onDraw(Canvas canvas) {
        canvas.drawColor(Color.BLACK);
        scale = Math.min(getWidth() / 480f, getHeight() / 320f);
        left = (getWidth() - 480 * scale) / 2;
        top = (getHeight() - 320 * scale) / 2;
        canvas.save();
        canvas.translate(left, top);
        canvas.scale(scale, scale);
        canvas.clipRect(0, 0, 480, 320);
        long now = android.os.SystemClock.uptimeMillis();
        if (engine != null) selectedTroops.removeIf(unit -> unit.n <= 0 || !engine.marches.contains(unit));
        if (engine != null && spell >= 0 && !engine.canUseSpell(spell)) spell = -1;
        if (engine != null && !paused && helpPage < 0 && engine.result == 0) {
            if (lastTime != 0) engine.tick(Math.min(.05f, (now - lastTime) / 1000f));
            lastTime = now;
        }
        if(engine!=null && engine.result!=0 && resultStarted==0)resultStarted=now;
        recordVictory();
        audio.update(engine,foreground,paused||helpPage>=0,profile.soundEnabled(),profile.musicEnabled(),battleMusic,resultElapsed()>=1000);
        audioEvents.deliver(engine,audio);
        if (engine != null) drawBattle(canvas);
        else if (screen.equals("campaign")) drawCampaign(canvas);
        else if (screen.equals("upgrade")) drawUpgrades(canvas);
        else if (screen.equals("options")) drawOptions(canvas);
        else drawMenu(canvas);
        if (helpPage >= 0) drawHelp(canvas);
        canvas.restore();
        if (foreground && (engine != null || screen.equals("campaign"))) postInvalidateOnAnimation();
    }
    void drawMenu(Canvas canvas) {
        painter.image(canvas, "ui/screens/ui_title.png", 0, 0, 480, 320);
        painter.sprite(canvas, "menu_play", 294, 71, 185, 53);
        painter.sprite(canvas, "menu_options", 294, 130, 185, 53);
        painter.sprite(canvas, "map_level", 317, 198, 140, 37);
        painter.label(canvas, "REBUILT  •  LOCAL EDITION", 240, 312, 9, 0xfff3e1ae, true);
    }
    float campaignY(float y) { return 44+y*.66f; }
    List<MapMarker> campaignMarkers() { return resources.markers(Campaign.forPage(campaignPage)); }
    void centreCampaignSelection() {
        MapMarker point=campaignMarkers().get(Campaign.forStage(mission).localIndex(mission));
        mapScroll=Math.max(0,Math.min(356,(float)point.x-240));
    }
    void chooseCampaign(int page) {
        if(Campaign.forStage(mission).page==campaignPage)campaignSelection[campaignPage]=mission;
        campaignPage=page;
        if(campaignSelection[page]>0)mission=campaignSelection[page];
        else mission=Math.max(Campaign.forPage(page).firstStage,Math.min(Campaign.forPage(page).lastStage,profile.completedStages()+1));
        centreCampaignSelection();
    }
    void drawCampaign(Canvas canvas) {
        painter.sprite(canvas,"bg_map",-mapScroll,44,836,211.2f);
        List<MapMarker> pageMarkers=campaignMarkers();
        for(int i=0;i<pageMarkers.size();i++){
            int stage=Campaign.forPage(campaignPage).stageAt(i);
            MapMarker point=pageMarkers.get(i);
            float x=(float)point.x,y=campaignY((float)point.y);
            if(!unlocked(stage))paint.setColorFilter(new LightingColorFilter(0xff666666,0));
            painter.natural(canvas,stage<=profile.completedStages()?"map_player":"map_enemy",x-mapScroll,y,.7f);
            if(stage==mission&&unlocked(stage))painter.natural(canvas,"map_sel",x-mapScroll,y+3,.85f);
            painter.number(canvas,stage,x-mapScroll,y-3,11);paint.setColorFilter(null);
        }
        for(int page=0;page<Campaign.values().length;page++){
            paint.setColor(page==campaignPage?0xee725b32:0xee292924);
            canvas.drawRoundRect(3+page*95,3,94+page*95,28,5,5,paint);
            painter.label(canvas,Campaign.forPage(page).label,48.5f+page*95,20,6.5f,
                page==campaignPage?0xffffe3a1:0xffbcb9a9,true);
        }
        painter.label(canvas,CampaignInfo.title(mission),240,43,11,0xfff7e6b6,true);
        String brief = unlocked(mission) ? CampaignInfo.brief(mission) : "Clear stage " + (mission - 1) + " to unlock this stage.";
        painter.label(canvas,brief,240,263,painter.fittedTextSize(brief,8,460),0xffffefca,true);
        painter.sprite(canvas,"back",0,265,61,55);
        painter.sprite(canvas,"map_level",75,276,140,37);
        painter.sprite(canvas,unlocked(mission)?"map_play":"map_play_d",315,269,158,48);
    }
    void drawOptions(Canvas canvas) {
        painter.image(canvas, "ui/screens/bg_options.png", 0, 0, 480, 320);
        painter.sprite(canvas, profile.soundEnabled() ? "opt_soundon" : "opt_soundoff", 126, 41, 114, 51);
        painter.sprite(canvas, profile.musicEnabled() ? "opt_musicon" : "opt_musicoff", 252, 41, 114, 51);
        painter.sprite(canvas, "opt_help", 192, 120, 114, 51);
        painter.sprite(canvas, "back", 0, 265, 61, 55);
    }
    void drawHelp(Canvas canvas) {
        paint.setColor(0xcc000000);
        canvas.drawRect(0, 0, 480, 320, paint);
        painter.image(canvas, "ui/help/" + help[helpPage] + ".png", 38, 35, 404, 241);
        painter.label(canvas, "‹", 18, 166, 30, Color.WHITE, true);
        painter.label(canvas, "›", 462, 166, 30, Color.WHITE, true);
        painter.label(canvas, "×", 456, 32, 25, Color.WHITE, true);
        painter.label(canvas, (helpPage + 1) + " / " + help.length, 240, 303, 13, Color.WHITE, true);
    }
    void drawUpgrades(Canvas canvas) {
        painter.sprite(canvas, "bg_upgrade", 0, 0, 480, 320);
        painter.sprite(canvas, "up_board_back", 58, 25, 389, 211);
        for (int i = 0; i < profile.upgradeCount(); i++) {
            int x = upgradeX(i), y = upgradeY(i);
            painter.sprite(canvas, "up_" + upgradeNames[i], x, y, 50, 50);
            if (i == PlayerProfile.TROOP_GENERATION_UPGRADE) {
                // Keep the original tile and arrow; replace its baked EXP label with editable text.
                paint.setColor(0xff686963);
                canvas.drawRoundRect(x + 4, y + 25, x + 46, y + 46, 2, 2, paint);
                painter.label(canvas, "GEN", x + 25, y + 42, 14, 0xffffc85a, true);
            }
            if (i == upgradeSelection) painter.sprite(canvas, "up_skill_sel", x - 1, y - 1, 52, 52);
            painter.sprite(canvas, "up_skill_point", x + 49, y + 36, 20, 20);
            painter.number(canvas, profile.upgradeLevel(i), x + 59, y + 40, 12);
        }
        UpgradeDescription description = UpgradeDescription.forLevel(upgradeSelection, profile.upgradeLevel(upgradeSelection));
        painter.label(canvas, description.title, 217, 243, 11, Color.WHITE, true);
        painter.label(canvas, "Current: " + description.current, 67, 258, 8, Color.WHITE, false);
        painter.label(canvas, description.next == null ? "Maximum level" : "Next: " + description.next,
            67, 272, 8, 0xffffdda0, false);
        boolean canUpgrade = profile.canUpgrade(upgradeSelection);
        painter.sprite(canvas, canUpgrade ? "up_button" : "up_button_d", 315, 282, 146, 32);
        painter.label(canvas, String.format(Locale.US, profile.upgradePoints() % 1 == 0 ? "%.0f points" : "%.1f points", profile.upgradePoints()), 170, 307, 14, Color.WHITE, true);
        painter.sprite(canvas, "back", 0, 265, 61, 55);
    }

    int upgradeX(int index) { return 68 + index % 4 * 93; }
    int upgradeY(int index) { return 32 + index / 4 * 65; }
    boolean unlocked(int stage) { return profile.canEnter(stage); }
    void recordVictory() {
        if (engine==null || engine.result!=1 || rewardGiven) return;
        rewardGiven=true;
        profile.completeStage(mission);
        save();
    }
    void startMission() {
        if (!unlocked(mission)) return;
        resources.releaseTerrain();
        int page=Campaign.forStage(mission).page;
        campaignPage=page;campaignSelection[page]=mission;centreCampaignSelection();
        engine = matches.create(mission, profile);
        resultStarted=0;resultDialog=false;
        battleMusic=audio.chooseBattleTrack();
        rewardGiven = false; paused = false; spell = spellInfo = -1; lastTime = 0;
        selected.clear(); selectedTroops.clear();
        zoom = Math.min(480 / width(), 275 / height());
        cameraX = cameraY = 0;
        audio.restartMusic();
        invalidate();
    }
    float worldX(float x) { return x / zoom + cameraX; }
    float worldY(float y) { return y / zoom + cameraY; }
    void clampCamera() {
        cameraX = Math.max(0, Math.min(Math.max(0, width() - 480 / zoom), cameraX));
        cameraY = Math.max(0, Math.min(Math.max(0, height() - 275 / zoom), cameraY));
    }
    Engine.Fort fortAt(float x, float y) {
        Engine.Fort nearest = null;
        float limit = 24 / zoom;
        for (Engine.Fort fort : engine.forts) {
            float distance = (float) Math.hypot(fort.x - worldX(x), fort.y - worldY(y));
            if (distance < limit) { limit = distance; nearest = fort; }
        }
        return nearest;
    }
    void drawBattle(Canvas canvas) {
        canvas.save();
        canvas.clipRect(0, 0, 480, 275);
        canvas.scale(zoom, zoom);
        canvas.translate(-cameraX, -cameraY);
        // Background images are tiled at their original texture size.
        if(!Campaign.forStage(mission).usesTiledBackground())painter.image(canvas,world().background,0,0,Math.max(width(),480/zoom),Math.max(height(),275/zoom));
        else for(int y=0;y<Math.max(height(),275/zoom);y+=640)
            for(int x=0;x<Math.max(width(),480/zoom);x+=960)painter.image(canvas,world().background,x,y,960,640);
        WorldLayout.Decoration overlay = world().overlay;
        if (overlay != null) painter.image(canvas, overlay.image, overlay.x - overlay.width / 2,
            overlay.y - overlay.height / 2, overlay.width, overlay.height);
        for (WorldLayout.Decoration item : world().scenery) {
            if (!item.image.equals("sanctuary")) painter.sprite(canvas, item.image,
                item.x - item.width / 2, item.y - item.height / 2, item.width, item.height);
        }
        for (Engine.Altar altar : engine.altars) {
            painter.natural(canvas, "sanctuary", altar.x, altar.y + 8, .7f);
            String altarName = altar.buff == 1 ? "immunity" : "defup";
            if (altar.cooldown <= 0) painter.natural(canvas, altarName, altar.x, altar.y - 16, .85f);
            else if (altar.cooldown > 30 - 3f/8) {
                int frame = Math.min(3, 1 + (int)((30 - altar.cooldown) * 8));
                painter.natural(canvas, altarName + "_eff" + frame, altar.x, altar.y - 16, .85f);
            }
        }
        List<Engine.Fort> ordered = new ArrayList<>(engine.forts);
        ordered.sort(Comparator.comparingDouble(f -> f.y));
        for (Engine.Fort fort : ordered) units.drawFort(canvas,fort,selected.contains(fort),engine.time);
        for (Engine.March march : engine.marches) {
            if (march.encounter != null) continue;
            if (selectedTroops.contains(march)) {
                paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(2);paint.setColor(0xffffda58);
                float radius=units.selectionRadius(march.n);
                canvas.drawOval(march.x()-radius,march.y()-radius*.6f,march.x()+radius,march.y()+radius*.4f,paint);
                paint.setStyle(Paint.Style.FILL);
            }
            String direction = Math.abs(march.b.x - march.a.x) > Math.abs(march.b.y - march.a.y)
                ? (march.b.x > march.a.x ? "e" : "w") : (march.b.y > march.a.y ? "s" : "n");
            units.drawArmy(canvas,march.side,march.originKind,march.n,march.x(),march.y(),direction,engine.time);
            if(march.age<1.8f){
                int layer=canvas.saveLayerAlpha(null,(int)(255*Math.min(1,(1.8f-march.age)/.65f)));
                painter.troopNumber(canvas,march.n,march.x(),march.y()-units.formationHeight(march.n)-12,12,march.side);
                canvas.restoreToCount(layer);
            }
            units.drawMutiny(canvas,march,march.x(),march.y()-units.formationHeight(march.n)-16);
            if (march.poisoned) {
                painter.natural(canvas, "buffer_poison" + ((int) (engine.time * 10) % 6 + 1),
                    march.x(), march.y() - 16, .6f);
                painter.label(canvas, march.poisonFlash > 0 ? "−20%" : "POISON",
                    march.x(), march.y() - 33, 9, 0xffb8ef70, true);
            }
            if (march.slowed) {
                buffSprite(canvas, "buffer_spd", 6, march.x(), march.y() - 16);
                painter.label(canvas, "½ speed", march.x(), march.y() - 33, 10, 0xffa8d6ff, true);
            }
            if (march.buff == 1) units.drawPickupBuff(canvas, march, march.x(), march.y() - 16);
            if (march.buff == 2) units.drawPickupBuff(canvas, march, march.x(), march.y() - 16);

        }
        for (Engine.Fort fort : ordered) if (fort.fight != null) units.drawFight(canvas,fort,engine.time);
        for (Engine.Encounter encounter : engine.encounters) units.drawEncounter(canvas,encounter,engine.time);
        for (Engine.Arrow arrow : engine.arrows) {
            float progress = Math.min(1, arrow.elapsed / .2f);
            float x = arrow.fromX + (arrow.toX - arrow.fromX) * progress;
            float y = arrow.fromY + (arrow.toY - arrow.fromY) * progress;
            float angle = (float) Math.toDegrees(Math.atan2(
                arrow.toY - arrow.fromY, arrow.toX - arrow.fromX));
            canvas.save();
            canvas.rotate(angle, x, y);
            painter.natural(canvas, "arrow" + (1 + (int) (arrow.elapsed * 8) % 3), x, y, .5f);
            canvas.restore();
        }
        for (Engine.Meteor meteor : engine.meteors) {
            if (!meteor.exploded) {
                float progress = Math.min(1, meteor.age / meteor.duration);
                painter.natural(canvas, "skill_meteor" + (1 + (int)(meteor.age * 8) % 5),
                    meteor.impactX + 135 * (1 - progress), meteor.impactY - 180 * (1 - progress), .75f);
            } else {
                int frame = Math.min(8, 1 + (int)((meteor.age - meteor.duration) * 12));
                painter.natural(canvas, "skill_meteor_explode" + frame, meteor.impactX, meteor.impactY, .8f);
            }
        }
        for (Engine.Storm storm : engine.storms)
            painter.natural(canvas, "skill_storm" + (1 + (int)(storm.age * 8) % 8), storm.x(), storm.y() - 15, .8f);
        for(Engine.Death death:engine.deaths){
            int layer=canvas.saveLayerAlpha(null,(int)(255*Math.min(1,(1.1f-death.age)/.4f)));
            painter.natural(canvas,"soldier_die"+death.variant,death.x+death.drift*death.age,death.y-12-death.age*32,.5f);
            canvas.restoreToCount(layer);
        }
        if (dragTroop != null && moved) {
            paint.setColor(0xbbffda58);paint.setStrokeWidth(2 / zoom);
            canvas.drawLine(dragTroop.x(),dragTroop.y(),worldX(pointerX),worldY(pointerY),paint);
        }
        if (dragSource != null && moved) {
            paint.setColor(0xbbffda58); paint.setStrokeWidth(2 / zoom);
            canvas.drawLine(dragSource.x, dragSource.y, worldX(pointerX), worldY(pointerY), paint);
        }
        for(Engine.CastNotice notice:engine.castNotices){
            paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(2/zoom);paint.setColor(painter.color(notice.side));
            canvas.drawCircle(notice.x,notice.y,24+notice.age*5,paint);paint.setStyle(Paint.Style.FILL);
        }
        canvas.restore();
        if(!engine.castNotices.isEmpty()){
            Engine.CastNotice notice=engine.castNotices.get(engine.castNotices.size()-1);
            painter.panel(canvas,115,3,250,19);
            painter.label(canvas,(notice.side==2?"BLUE":"YELLOW")+" · "+Engine.ITEMS[notice.spell],240,17,11,painter.color(notice.side),true);
        }
        painter.panel(canvas, 0, 275, 480, 45);
        painter.sprite(canvas, "btn_pause", 6, 282, 28, 30);
        painter.sprite(canvas, "icon_crystal", 43, 281, 29, 27);
        painter.number(canvas, (int) engine.energy, 93, 287, 17);
        for (int i = 0; i < 4; i++) {
            int utility = spellSlots[i];
            String state = !engine.canUseSpell(utility) ? "_i" : spell == utility ? "_d" : "";
            painter.sprite(canvas, "icon_skill" + (i + 1) + state, 270 + i * 51, 276, 44, 45);
            if (engine.cooldown[spellSlots[i]] > 0) painter.number(canvas, (int) Math.ceil(engine.cooldown[spellSlots[i]]), 292 + i * 51, 294, 15);
        }
        if (spellInfo >= 0) {
            painter.panel(canvas, 10, 225, 460, 48);
            painter.label(canvas, spellNames[spellInfo] + " Lv " + engine.skillLevels[spellInfo] + " · " + Engine.COST[spellInfo] + " crystals",
                240, 239, 12, 0xffffe3a1, true);
            painter.label(canvas, engine.spellDetails(spellInfo), 240, 254, 10, Color.WHITE, true);
            String availability = engine.cooldown[spellInfo] > 0 ? String.format(java.util.Locale.US, "Ready in %.1fs", engine.cooldown[spellInfo])
                : engine.energy < Engine.COST[spellInfo] ? "Need " + (Engine.COST[spellInfo] - (int) engine.energy) + " more crystals" : "Choose a target";
            painter.label(canvas, availability + String.format(java.util.Locale.US, " · cooldown %.1fs", engine.spellCooldown(spellInfo)), 240, 267, 10, Color.WHITE, true);
        }
        if (paused || engine.result != 0 && resultElapsed()>=1000) drawBattleOverlay(canvas);
    }


    Engine.March troopAt(float x, float y) {
        Engine.March closest=null;double distance=Double.MAX_VALUE;
        for (Engine.March unit:engine.marches) if(unit.side==1 && unit.n>0 && unit.encounter==null) {
            double d=Math.hypot(unit.x()-worldX(x),unit.y()-9-worldY(y));
            if(d<Math.max(22/zoom,units.selectionRadius(unit.n)) && d<distance){closest=unit;distance=d;}
        }
        return closest;
    }
    void deploySelection(float x,float y) {
        if(engine.command(selected,selectedTroops,fortAt(x,y),worldX(x),worldY(y))>0) {
            audio.playMovement();
            selected.clear();selectedTroops.clear();spellInfo=-1;
        }
    }

    void drawBattleOverlay(Canvas canvas) {
        if(engine.result!=0 && !resultDialog){
            paint.setColor(0xdd000000);canvas.drawRect(0,0,480,320,paint);
            String name=engine.result==1?"bg_victory":"bg_defeat";
            GameResources.SpriteFrame splash=resources.sprite(name);
            if(splash!=null){float w=440,h=w*splash.frame.height()/splash.frame.width();painter.sprite(canvas,name,240-w/2,153-h/2,w,h);}
            painter.label(canvas,"Tap to continue",240,295,13,Color.WHITE,true);return;
        }
        paint.setColor(0x88000000); canvas.drawRect(0, 0, 480, 320, paint);
        if (engine.result == 0) {
            painter.sprite(canvas, "pause_continu", 142, 97, 175, 55);
            painter.sprite(canvas, "pause_level", 142, 165, 175, 55);
        } else {
            painter.sprite(canvas, "sum_board", 84, 9, 320, 298);
            painter.sprite(canvas, engine.result == 1 ? "sum_victory" : "sum_defeat", 108, 48, 240, 98);
            painter.sprite(canvas, "sum_replay", 115, 172, 110, 45);
            painter.sprite(canvas, engine.result==1 && unlocked(mission+1) ? "sum_next" : "sum_next_d", 243, 172, 110, 45);
            painter.sprite(canvas, "sum_menu", 183, 222, 110, 45);
        }
    }

    void back() {
        if (helpPage >= 0) helpPage = -1;
        else if (engine != null) {
            if(engine.result!=0 && !resultDialog){
                if(resultElapsed()>=1000)resultDialog=true;
                invalidate();return;
            }
            if (!paused && engine.result == 0) paused = true;
            else { engine = null; screen = "campaign"; }
        } else if (!screen.equals("menu")) screen = "menu";
        else { onExit.run(); return; }
        lastTime = 0; invalidate();
    }
    boolean hit(float x, float y, float a, float b, float w, float h) {
        return x >= a && x <= a + w && y >= b && y <= b + h;
    }
    void click(float x, float y) {
        sound("effects/click.wav");
        if (helpPage >= 0) {
            if (x > 440 && y < 60) helpPage = -1;
            else if (x < 38) helpPage = Math.max(0, helpPage - 1);
            else if (x > 442) helpPage = Math.min(help.length - 1, helpPage + 1);
            return;
        }
        if (engine != null) { battleClick(x, y); return; }
        if (screen.equals("menu")) {
            if (hit(x, y, 294, 71, 185, 53)) screen = "campaign";
            else if (hit(x, y, 294, 130, 185, 53)) screen = "options";
            else if (hit(x, y, 317, 198, 140, 37)) screen = "upgrade";
        } else if (hit(x, y, 0, 265, 61, 55)) back();
        else if (screen.equals("campaign")) {
            if(y<30){chooseCampaign(Math.min(Campaign.values().length-1,(int)(x/95)));return;}
            if (hit(x, y, 315, 269, 158, 48)) startMission();
            else if (hit(x, y, 75, 276, 140, 37)) screen = "upgrade";
            else for(int i=0;i<campaignMarkers().size();i++){
                MapMarker point=campaignMarkers().get(i);
                int stage=Campaign.forPage(campaignPage).stageAt(i);
                if(Math.hypot(x+mapScroll-point.x,y-campaignY((float)point.y))<24){if(unlocked(stage))mission=stage;break;}
            }
        } else if (screen.equals("options")) {
            if (hit(x, y, 126, 41, 114, 51)) profile.toggleSound();
            if (hit(x, y, 252, 41, 114, 51)) profile.toggleMusic();
            if (hit(x, y, 192, 120, 114, 51)) helpPage = 0;
            save();
        } else if (screen.equals("upgrade")) {
            if (hit(x, y, 315, 282, 146, 32) && profile.canUpgrade(upgradeSelection)) {
                profile.purchaseUpgrade(upgradeSelection); save();
            } else for (int i = 0; i < profile.upgradeCount(); i++)
                if (hit(x, y, upgradeX(i), upgradeY(i), 60, 60)) upgradeSelection = i;
        }
    }
    void battleClick(float x, float y) {
        if (engine.result != 0) {
            if(resultElapsed()<1000)return;
            if(!resultDialog){resultDialog=true;return;}
            recordVictory();
            if (hit(x, y, 115, 172, 110, 45)) startMission();
            else if (hit(x, y, 243, 172, 110, 45) && engine.result==1 && unlocked(mission+1)) { mission++; startMission(); }
            else if (hit(x, y, 183, 222, 110, 45)) { engine = null; screen = "campaign"; }
            return;
        }
        if (paused) {
            if (hit(x, y, 142, 97, 175, 55)) { paused = false; lastTime = 0; }
            else if (hit(x, y, 142, 165, 175, 55)) { engine = null; screen = "campaign"; }
            return;
        }
        if (y >= 275) {
            if (x < 36) paused = true;
            for (int i = 0; i < 4; i++) if (hit(x, y, 270 + i * 51, 275, 44, 45)) {
                int utility=spellSlots[i];
                spellInfo=spellInfo==utility ? -1 : utility;
                spell=spellInfo>=0 && engine.canUseSpell(utility) ? utility : -1;
            }
            return;
        }
        Engine.Fort target = fortAt(x, y);
        spellInfo=-1;
        if (spell >= 0) {
            if (engine.power(spell, target, worldX(x), worldY(y))) spell = -1;
            else spellInfo=spell;
            return;
        }
        Engine.March troop=troopAt(x,y);
        if(troop!=null) {
            selected.clear();
            if(!selectedTroops.add(troop))selectedTroops.remove(troop);
        } else if(!selectedTroops.isEmpty()) deploySelection(x,y);
        else if(target != null && target.side == 1 && target.fight != null &&
                   selected.stream().anyMatch(fort -> fort != target && fort.side == 1)) deploySelection(x,y);
        else if(target != null && target.side == 1) {
            long now=android.os.SystemClock.uptimeMillis();
            if(target==lastFort && now-lastTap<350) {
                selected.clear();
                for(Engine.Fort fort:engine.forts)if(fort.side==1)selected.add(fort);
            } else if(!selected.add(target))selected.remove(target);
            lastFort=target;lastTap=now;
        } else if(!selected.isEmpty())deploySelection(x,y);
    }
    @Override public boolean performClick() { super.performClick(); return true; }
    @Override public boolean onTouchEvent(MotionEvent event) {
        float x = (event.getX() - left) / scale, y = (event.getY() - top) / scale;
        int action = event.getActionMasked();
        if(engine!=null && engine.result!=0){
            if(action==MotionEvent.ACTION_UP){battleClick(x,y);performClick();invalidate();}
            return true;
        }
        if (engine != null && !paused && helpPage < 0 && event.getPointerCount() > 1) {
            float span = (float) Math.hypot(event.getX(0) - event.getX(1), event.getY(0) - event.getY(1));
            if (!pinching) { pinchDistance = span; pinchZoom = zoom; pinching = true; }
            zoom = Math.max(Math.min(480 / width(), 275 / height()), Math.min(2, pinchZoom * span / Math.max(1, pinchDistance)));
            clampCamera(); dragSource = null; dragTroop = null; invalidate(); return true;
        }
        if (pinching) {
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) pinching = false;
            return true;
        }
        if (action == MotionEvent.ACTION_DOWN) {
            downX = previousX = pointerX = x; downY = previousY = pointerY = y; moved = false;
            dragSource = engine != null && !paused && spell < 0 && y < 275 ? fortAt(x, y) : null;
            if (dragSource != null && dragSource.side != 1) dragSource = null;
            dragTroop = engine != null && !paused && spell < 0 && y < 275 ? troopAt(x,y) : null;
            if(dragTroop!=null)dragSource=null;
            return true;
        }
        if (action == MotionEvent.ACTION_MOVE) {
            moved |= Math.hypot(x - downX, y - downY) > 6;
            if (moved && helpPage < 0) {
                if (engine == null && screen.equals("campaign")) mapScroll = Math.max(0, Math.min(356, mapScroll + previousX - x));
                else if (engine != null && !paused && dragSource == null && dragTroop == null && spell < 0 && downY < 275) {
                    cameraX += (previousX - x) / zoom; cameraY += (previousY - y) / zoom; clampCamera();
                }
            }
            pointerX = previousX = x; pointerY = previousY = y; invalidate(); return true;
        }
        if (action == MotionEvent.ACTION_UP) {
            if (!moved) { click(x, y); performClick(); }
            else if (engine != null && !paused && y < 275) {
                if (dragTroop != null) {
                    if(!selectedTroops.contains(dragTroop)){selectedTroops.clear();selectedTroops.add(dragTroop);selected.clear();}
                    deploySelection(x,y);
                } else if (dragSource != null) {
                    if(!selected.contains(dragSource)){selected.clear();selected.add(dragSource);selectedTroops.clear();}
                    deploySelection(x,y);
                }
                else if (downY >= 275) {
                    int index = (int) ((downX - 270) / 51);
                    if (index >= 0 && index < 4) engine.power(spellSlots[index], fortAt(x, y), worldX(x), worldY(y));
                }
            }
            dragSource = null; dragTroop = null; invalidate(); return true;
        }
        if (action == MotionEvent.ACTION_CANCEL) { dragSource = null; dragTroop = null; invalidate(); }
        return true;
    }
}
