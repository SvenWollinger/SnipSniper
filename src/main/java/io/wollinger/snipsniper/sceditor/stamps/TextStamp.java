package io.wollinger.snipsniper.sceditor.stamps;

import io.wollinger.snipsniper.Config;
import io.wollinger.snipsniper.sceditor.SCEditorWindow;
import io.wollinger.snipsniper.utils.InputContainer;
import io.wollinger.snipsniper.utils.PBRColor;
import io.wollinger.snipsniper.utils.Vector2Int;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.ArrayList;

public class TextStamp implements IStamp{

    private PBRColor color;
    private int fontSize;
    private int fontSizeSpeed;
    private String text;

    private final ArrayList<Integer> nonTypeKeys = new ArrayList<>();

    private int fontMode = Font.PLAIN;
    private final SCEditorWindow scEditorWindow;

    private TextState state = TextState.IDLE;
    private Vector2Int position = new Vector2Int();
    private Vector2Int livePosition = new Vector2Int();
    private boolean doSaveNextRender = false;

    enum TextState {IDLE, TYPING};

    public TextStamp(SCEditorWindow scEditorWindow) {
        this.scEditorWindow = scEditorWindow;

        nonTypeKeys.add(KeyEvent.VK_SHIFT);
        nonTypeKeys.add(KeyEvent.VK_CONTROL);
        nonTypeKeys.add(KeyEvent.VK_ALT);
        reset();
    }

    @Override
    public void update(InputContainer input, int mouseWheelDirection, KeyEvent keyEvent) {
        if(mouseWheelDirection == 1)
            fontSize -= fontSizeSpeed;
        else if(mouseWheelDirection == -1)
            fontSize += fontSizeSpeed;

        if(input.areKeysPressed(KeyEvent.VK_CONTROL, KeyEvent.VK_B)) {
            fontMode++;
            if(fontMode > 2)
                fontMode = 0;
        }

        if(keyEvent != null && state == TextState.TYPING) {
            if (keyEvent.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
                if (text.length() > 0)
                    text = text.substring(0, text.length() - 1);
                return;
            }

            if(!nonTypeKeys.contains(keyEvent.getKeyCode()) && !input.isKeyPressed(KeyEvent.VK_CONTROL))
                text += keyEvent.getKeyChar();
        }
    }

    @Override
    public Rectangle render(Graphics g, InputContainer input, boolean isSaveRender, boolean isCensor, int historyPoint) {
        livePosition = new Vector2Int(input.getMouseX(), input.getMouseY()); //Update method only gets called upon keypress

        Point pointToUseForRenderPos = new Point(input.getMouseX(), input.getMouseY());

        if(state == TextState.TYPING)
            pointToUseForRenderPos = new Point(position.toPoint());

        if(isSaveRender && !doSaveNextRender)
            return null;

        Vector2Int renderPos = scEditorWindow.getPointOnImage(pointToUseForRenderPos);
        String textToDraw = "Text";
        if(!text.isEmpty())
            textToDraw = text;

        int drawFontSize = (int) ((double)fontSize * scEditorWindow.getDifferenceFromImage()[1]);

        Font oldFont = g.getFont();
        Color oldColor = g.getColor();
        g.setFont(new Font("Arial", fontMode, drawFontSize));
        g.setColor(color.getColor());
        g.drawString(textToDraw, renderPos.getX(), renderPos.getY());
        g.setFont(oldFont);
        g.setColor(oldColor);

        if(isSaveRender) {
            reset();
        }
        return new Rectangle(renderPos.getX(), renderPos.getY(), g.getFontMetrics().stringWidth(textToDraw), drawFontSize);
    }

    @Override
    public void editorUndo(int historyPoint) {

    }

    @Override
    public void mousePressedEvent(int button, boolean pressed) {
        if(pressed && state == TextState.IDLE) {
            state = TextState.TYPING;
            position = new Vector2Int(livePosition);
        } else if(pressed && state == TextState.TYPING) {
            doSaveNextRender = true;
        }
    }

    @Override
    public void reset() {
        Config config = scEditorWindow.getConfig();
        text = "";
        state = TextState.IDLE;
        doSaveNextRender = false;
        color = new PBRColor(config.getColor("editorStampTextDefaultColor"));
        fontSize = config.getInt("editorStampTextDefaultFontSize");
        fontSizeSpeed = config.getInt("editorStampTextDefaultSpeed");
    }

    @Override
    public int getWidth() {
        return 0;
    }

    @Override
    public int getHeight() {
        return 0;
    }

    @Override
    public String getID() {
        return "editorStampText";
    }

    @Override
    public void setColor(PBRColor color) {
        this.color = color;
    }

    @Override
    public PBRColor getColor() {
        return color;
    }
}
