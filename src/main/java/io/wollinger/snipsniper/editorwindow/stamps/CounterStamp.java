package io.wollinger.snipsniper.editorwindow.stamps;

import io.wollinger.snipsniper.Config;
import io.wollinger.snipsniper.utils.InputContainer;
import io.wollinger.snipsniper.utils.PBRColor;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.ArrayList;

public class CounterStamp implements IStamp{
    private final Config cfg;

    private int width;
    private int height;
    private int thickness;

    private final int minimumWidth;
    private final int minimumHeight;

    private final int speedWidth;
    private final int speedHeight;
    private final int speed;

    private PBRColor color = new PBRColor(175, 254, 0, 255);

    private final float fontSizeModifier;
    private int count = 1;
    private final boolean solidColor;

    private ArrayList<Integer> historyPoints = new ArrayList<>();

    public CounterStamp(Config cfg) {
        this.cfg = cfg;
        width = cfg.getInt("editorStampCounterWidth");
        height = cfg.getInt("editorStampCounterHeight");

        minimumWidth = cfg.getInt("editorStampCounterWidthMinimum");
        minimumHeight = cfg.getInt("editorStampCounterHeightMinimum");

        speedWidth = cfg.getInt("editorStampCounterWidthSpeed");
        speedHeight = cfg.getInt("editorStampCounterHeightSpeed");
        speed = cfg.getInt("editorStampCounterSpeed");
        fontSizeModifier = cfg.getFloat("editorStampCounterFontSizeModifier");
        solidColor = cfg.getBool("editorStampCounterSolidColor");
    }

    @Override
    public void updateSize(InputContainer input, int mouseWheelDirection) {
        if(mouseWheelDirection != 0) {
            boolean doWidth = true;
            boolean doHeight = true;

            int speedToUse = speed;

            if (input.isKeyPressed(KeyEvent.VK_CONTROL)) {
                doWidth = false;
                speedToUse = speedHeight;
            } else if (input.isKeyPressed(KeyEvent.VK_SHIFT)) {
                doHeight = false;
                speedToUse = speedWidth;
            }

            switch (mouseWheelDirection) {
                case 1:
                    if (doWidth) width -= speedToUse;
                    if (doHeight) height -= speedToUse;
                    break;
                case -1:
                    if (doWidth) width += speedToUse;
                    if (doHeight) height += speedToUse;
                    break;
            }

            if (width <= minimumWidth)
                width = minimumWidth;

            if (height <= minimumHeight)
                height = minimumHeight;
        }
    }

    public void render(Graphics g, InputContainer input, boolean isSaveRender, boolean isCensor, int historyPoint) {
        if(isSaveRender && historyPoint != -1 && !isCensor) {
            historyPoints.add(historyPoint);
        }
        final int x = input.getMouseX() - width / 2;
        final int y = input.getMouseY() - height / 2;

        Color oldFillColor = g.getColor();
        if(solidColor) {
            g.setColor(new Color(oldFillColor.getRed(), oldFillColor.getGreen(), oldFillColor.getBlue(), 255));
        }
        g.fillOval(x, y, width, height);
        g.setColor(oldFillColor);

        Color oldColor = g.getColor();
        g.setColor(Color.BLACK);
        int h = (int)(height/fontSizeModifier);
        g.setFont(new Font("TimesRoman", Font.PLAIN, h));
        int w = g.getFontMetrics().stringWidth(""+count);
        g.drawString("" + count, input.getMouseX()-w/2, input.getMouseY()+h/3);
        g.setColor(oldColor);

        if(cfg.getBool("editorStampCounterBorderEnabled")) {
            oldColor = g.getColor();
            g.setColor(Color.BLACK);
            Graphics2D g2 = (Graphics2D) g;
            Stroke oldStroke = g2.getStroke();
            g2.setStroke(new BasicStroke(height / cfg.getInt("editorStampCounterBorderModifier")));
            g2.drawOval(x, y, width, height);
            g2.setStroke(oldStroke);
            g2.dispose();
            g.setColor(oldColor);
        }

        if(isSaveRender && !isCensor)
            count++;
    }

    @Override
    public void editorUndo(int historyPoint) {
        if(historyPoints.contains(historyPoint)) {
            for(int i = 0; i < historyPoints.size(); i++) {
                if (historyPoints.get(i) == historyPoint) {
                    historyPoints.remove(i);
                    break;
                }
            }
            if (count > 1)
                count--;
        }
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
    public int getMinWidth() {
        return minimumWidth;
    }

    @Override
    public int getMinHeight() {
        return minimumHeight;
    }

    @Override
    public int getSpeedWidth() {
        return speedWidth;
    }

    @Override
    public int getSpeedHeight() {
        return speedHeight;
    }

    @Override
    public int getSpeed() {
        return speed;
    }

    @Override
    public int getThickness() {
        return thickness;
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
