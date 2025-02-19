package com.tom.cpl.gui;

import java.util.function.Consumer;

import com.tom.cpl.math.Box;
import com.tom.cpl.math.Vec2i;
import com.tom.cpm.shared.MinecraftClientAccess;

public interface IGui {
	void drawBox(int x, int y, int w, int h, int color);
	void drawGradientBox(int x, int y, int w, int h, int topLeft, int topRight, int bottomLeft, int bottomRight);
	void drawText(int x, int y, String text, int color);
	String i18nFormat(String key, Object... obj);
	int textWidth(String text);
	void drawTexture(int x, int y, int w, int h, int u, int v, String texture);
	void drawTexture(int x, int y, int width, int height, float u1, float v1, float u2, float v2);
	void close();
	void pushMatrix();
	void setPosOffset(Box box);
	void setupCut();
	void popMatrix();
	UIColors getColors();
	void setCloseListener(Consumer<Runnable> listener);
	Vec2i getOffset();
	boolean isShiftDown();
	boolean isCtrlDown();
	boolean isAltDown();
	KeyCodes getKeyCodes();
	NativeGuiComponents getNative();
	void setClipboardText(String text);
	String getClipboardText();
	Frame getFrame();

	default void drawBox(float x, float y, float w, float h, int color) {
		drawBox((int) x, (int) y, (int) w, (int) h, color);
	}

	default void executeLater(Runnable r) {
		MinecraftClientAccess.get().executeLater(() -> {
			try {
				r.run();
			} catch (Throwable e) {
				e.printStackTrace();
				getFrame().logMessage(e.getMessage());
			}
		});
	}
}
