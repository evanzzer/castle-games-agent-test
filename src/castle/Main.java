package castle;

import android.app.Activity;
import android.os.Bundle;
import android.content.SharedPreferences;
import android.graphics.*;
import android.media.AudioManager;
import android.view.*;
import org.json.*;
import java.io.*;
import java.util.*;

/** Android composition root: wires presentation, the match engine and platform adapters. */
public final class Main extends Activity {
    private Board board;
    private GameAudio audio;
    private boolean foreground;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        immersive();
        audio=new AndroidGameAudio(this);
        board = new Board();
        setContentView(board);
    }

    private void immersive() { getWindow().getDecorView().setSystemUiVisibility(5894); }
    @Override public void onWindowFocusChanged(boolean focused) {
        super.onWindowFocusChanged(focused);
        if (focused) immersive();
    }
    @Override protected void onResume() {
        super.onResume();
        foreground = true;
        if (board != null) board.invalidate();
    }
    @Override protected void onPause() {
        foreground = false;
        if(audio!=null)audio.pause();
        if (board != null) { board.paused = board.engine != null; board.lastTime = 0; }
        super.onPause();
    }
    @Override protected void onDestroy() { if(audio!=null)audio.close(); super.onDestroy(); }
    @Override public void onBackPressed() { board.back(); }

    private void sound(String file) { if(board.soundEnabled && foreground)audio.play(file); }

    final class Sprite {
        final Bitmap image;
        final Rect frame;
        Sprite(Bitmap image, Rect frame) { this.image = image; this.frame = frame; }
    }

    /** Converts input into game commands and renders the current match without owning platform media APIs. */
    final class Board extends View {
        final Paint paint = new Paint(3);
        final Map<String, Bitmap> images = new HashMap<>();
        final Map<String, Sprite> sprites = new HashMap<>();
        final Map<Character, Rect> digits = new HashMap<>();
        String numberFontImage;int numberFontHeight;
        final SharedPreferences preferences = getSharedPreferences("original-campaign", 0);
        final Set<Engine.Fort> selected = new LinkedHashSet<>();
        final Set<Engine.March> selectedTroops = new LinkedHashSet<>();
        final String[] help = {"help1", "help2", "help3_1", "help3_2", "help4_1",
            "help4_2", "help5_1", "help5_2", "help6", "help7"};
        static final int MAX_UPGRADE_LEVEL = UpgradeRules.MAX_LEVEL;
        final String[] upgradeNames = {"atk", "def", "spd", "exp", "crys_init",
            "crys_add", "skill_meteor", "skill_storm", "skill_mutiny", "skill_reinf", "cd"};
        final String[] upgradeLabels = {"Attack", "Defense", "Speed", "Experience", "Initial crystals",
            "Crystal production", "Meteor", "Storm", "Mutiny", "Reinforce", "Cooldown"};
        final int[] upgrades = new int[11];
        final int[] spellSlots = {0, 2, 1, 3};
        int heardRaidSerial;
        final String[] spellNames = {"Meteor", "Storm", "Mutiny", "Reinforce"};
        JSONArray worlds, markers, frontierMarkers, wildsMarkers;
        final int[] campaignSelection = new int[Campaign.values().length];
        Engine engine;
        String screen = "menu", battleMusic;
        int campaignPage, mission = 1, completed, points, upgradeSelection, helpPage = -1, spell = -1, spellInfo = -1;
        boolean paused, rewardGiven, musicEnabled, soundEnabled;
        float scale, left, top, cameraX, cameraY, zoom = 1, mapScroll;
        float downX, downY, previousX, previousY, pointerX, pointerY, pinchDistance, pinchZoom;
        boolean moved, pinching;
        Engine.Fort dragSource, lastFort;
        Engine.March dragTroop;
        long lastTap, lastTime, resultStarted;
        boolean resultDialog;
        long resultElapsed(){return resultStarted==0?0:android.os.SystemClock.uptimeMillis()-resultStarted;}

        Board() {
            super(Main.this);
            setFocusable(true);
            completed = Math.max(0, Math.min(Engine.TOTAL_STAGES, preferences.getInt("completed", 0)));
            points = preferences.getInt("points", 0);
            musicEnabled = preferences.getBoolean("music", true);
            soundEnabled = preferences.getBoolean("sound", true);
            if (preferences.getBoolean("five-upgrades", false)) {
                for (int i = 0; i < 3; i++) upgrades[i] = preferences.getInt("up" + i, 0);
                upgrades[5] = preferences.getInt("up3", 0);
                int spellLevel = preferences.getInt("up4", 0);
                for (int i = 6; i <= 9; i++) upgrades[i] = Math.min(MAX_UPGRADE_LEVEL, spellLevel);
                points += Math.max(0, spellLevel - MAX_UPGRADE_LEVEL);
            } else {
                for (int i = 0; i < upgrades.length; i++) upgrades[i] = preferences.getInt("up" + i, 0);
            }
            points += UpgradeRules.normalizeAndRefund(upgrades);
            save();
            try {
                JSONObject manifest=AssetMetadata.read(getAssets(),"manifest.json");
                JSONArray atlases=manifest.getJSONArray("atlases");
                for(int i=0;i<atlases.length();i++)loadAtlas(atlases.getString(i));
                JSONObject navigation=AssetMetadata.read(getAssets(),manifest.getString("campaigns")).getJSONObject("campaigns");
                JSONObject terrainPaths=manifest.getJSONObject("terrain");
                worlds=new JSONArray();
                for(Campaign campaign:Campaign.values()){
                    JSONObject terrain=AssetMetadata.read(getAssets(),terrainPaths.getJSONObject(campaign.id).getString("worlds"));
                    JSONArray levels=terrain.getJSONArray("worlds");
                    if(!terrain.getString("campaign").equals(campaign.id)||terrain.getInt("first_stage")!=campaign.firstStage||levels.length()!=Campaign.STAGES_PER_CAMPAIGN)
                        throw new JSONException("Invalid terrain world group: "+campaign.id);
                    for(int i=0;i<levels.length();i++)worlds.put(levels.getJSONObject(i));
                }
                markers=navigation.getJSONObject(Campaign.ORIGINAL.id).getJSONArray("markers");
                frontierMarkers=navigation.getJSONObject(Campaign.FRONTIER.id).getJSONArray("markers");
                wildsMarkers=navigation.getJSONObject(Campaign.WILD.id).getJSONArray("markers");
                mission=Math.min(Engine.TOTAL_STAGES,completed+1);
                campaignPage=Campaign.forStage(mission).page;campaignSelection[campaignPage]=mission;
                centreCampaignSelection();
                JSONObject font=AssetMetadata.read(getAssets(),manifest.getString("number_font"));
                numberFontImage=font.getString("image");numberFontHeight=font.getInt("line_height");
                JSONObject glyphs=font.getJSONObject("glyphs");
                for(Iterator<String> it=glyphs.keys();it.hasNext();){String code=it.next();
                    digits.put((char)Integer.parseInt(code),AssetMetadata.frame(glyphs.getJSONObject(code)));
                }
            } catch (Exception failure) { throw new IllegalStateException("Game assets could not load", failure); }
        }

        Bitmap bitmap(String name) {
            if (images.containsKey(name)) return images.get(name);
            Bitmap result;
            try (InputStream stream = getAssets().open(name)) { result = BitmapFactory.decodeStream(stream); }
            catch (IOException failure) { result = null; }
            images.put(name, result);
            return result;
        }
        void loadAtlas(String file) throws IOException,JSONException {
            JSONObject atlas=AssetMetadata.read(getAssets(),file);
            Bitmap image=bitmap(atlas.getString("image"));
            if(image==null)throw new IOException("Missing atlas image: "+file);
            JSONObject frames=atlas.getJSONObject("frames");
            for(Iterator<String> it=frames.keys();it.hasNext();){String name=it.next();
                Rect bounds=AssetMetadata.frame(frames.getJSONObject(name));
                if(bounds.right>image.getWidth()||bounds.bottom>image.getHeight())throw new IOException("Atlas frame outside image: "+name);
                sprites.put(name,new Sprite(image,bounds));
            }
        }
        void image(Canvas canvas, String file, float x, float y, float width, float height) {
            Bitmap bitmap = bitmap(file);
            if (bitmap == null) return;
            paint.setAlpha(255);
            canvas.drawBitmap(bitmap, null, new RectF(x, y, x + width, y + height), paint);
        }
        void sprite(Canvas canvas, String name, float x, float y, float width, float height) {
            Sprite sprite = sprites.get(name);
            if (sprite == null) return;
            paint.setAlpha(255);
            canvas.drawBitmap(sprite.image, sprite.frame, new RectF(x, y, x + width, y + height), paint);
        }
        void natural(Canvas canvas, String name, float x, float y, float size) {
            Sprite sprite = sprites.get(name);
            if (sprite != null) sprite(canvas, name, x - sprite.frame.width() * size / 2,
                y - sprite.frame.height() * size / 2, sprite.frame.width() * size, sprite.frame.height() * size);
        }
        void label(Canvas canvas, String text, float x, float y, float size, int color, boolean centered) {
            paint.setColor(color);
            paint.setTypeface(Typeface.create("serif", Typeface.BOLD));
            paint.setTextAlign(centered ? Paint.Align.CENTER : Paint.Align.LEFT);
            paint.setTextSize(size);
            paint.setShadowLayer(2, 0, 1, Color.BLACK);
            canvas.drawText(text, x, y, paint);
            paint.clearShadowLayer();
        }
        void number(Canvas canvas, int value, float x, float y, float height) {
            String text = Integer.toString(Math.max(0, value));
            float width = 0;
            for (char ch : text.toCharArray()) {
                Rect frame = digits.get(ch);
                if (frame != null) width += frame.width() * height / numberFontHeight;
            }
            float pen = x - width / 2;
            for (char ch : text.toCharArray()) {
                Rect frame = digits.get(ch);
                if (frame == null) continue;
                float w = frame.width() * height / numberFontHeight;
                paint.setAlpha(255);
                canvas.drawBitmap(bitmap(numberFontImage), frame, new RectF(pen, y, pen + w, y + height), paint);
                pen += w;
            }
        }
        void troopNumber(Canvas canvas, int value, float x, float y, float height, int side) {
            paint.setColorFilter(new LightingColorFilter(color(side), 0));
            number(canvas, value, x, y, height);
            paint.setColorFilter(null);
        }
        void buffSprite(Canvas canvas, String name, int frames, float x, float y) {
            natural(canvas, name + (1 + (int)(engine.time * 8) % frames), x, y, .55f);
        }
        void panel(Canvas canvas, float x, float y, float w, float h) {
            paint.setColor(0xe6211c19);
            canvas.drawRoundRect(x, y, x + w, y + h, 4, 4, paint);
        }
        int color(int side) { return new int[]{0xffb3cba2, 0xffff7066, 0xff7dabff, 0xffffdf70}[side]; }
        JSONObject world() { return worlds.optJSONObject(mission - 1); }
        float width() { return (float) world().optDouble("width", 480); }
        float height() { return (float) world().optDouble("height", 320); }
        void save() {
            SharedPreferences.Editor editor = preferences.edit().putInt("completed", completed)
                .putInt("points", points).putBoolean("five-upgrades", false).putBoolean("music", musicEnabled).putBoolean("sound", soundEnabled);
            for (int i = 0; i < upgrades.length; i++) editor.putInt("up" + i, upgrades[i]);
            editor.apply();
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
            audio.update(engine,foreground,paused||helpPage>=0,soundEnabled,musicEnabled,battleMusic,resultElapsed()>=1000);
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
            image(canvas, "ui/screens/ui_title.png", 0, 0, 480, 320);
            sprite(canvas, "menu_play", 294, 71, 185, 53);
            sprite(canvas, "menu_options", 294, 130, 185, 53);
            sprite(canvas, "map_level", 317, 198, 140, 37);
            label(canvas, "REBUILT  •  LOCAL EDITION", 240, 312, 9, 0xfff3e1ae, true);
        }
        float campaignY(float y) { return 44+y*.66f; }
        JSONArray campaignMarkers() {
            switch(Campaign.forPage(campaignPage)){case ORIGINAL:return markers;case FRONTIER:return frontierMarkers;default:return wildsMarkers;}
        }
        void centreCampaignSelection() {
            JSONArray point=campaignMarkers().optJSONArray(Campaign.forStage(mission).localIndex(mission));
            mapScroll=Math.max(0,Math.min(356,(float)point.optDouble(0)-240));
        }
        void chooseCampaign(int page) {
            if(Campaign.forStage(mission).page==campaignPage)campaignSelection[campaignPage]=mission;
            campaignPage=page;
            if(campaignSelection[page]>0)mission=campaignSelection[page];
            else mission=Math.max(Campaign.forPage(page).firstStage,Math.min(Campaign.forPage(page).lastStage,completed+1));
            centreCampaignSelection();
        }
        void drawCampaign(Canvas canvas) {
            sprite(canvas,"bg_map",-mapScroll,44,836,211.2f);
            JSONArray pageMarkers=campaignMarkers();
            for(int i=0;i<pageMarkers.length();i++){
                int stage=Campaign.forPage(campaignPage).stageAt(i);
                JSONArray point=pageMarkers.optJSONArray(i);
                float x=(float)point.optDouble(0),y=campaignY((float)point.optDouble(1));
                if(!unlocked(stage))paint.setColorFilter(new LightingColorFilter(0xff666666,0));
                natural(canvas,stage<=completed?"map_player":"map_enemy",x-mapScroll,y,.7f);
                if(stage==mission&&unlocked(stage))natural(canvas,"map_sel",x-mapScroll,y+3,.85f);
                number(canvas,stage,x-mapScroll,y-3,11);paint.setColorFilter(null);
            }
            for(int page=0;page<Campaign.values().length;page++){
                paint.setColor(page==campaignPage?0xee725b32:0xee292924);
                canvas.drawRoundRect(5+page*160,3,155+page*160,28,5,5,paint);
                label(canvas,Campaign.forPage(page).label,80+page*160,20,10,
                    page==campaignPage?0xffffe3a1:0xffbcb9a9,true);
            }
            label(canvas,CampaignInfo.title(mission),240,43,11,0xfff7e6b6,true);
            label(canvas,unlocked(mission)?CampaignInfo.brief(mission):"Clear stage "+(mission-1)+" to unlock this stage.",
                240,263,8,0xffffefca,true);
            sprite(canvas,"back",0,265,61,55);
            sprite(canvas,"map_level",75,276,140,37);
            sprite(canvas,unlocked(mission)?"map_play":"map_play_d",315,269,158,48);
        }
        void drawOptions(Canvas canvas) {
            image(canvas, "ui/screens/bg_options.png", 0, 0, 480, 320);
            sprite(canvas, soundEnabled ? "opt_soundon" : "opt_soundoff", 126, 41, 114, 51);
            sprite(canvas, musicEnabled ? "opt_musicon" : "opt_musicoff", 252, 41, 114, 51);
            sprite(canvas, "opt_help", 192, 120, 114, 51);
            sprite(canvas, "back", 0, 265, 61, 55);
        }
        void drawHelp(Canvas canvas) {
            paint.setColor(0xcc000000);
            canvas.drawRect(0, 0, 480, 320, paint);
            image(canvas, "ui/help/" + help[helpPage] + ".png", 38, 35, 404, 241);
            label(canvas, "‹", 18, 166, 30, Color.WHITE, true);
            label(canvas, "›", 462, 166, 30, Color.WHITE, true);
            label(canvas, "×", 456, 32, 25, Color.WHITE, true);
            label(canvas, (helpPage + 1) + " / " + help.length, 240, 303, 13, Color.WHITE, true);
        }
        void drawUpgrades(Canvas canvas) {
            sprite(canvas, "bg_upgrade", 0, 0, 480, 320);
            sprite(canvas, "up_board_back", 58, 25, 389, 211);
            for (int i = 0; i < upgrades.length; i++) {
                int x = upgradeX(i), y = upgradeY(i);
                sprite(canvas, "up_" + upgradeNames[i], x, y, 50, 50);
                if (i == upgradeSelection) sprite(canvas, "up_skill_sel", x - 1, y - 1, 52, 52);
                sprite(canvas, "up_skill_point", x + 49, y + 36, 20, 20);
                number(canvas, upgrades[i], x + 59, y + 40, 12);
            }
            sprite(canvas, "up_des" + upgradeSelection, 67, 237, 300, 41);
            boolean canUpgrade = UpgradeRules.canBuy(upgrades[upgradeSelection], points);
            sprite(canvas, canUpgrade ? "up_button" : "up_button_d", 315, 282, 146, 32);
            label(canvas, points + " points", 170, 307, 14, Color.WHITE, true);
            sprite(canvas, "back", 0, 265, 61, 55);
        }

        int upgradeX(int index) { return 68 + index % 4 * 93; }
        int upgradeY(int index) { return 32 + index / 4 * 65; }
        boolean unlocked(int stage) { return stage>=1 && stage<=Engine.TOTAL_STAGES && stage<=completed+1; }
        void recordVictory() {
            if (engine==null || engine.result!=1 || rewardGiven) return;
            rewardGiven=true;
            if (mission==completed+1) completed=mission;
            points+=2+upgrades[3]/3;
            save();
        }
        void startMission() {
            if (!unlocked(mission)) return;
            images.keySet().removeIf(name -> name.startsWith("terrain/"));
            int page=Campaign.forStage(mission).page;
            campaignPage=page;campaignSelection[page]=mission;centreCampaignSelection();
            engine = new Engine(mission, 0);
            engine.attackLevels[1] = upgrades[0];
            engine.defenseLevels[1] = upgrades[1];
            engine.speedLevel = upgrades[2];
            engine.crystalLevel = upgrades[5];
            engine.cooldownLevel = upgrades[10];
            engine.energy = 200 * (1 + upgrades[4] * .25f);
            for (int i = 0; i < 4; i++) engine.skillLevels[i] = upgrades[6 + i];
            heardRaidSerial = 0;resultStarted=0;resultDialog=false;
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
            if(!Campaign.forStage(mission).usesTiledBackground())image(canvas,"map/backgrounds/map1.png",0,0,Math.max(width(),480/zoom),Math.max(height(),275/zoom));
            else for(int y=0;y<Math.max(height(),275/zoom);y+=640)
                for(int x=0;x<Math.max(width(),480/zoom);x+=960)image(canvas,"map/backgrounds/map1.png",x,y,960,640);
            JSONArray overlay = world().optJSONArray("overlay");
            if (overlay != null) {
                float w = (float) overlay.optDouble(3), h = (float) overlay.optDouble(4);
                image(canvas, overlay.optString(0),
                    (float) overlay.optDouble(1) - w / 2, (float) overlay.optDouble(2) - h / 2, w, h);
            }
            JSONArray objects = world().optJSONArray("objects");
            if (objects != null) for (int i = 0; i < objects.length(); i++) {
                JSONArray item = objects.optJSONArray(i);
                if (item.optString(0).equals("sanctuary")) continue;
                float w = (float) item.optDouble(3), h = (float) item.optDouble(4);
                sprite(canvas, item.optString(0), (float) item.optDouble(1) - w / 2,
                    (float) item.optDouble(2) - h / 2, w, h);
            }
            for (Engine.Altar altar : engine.altars) {
                natural(canvas, "sanctuary", altar.x, altar.y + 8, .7f);
                String altarName = altar.buff == 1 ? "immunity" : "defup";
                if (altar.cooldown <= 0) natural(canvas, altarName, altar.x, altar.y - 16, .85f);
                else if (altar.cooldown > 30 - 3f/8) {
                    int frame = Math.min(3, 1 + (int)((30 - altar.cooldown) * 8));
                    natural(canvas, altarName + "_eff" + frame, altar.x, altar.y - 16, .85f);
                }
            }
            List<Engine.Fort> ordered = new ArrayList<>(engine.forts);
            ordered.sort(Comparator.comparingDouble(f -> f.y));
            for (Engine.Fort fort : ordered) drawFort(canvas, fort);
            for (Engine.March march : engine.marches) {
                if (march.encounter != null) continue;
                if (selectedTroops.contains(march)) {
                    paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(2);paint.setColor(0xffffda58);
                    float radius=selectionRadius(march.n);
                    canvas.drawOval(march.x()-radius,march.y()-radius*.6f,march.x()+radius,march.y()+radius*.4f,paint);
                    paint.setStyle(Paint.Style.FILL);
                }
                String direction = Math.abs(march.b.x - march.a.x) > Math.abs(march.b.y - march.a.y)
                    ? (march.b.x > march.a.x ? "e" : "w") : (march.b.y > march.a.y ? "s" : "n");
                drawArmy(canvas,march.side,march.originKind,march.n,march.x(),march.y(),direction);
                if(march.age<1.8f){
                    int layer=canvas.saveLayerAlpha(null,(int)(255*Math.min(1,(1.8f-march.age)/.65f)));
                    troopNumber(canvas,march.n,march.x(),march.y()-formationHeight(march.n)-12,12,march.side);
                    canvas.restoreToCount(layer);
                }
                drawMutiny(canvas,march,march.x(),march.y()-formationHeight(march.n)-16);
                if (march.poisoned) {
                    natural(canvas, "buffer_poison" + ((int) (engine.time * 10) % 6 + 1),
                        march.x(), march.y() - 16, .6f);
                    label(canvas, march.poisonFlash > 0 ? "−20%" : "POISON",
                        march.x(), march.y() - 33, 9, 0xffb8ef70, true);
                }
                if (march.slowed) {
                    buffSprite(canvas, "buffer_spd", 6, march.x(), march.y() - 16);
                    label(canvas, "½ speed", march.x(), march.y() - 33, 10, 0xffa8d6ff, true);
                }
                if (march.buff == 1) drawPickupBuff(canvas, march, march.x(), march.y() - 16);
                if (march.buff == 2) drawPickupBuff(canvas, march, march.x(), march.y() - 16);

            }
            for (Engine.Fort fort : ordered) if (fort.fight != null) drawFight(canvas, fort);
            for (Engine.Encounter encounter : engine.encounters) drawEncounter(canvas, encounter);
            while (heardRaidSerial < engine.raidSerial) {
                heardRaidSerial++;
                audio.playBattleStart();
            }
            for (Engine.Arrow arrow : engine.arrows) {
                float progress = Math.min(1, arrow.elapsed / .2f);
                float x = arrow.fromX + (arrow.toX - arrow.fromX) * progress;
                float y = arrow.fromY + (arrow.toY - arrow.fromY) * progress;
                float angle = (float) Math.toDegrees(Math.atan2(
                    arrow.toY - arrow.fromY, arrow.toX - arrow.fromX));
                canvas.save();
                canvas.rotate(angle, x, y);
                natural(canvas, "arrow" + (1 + (int) (arrow.elapsed * 8) % 3), x, y, .5f);
                canvas.restore();
            }
            for (Engine.Meteor meteor : engine.meteors) {
                if (!meteor.exploded) {
                    float progress = Math.min(1, meteor.age / meteor.duration);
                    natural(canvas, "skill_meteor" + (1 + (int)(meteor.age * 8) % 5),
                        meteor.impactX + 135 * (1 - progress), meteor.impactY - 180 * (1 - progress), .75f);
                } else {
                    int frame = Math.min(8, 1 + (int)((meteor.age - meteor.duration) * 12));
                    natural(canvas, "skill_meteor_explode" + frame, meteor.impactX, meteor.impactY, .8f);
                }
            }
            for (Engine.Storm storm : engine.storms)
                natural(canvas, "skill_storm" + (1 + (int)(storm.age * 8) % 8), storm.x(), storm.y() - 15, .8f);
            for(Engine.Death death:engine.deaths){
                int layer=canvas.saveLayerAlpha(null,(int)(255*Math.min(1,(1.1f-death.age)/.4f)));
                natural(canvas,"soldier_die"+death.variant,death.x+death.drift*death.age,death.y-12-death.age*32,.5f);
                canvas.restoreToCount(layer);
            }
            for(Engine.AudioCue cue:engine.audioCues)audio.play(cue);
            engine.audioCues.clear();
            if (dragTroop != null && moved) {
                paint.setColor(0xbbffda58);paint.setStrokeWidth(2 / zoom);
                canvas.drawLine(dragTroop.x(),dragTroop.y(),worldX(pointerX),worldY(pointerY),paint);
            }
            if (dragSource != null && moved) {
                paint.setColor(0xbbffda58); paint.setStrokeWidth(2 / zoom);
                canvas.drawLine(dragSource.x, dragSource.y, worldX(pointerX), worldY(pointerY), paint);
            }
            for(Engine.CastNotice notice:engine.castNotices){
                paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(2/zoom);paint.setColor(color(notice.side));
                canvas.drawCircle(notice.x,notice.y,24+notice.age*5,paint);paint.setStyle(Paint.Style.FILL);
            }
            canvas.restore();
            if(!engine.castNotices.isEmpty()){
                Engine.CastNotice notice=engine.castNotices.get(engine.castNotices.size()-1);
                panel(canvas,115,3,250,19);
                label(canvas,(notice.side==2?"BLUE":"YELLOW")+" · "+Engine.ITEMS[notice.spell],240,17,11,color(notice.side),true);
            }
            panel(canvas, 0, 275, 480, 45);
            sprite(canvas, "btn_pause", 6, 282, 28, 30);
            sprite(canvas, "icon_crystal", 43, 281, 29, 27);
            number(canvas, (int) engine.energy, 93, 287, 17);
            for (int i = 0; i < 4; i++) {
                int utility = spellSlots[i];
                String state = !engine.canUseSpell(utility) ? "_i" : spell == utility ? "_d" : "";
                sprite(canvas, "icon_skill" + (i + 1) + state, 270 + i * 51, 276, 44, 45);
                if (engine.cooldown[spellSlots[i]] > 0) number(canvas, (int) Math.ceil(engine.cooldown[spellSlots[i]]), 292 + i * 51, 294, 15);
            }
            if (spellInfo >= 0) {
                panel(canvas, 10, 225, 460, 48);
                label(canvas, spellNames[spellInfo] + " Lv " + engine.skillLevels[spellInfo] + " · " + Engine.COST[spellInfo] + " crystals",
                    240, 239, 12, 0xffffe3a1, true);
                label(canvas, engine.spellDetails(spellInfo), 240, 254, 10, Color.WHITE, true);
                String availability = engine.cooldown[spellInfo] > 0 ? String.format(java.util.Locale.US, "Ready in %.1fs", engine.cooldown[spellInfo])
                    : engine.energy < Engine.COST[spellInfo] ? "Need " + (Engine.COST[spellInfo] - (int) engine.energy) + " more crystals" : "Choose a target";
                label(canvas, availability + String.format(java.util.Locale.US, " · cooldown %.1fs", engine.spellCooldown(spellInfo)), 240, 267, 10, Color.WHITE, true);
            }
            if (paused || engine.result != 0 && resultElapsed()>=1000) drawBattleOverlay(canvas);
        }
        String fortArt(Engine.Fort fort){return fort.kind==Engine.GENERATOR?"house_"+fort.size:fort.kind==Engine.ARCHERY?"tower_"+fort.size:"mine";}
        // Architecture::SetMapObj / SetNation tables, with the original Y axis inverted.
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
            Sprite body=sprites.get("grby".charAt(fort.side)+"_"+fortArt(fort));
            float roof=fort.visualY-(body==null?40:body.frame.height()*.65f/2);
            if(fort.side!=0){float[] flag=flagOffset(fort);char size=fort.kind==Engine.CRYSTAL?'m':fort.size;
                Sprite cloth=sprites.get("flag_"+"grby".charAt(fort.side)+"_"+size+"1");
                if(cloth!=null)roof=Math.min(roof,fort.visualY-flag[1]*.65f-cloth.frame.height()*.65f/2);
            }
            return roof-5;
        }
        void drawFort(Canvas canvas, Engine.Fort fort) {
            String kind=fortArt(fort);float[] shadow=shadowOffset(fort);
            natural(canvas,kind+"_s",fort.visualX+shadow[0]*.65f,fort.visualY+shadow[1]*.65f,.65f);
            natural(canvas,"grby".charAt(fort.side)+"_"+kind+(selected.contains(fort)?"_d":""),fort.visualX,fort.visualY,.65f);
            if(fort.side!=0){
                float[] flag=flagOffset(fort);char size=fort.kind==Engine.CRYSTAL?'m':fort.size;
                int frames=size=='s'?5:6;
                natural(canvas,"flag_"+"grby".charAt(fort.side)+"_"+size+(1+(int)(engine.time*9)%frames),
                    fort.visualX+flag[0]*.65f,fort.visualY-flag[1]*.65f,.65f);
            }
            if(fort.fight==null)troopNumber(canvas,(int)fort.n,fort.visualX,fortNumberY(fort),15,fort.side);
        }
        float formationHeight(int n){return 10+(float)Math.ceil(Math.sqrt(Engine.soldierCount(n)))*3;}
        float selectionRadius(int n){return 14+(float)Math.ceil(Math.sqrt(Engine.soldierCount(n)))*3;}
        void drawArmy(Canvas canvas,int side,int kind,int n,float x,float y,String direction){
            int count=Engine.soldierCount(n),columns=(int)Math.ceil(Math.sqrt(count));
            int rows=columns==0?0:(count+columns-1)/columns;
            int variant=soldierVariant(kind);
            for(int row=0,index=0;row<rows;row++)for(int col=0;col<columns&&index<count;col++,index++){
                int frame=1+(int)(engine.time*12+index*1.7f)%(variant==3?4:8);
                float dx=(col-(columns-1)/2f)*6+(row%2)*2;
                float dy=(row-(rows-1)/2f)*4;
                natural(canvas,"soldier_"+"brby".charAt(side)+"_"+variant+"_"+direction+frame,x+dx,y+dy-11,.48f);
            }
        }
        void drawMutiny(Canvas canvas,Engine.March unit,float x,float y){
            if(unit.mutinyAge>=1.35f)return;
            int layer=canvas.saveLayerAlpha(null,(int)(255*Math.min(1,(1.35f-unit.mutinyAge)/.45f)));
            natural(canvas,"skill_mutiny"+Math.min(8,1+(int)(unit.mutinyAge*8)),x,y,.55f);
            canvas.restoreToCount(layer);
        }
        void drawPickupBuff(Canvas canvas, Engine.March unit, float x, float y) {
            String name=unit.buff==1 ? "buffer_immunity" : "buffer_def";
            int frames=unit.buff==1 ? 5 : 4;
            // Play once, retaining the last frame as a status icon until expiry.
            natural(canvas, name + Math.min(frames, 1+(int)(unit.buffAge*8)), x, y, .6f);
        }
        Engine.March troopAt(float x, float y) {
            Engine.March closest=null;double distance=Double.MAX_VALUE;
            for (Engine.March unit:engine.marches) if(unit.side==1 && unit.n>0 && unit.encounter==null) {
                double d=Math.hypot(unit.x()-worldX(x),unit.y()-9-worldY(y));
                if(d<Math.max(22/zoom,selectionRadius(unit.n)) && d<distance){closest=unit;distance=d;}
            }
            return closest;
        }
        void deploySelection(float x,float y) {
            if(engine.command(selected,selectedTroops,fortAt(x,y),worldX(x),worldY(y))>0) {
                audio.playMovement();
                selected.clear();selectedTroops.clear();spellInfo=-1;
            }
        }
        int soldierVariant(int kind) {
            return kind == Engine.GENERATOR ? 1 : kind == Engine.CRYSTAL ? 2 : 3;
        }
        void drawFight(Canvas canvas, Engine.Fort fort) {
            String cloudName="battle"+(1+(int)(engine.time*10)%4);
            Sprite cloud=sprites.get(cloudName);
            if(cloud!=null){
                float width=cloud.frame.width()*.7f,height=cloud.frame.height()*.9f;
                sprite(canvas,cloudName,fort.x-width/2,fort.y-16-height/2,width,height);
            }
            int count=0;for(int side=1;side<4;side++)if(fort.fight.troops[side]>0)count++;
            float y=fortNumberY(fort)-count*15;
            for(int side=1;side<4;side++)if(fort.fight.troops[side]>0){
                troopNumber(canvas,(int)fort.fight.troops[side],fort.visualX,y,14,side);y+=15;
            }
            troopNumber(canvas,(int)fort.n,fort.visualX,y,14,fort.side);
        }
        void drawEncounter(Canvas canvas, Engine.Encounter encounter) {
            natural(canvas,"battle_troop"+(1+(int)(engine.time*10)%4),encounter.x,encounter.y-5,.65f);
            int index=0,teams=encounter.teams();
            for(int side=1;side<4;side++)if(encounter.total(side)>0){
                troopNumber(canvas,(int)encounter.total(side),encounter.x,encounter.y-38-(teams-1-index)*15,13,side);
                for(Engine.March unit:encounter.units)if(unit.side==side&&unit.n>0){
                    if(unit.buff!=0)drawPickupBuff(canvas,unit,encounter.x+(index-(teams-1)/2f)*20,encounter.y-25);
                    drawMutiny(canvas,unit,encounter.x,encounter.y-38);break;
                }
                index++;
            }
        }
        void drawBattleOverlay(Canvas canvas) {
            if(engine.result!=0 && !resultDialog){
                paint.setColor(0xdd000000);canvas.drawRect(0,0,480,320,paint);
                String name=engine.result==1?"bg_victory":"bg_defeat";
                Sprite splash=sprites.get(name);
                if(splash!=null){float w=440,h=w*splash.frame.height()/splash.frame.width();sprite(canvas,name,240-w/2,153-h/2,w,h);}
                label(canvas,"Tap to continue",240,295,13,Color.WHITE,true);return;
            }
            paint.setColor(0x88000000); canvas.drawRect(0, 0, 480, 320, paint);
            if (engine.result == 0) {
                sprite(canvas, "pause_continu", 142, 97, 175, 55);
                sprite(canvas, "pause_level", 142, 165, 175, 55);
            } else {
                sprite(canvas, "sum_board", 84, 9, 320, 298);
                sprite(canvas, engine.result == 1 ? "sum_victory" : "sum_defeat", 108, 48, 240, 98);
                sprite(canvas, "sum_replay", 115, 172, 110, 45);
                sprite(canvas, engine.result==1 && unlocked(mission+1) ? "sum_next" : "sum_next_d", 243, 172, 110, 45);
                sprite(canvas, "sum_menu", 183, 222, 110, 45);
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
            else { Main.this.finish(); return; }
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
                if(y<30){chooseCampaign(Math.min(2,(int)(x/160)));return;}
                if (hit(x, y, 315, 269, 158, 48)) startMission();
                else if (hit(x, y, 75, 276, 140, 37)) screen = "upgrade";
                else for(int i=0;i<campaignMarkers().length();i++){
                    JSONArray point=campaignMarkers().optJSONArray(i);
                    int stage=Campaign.forPage(campaignPage).stageAt(i);
                    if(Math.hypot(x+mapScroll-point.optDouble(0),y-campaignY((float)point.optDouble(1)))<24){if(unlocked(stage))mission=stage;break;}
                }
            } else if (screen.equals("options")) {
                if (hit(x, y, 126, 41, 114, 51)) soundEnabled = !soundEnabled;
                if (hit(x, y, 252, 41, 114, 51)) musicEnabled = !musicEnabled;
                if (hit(x, y, 192, 120, 114, 51)) helpPage = 0;
                save();
            } else if (screen.equals("upgrade")) {
                if (hit(x, y, 315, 282, 146, 32) && UpgradeRules.canBuy(upgrades[upgradeSelection], points)) {
                    points--; upgrades[upgradeSelection]++; save();
                } else for (int i = 0; i < upgrades.length; i++)
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
}
