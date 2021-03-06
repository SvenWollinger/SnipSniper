package io.wollinger.snipsniper.sceditor.stamps;

import io.wollinger.snipsniper.Config;
import io.wollinger.snipsniper.utils.ConfigHelper;
import io.wollinger.snipsniper.utils.InputContainer;
import io.wollinger.snipsniper.utils.PBRColor;
import io.wollinger.snipsniper.utils.Vector2Int;

import java.awt.*;
import java.awt.event.KeyEvent;

public class RectangleStamp implements IStamp {
    private final Config config;

    private int width;
    private int height;

    private int minimumWidth;
    private int minimumHeight;

    private int speedWidth;
    private int speedHeight;

    private int thickness;

    private PBRColor color;

    public RectangleStamp(Config config) {
        this.config = config;
        reset();
    }

    @Override
    public void update(InputContainer input, int mouseWheelDirection, KeyEvent keyEvent) {
        if(input.isKeyPressed(KeyEvent.VK_B)) {
            switch (mouseWheelDirection) {
                case 1:
                    thickness--;
                    break;
                case -1:
                    thickness++;
                    break;
            }
            if(thickness <= 0)
                thickness = 1;
            return;
        }

        String dir = "Width";
        if (input.isKeyPressed(KeyEvent.VK_SHIFT))
            dir = "Height";

        int idSpeed = speedWidth;
        int idMinimum = minimumWidth;
        if (dir.equals("Height")) {
            idSpeed = speedHeight;
            idMinimum = minimumHeight;
        }

        switch (mouseWheelDirection) {
            case 1:
                if (dir.equals("Height")) {
                    height -= idSpeed;
                    if (height <= idMinimum)
                        height = idMinimum;
                } else {
                    width -= idSpeed;
                    if (width <= idMinimum)
                        width = idMinimum;
                }
                break;
            case -1:
                if (dir.equals("Height")) {
                    height += idSpeed;
                } else {
                    width += idSpeed;
                }
                break;
        }
    }

    @Override
    public Rectangle render(Graphics g, InputContainer input, Vector2Int position, Double[] difference, boolean isSaveRender, boolean isCensor, int historyPoint) {
        Graphics2D g2 = (Graphics2D)g;
        Stroke oldStroke = g2.getStroke();
        int strokeThickness = (int)(thickness*difference[0]);
        if(strokeThickness <= 0)
            strokeThickness = 1;
        g2.setStroke(new BasicStroke(strokeThickness));
        Color oldColor = g2.getColor();
        g2.setColor(color.getColor());
        Rectangle rectangle = new Rectangle((int)(position.getX() - ((double)width*difference[0]) / 2), (int)(position.getY() - ((double)height*difference[1]) / 2), (int)((double)width*difference[0]), (int)((double)height*difference[1]));
        g2.drawRect(rectangle.x, rectangle.y, rectangle.width, rectangle.height);
        g2.setColor(oldColor);
        g2.setStroke(oldStroke);
        g2.dispose();
        return rectangle;
    }

    @Override
    public void editorUndo(int historyPoint) { }

    @Override
    public void mousePressedEvent(int button, boolean pressed) {

    }

    @Override
    public void reset() {
        color = new PBRColor(config.getColor(ConfigHelper.PROFILE.editorStampRectangleDefaultColor));

        width = config.getInt(ConfigHelper.PROFILE.editorStampRectangleWidth);
        height = config.getInt(ConfigHelper.PROFILE.editorStampRectangleHeight);

        minimumWidth = config.getInt(ConfigHelper.PROFILE.editorStampRectangleWidthMinimum);
        minimumHeight = config.getInt(ConfigHelper.PROFILE.editorStampRectangleHeightMinimum);

        speedWidth = config.getInt(ConfigHelper.PROFILE.editorStampRectangleWidthSpeed);
        speedHeight = config.getInt(ConfigHelper.PROFILE.editorStampRectangleHeightSpeed);

        thickness = config.getInt(ConfigHelper.PROFILE.editorStampRectangleThickness);
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public String getID() {
        return "editorStampRectangle";
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
