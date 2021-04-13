package io.wollinger.snipsniper.editorwindow.stamps;

import io.wollinger.snipsniper.editorwindow.EditorWindow;
import io.wollinger.snipsniper.utils.InputContainer;
import io.wollinger.snipsniper.utils.PBRColor;

import java.awt.*;

public class SimpleBrush implements IStamp {
    private final EditorWindow editorWindow;
    private PBRColor color = new PBRColor(new Color(0,166,176,255));
    private int size;
    private final int speed;

    public SimpleBrush(EditorWindow editorWindow) {
        this.editorWindow = editorWindow;
        this.size = editorWindow.getSniperInstance().cfg.getInt("editorStampSimpleBrushSize");
        this.speed = editorWindow.getSniperInstance().cfg.getInt("editorStampSimpleBrushSizeSpeed");
    }

    @Override
    public void updateSize(InputContainer input, int mouseWheelDirection) {
        if(mouseWheelDirection != 0) {
            switch (mouseWheelDirection) {
                case 1:
                    if(size > 1) size -= speed;
                    break;
                case -1:
                    size += speed;
                    break;
            }
        }
    }

    @Override
    public void render(Graphics g, InputContainer input, boolean isSaveRender, boolean isCensor, int historyPoint) {
        Color oldColor = g.getColor();
        g.setColor(new Color(color.c.getRed(), color.c.getGreen(), color.c.getBlue(), 255));
        g.fillOval(input.getMouseX()-size/2, input.getMouseY()-size/2, size, size);
        g.setColor(oldColor);

        Point p0 = input.getMousePathPoint(0);
        Point p1 = input.getMousePathPoint(1);
        if(p0 != null && p1 != null) {
            Graphics2D g2 = (Graphics2D)editorWindow.getImage().getGraphics();
            g2.setRenderingHints(editorWindow.getQualityHints());
            Stroke oldStroke = g2.getStroke();
            oldColor = g2.getColor();
            g2.setColor(new Color(color.c.getRed(), color.c.getGreen(), color.c.getBlue(), 255));
            g2.setStroke(new BasicStroke(size, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            double distance = Math.hypot(p0.getX() - p1.getX(), p0.getY() - p1.getY());
            if(distance > editorWindow.getSniperInstance().cfg.getInt("editorStampSimpleBrushDistance")) {
                g2.drawLine((int) p0.getX(), (int) p0.getY(), (int) p1.getX(), (int) p1.getY());
                input.removeMousePathPoint(0);
            } else {
                input.removeMousePathPoint(1);
            }

            g2.setStroke(oldStroke);
            g.setColor(oldColor);
            g2.dispose();
        }
    }

    @Override
    public void editorUndo(int historyPoint) {

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
    public int getMinWidth() {
        return 0;
    }

    @Override
    public int getMinHeight() {
        return 0;
    }

    @Override
    public int getSpeedWidth() {
        return 0;
    }

    @Override
    public int getSpeedHeight() {
        return 0;
    }

    @Override
    public int getSpeed() {
        return 0;
    }

    @Override
    public int getThickness() {
        return 0;
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