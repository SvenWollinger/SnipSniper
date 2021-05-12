package io.wollinger.snipsniper.scviewer;

import io.wollinger.snipsniper.snipscope.SnipScopeListener;

import java.awt.event.KeyEvent;

public class SCViewerListener extends SnipScopeListener {
    private final SCViewerWindow scViewerWindow;

    public SCViewerListener(SCViewerWindow snipScopeWindow) {
        super(snipScopeWindow);
        scViewerWindow = snipScopeWindow;
    }

    @Override
    public void keyPressed(KeyEvent keyEvent) {
        super.keyPressed(keyEvent);
        if(keyEvent.getKeyCode() == KeyEvent.VK_LEFT) {
            if(!scViewerWindow.isLocked())
                scViewerWindow.slideImage(-1);
        } else if(keyEvent.getKeyCode() == KeyEvent.VK_RIGHT) {
            if(!scViewerWindow.isLocked())
                scViewerWindow.slideImage(1);
        } else if(keyEvent.getKeyCode() == KeyEvent.VK_ENTER) {
            scViewerWindow.openEditor();
        } else if(keyEvent.getKeyCode() == KeyEvent.VK_F5) {
            scViewerWindow.refreshFolder();
        }
    }
}