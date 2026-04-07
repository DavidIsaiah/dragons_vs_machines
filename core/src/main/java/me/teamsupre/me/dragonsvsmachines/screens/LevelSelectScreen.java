package me.teamsupre.me.dragonsvsmachines.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import me.teamsupre.me.dragonsvsmachines.DragonsVsMachines;

public class LevelSelectScreen implements Screen {
    private final DragonsVsMachines game;
    private Stage stage;
    private Skin skin;

    public LevelSelectScreen(DragonsVsMachines game) {
        this.game = game;
    }

    @Override
    public void show() {
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        skin = createSkin();

        Table table = new Table();
        table.setFillParent(true);
        stage.addActor(table);

        Label title = new Label("SELECT LEVEL", skin, "title");
        table.add(title).colspan(4).padBottom(50).row();

        String[] names = {"Level 1\nSimple Tower", "Level 2\nDouble Towers", "Level 3\nFortress", "Level 4\nCompound"};
        Color[] colors = {
            new Color(0.2f, 0.7f, 0.2f, 1f),
            new Color(0.7f, 0.6f, 0.1f, 1f),
            new Color(0.8f, 0.2f, 0.2f, 1f),
            new Color(0.5f, 0.1f, 0.5f, 1f)
        };

        for (int i = 0; i < 4; i++) {
            final int level = i + 1;

            TextButton.TextButtonStyle style = new TextButton.TextButtonStyle();
            style.up = skin.newDrawable("white", colors[i]);
            style.down = skin.newDrawable("white", colors[i].cpy().mul(0.7f));
            style.over = skin.newDrawable("white", colors[i].cpy().mul(1.2f));
            style.font = skin.getFont("default-font");
            style.fontColor = Color.WHITE;

            TextButton btn = new TextButton(names[i], style);
            btn.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    game.setScreen(new GameScreen(game, level));
                }
            });
            table.add(btn).width(200).height(120).pad(15);
        }

        table.row();

        TextButton backBtn = new TextButton("BACK", skin);
        backBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new MainMenuScreen(game));
            }
        });
        table.add(backBtn).colspan(4).width(200).height(60).padTop(40);
    }

    private Skin createSkin() {
        Skin skin = new Skin();

        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        skin.add("white", new Texture(pixmap));
        pixmap.dispose();

        BitmapFont font = new BitmapFont();
        font.getData().setScale(1.5f);
        skin.add("default-font", font);

        BitmapFont titleFont = new BitmapFont();
        titleFont.getData().setScale(2.5f);
        skin.add("title-font", titleFont);

        TextButton.TextButtonStyle buttonStyle = new TextButton.TextButtonStyle();
        buttonStyle.up = skin.newDrawable("white", new Color(0.3f, 0.3f, 0.3f, 1f));
        buttonStyle.down = skin.newDrawable("white", new Color(0.2f, 0.2f, 0.2f, 1f));
        buttonStyle.over = skin.newDrawable("white", new Color(0.4f, 0.4f, 0.4f, 1f));
        buttonStyle.font = font;
        buttonStyle.fontColor = Color.WHITE;
        skin.add("default", buttonStyle);

        Label.LabelStyle titleStyle = new Label.LabelStyle();
        titleStyle.font = titleFont;
        titleStyle.fontColor = Color.WHITE;
        skin.add("title", titleStyle);

        return skin;
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.15f, 0.15f, 0.3f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        if (width <= 0 || height <= 0) return;
        stage.getViewport().update(width, height, true);
    }

    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    @Override
    public void dispose() {
        stage.dispose();
        skin.dispose();
    }
}
