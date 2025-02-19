package com.tom.cpm.shared.editor.gui;

import com.tom.cpl.gui.IGui;
import com.tom.cpl.math.Vec2i;
import com.tom.cpm.shared.MinecraftObjectHolder;
import com.tom.cpm.shared.editor.Editor;
import com.tom.cpm.shared.editor.tree.TreeElement;
import com.tom.cpm.shared.util.PaintImageCreator;

public class ViewportPaintPanel extends ViewportPanel {
	private int color;
	private int dragging;

	public ViewportPaintPanel(IGui gui, Editor editor) {
		super(gui, editor);
	}

	@Override
	public void draw(int mouseX, int mouseY, float partialTicks) {
		mouseCursorPos.x = mouseX;
		mouseCursorPos.y = mouseY;

		gui.pushMatrix();
		gui.setPosOffset(bounds);
		gui.setupCut();
		gui.drawBox(0, 0, bounds.w, bounds.h, 0xff333333);

		TreeElement e = editor.selectedElement;
		editor.selectedElement = null;
		editor.renderPaint = true;
		nat.render(partialTicks, mouseX, mouseY);

		if(bounds.isInBounds(mouseX - bounds.x, mouseY - bounds.y)) {
			color = nat.colorUnderMouse;
		} else
			color = 0;

		editor.renderPaint = false;
		editor.selectedElement = e;

		if(!MinecraftObjectHolder.DEBUGGING || !gui.isAltDown()) {
			gui.drawBox(0, 0, bounds.w, bounds.h, 0xff333333);
			nat.render(partialTicks, mouseX, mouseY);
		}

		if(MinecraftObjectHolder.DEBUGGING) {
			Vec2i v = getHoveredTexPos();
			if(v != null)gui.drawText(0, 0, v.x + " " + v.y, 0xffffffff);
			gui.drawBox(bounds.x, bounds.y + 10, 16, 16, 0xffffffff);
			gui.drawBox(bounds.x + 1, bounds.y + 11, 14, 14, color | 0xff000000);
		}

		gui.popMatrix();
		gui.setupCut();
	}

	public Vec2i getHoveredTexPos() {
		return PaintImageCreator.getImageCoords(color, editor.renderTexture.size.x, editor.renderTexture.size.y);
	}

	@Override
	public boolean mouseClick(int x, int y, int btn) {
		if(bounds.isInBounds(x, y) && btn == 0) {
			dragging = 1;
			Vec2i v = getHoveredTexPos();
			if(v != null) {
				if(gui.isCtrlDown()) {
					editor.penColor = editor.skinProvider.getImage().getRGB(v.x, v.y);
					editor.setPenColor.accept(editor.penColor);
					dragging = 2;
				} else
					editor.drawPixel(v.x, v.y);
			}
			return true;
		}
		return super.mouseClick(x, y, btn);
	}

	@Override
	public boolean mouseDrag(int x, int y, int btn) {
		if(bounds.isInBounds(x, y) && btn == 0) {
			Vec2i v = getHoveredTexPos();
			if(v != null) {
				if(dragging == 1) {
					editor.drawPixel(v.x, v.y);
				} else if(dragging == 2) {
					editor.penColor = editor.skinProvider.getImage().getRGB(v.x, v.y);
					editor.setPenColor.accept(editor.penColor);
				}
			}
			return true;
		}
		return super.mouseDrag(x, y, btn);
	}

	@Override
	public boolean mouseRelease(int x, int y, int btn) {
		if(btn == 0 && dragging > 0) {
			dragging = 0;
			return true;
		}
		return super.mouseRelease(x, y, btn);
	}
}
