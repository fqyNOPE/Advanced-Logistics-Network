package headless;

import arc.Graphics;
import arc.graphics.*;
import arc.graphics.gl.*;

/**
 * Dummy graphics implementation for headless simulation.
 */
public class MiniGraphics extends Graphics{
    @Override public GL20 getGL20(){ return null; }
    @Override public void setGL20(GL20 gl){}
    @Override public GL30 getGL30(){ return null; }
    @Override public void setGL30(GL30 gl){}
    @Override public int getWidth(){ return 1; }
    @Override public int getHeight(){ return 1; }
    @Override public int getBackBufferWidth(){ return 1; }
    @Override public int getBackBufferHeight(){ return 1; }
    @Override public long getFrameId(){ return 0; }
    @Override public float getDeltaTime(){ return arc.util.Time.delta / 60f; }
    @Override public int getFramesPerSecond(){ return 60; }
    @Override public GLVersion getGLVersion(){ return null; }
    @Override public float getPpiX(){ return 1; }
    @Override public float getPpiY(){ return 1; }
    @Override public float getPpcX(){ return 1; }
    @Override public float getPpcY(){ return 1; }
    @Override public float getDensity(){ return 1; }
    @Override public void setTitle(String title){}
    @Override public void setVSync(boolean vsync){}
    @Override public BufferFormat getBufferFormat(){ return null; }
    @Override public boolean supportsExtension(String extension){ return false; }
    @Override public boolean isContinuousRendering(){ return true; }
    @Override public void setContinuousRendering(boolean isContinuous){}
    @Override public void requestRendering(){}
    @Override public boolean isFullscreen(){ return false; }
    @Override public Cursor newCursor(Pixmap pixmap, int xHotspot, int yHotspot){ return null; }
    @Override public void setCursor(Cursor cursor){}
    @Override protected void setSystemCursor(Cursor.SystemCursor systemCursor){}
}
