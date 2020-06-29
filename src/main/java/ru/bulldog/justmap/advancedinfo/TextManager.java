package ru.bulldog.justmap.advancedinfo;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;

import ru.bulldog.justmap.client.config.ClientParams;
import ru.bulldog.justmap.util.ScreenPosition;
import ru.bulldog.justmap.util.DrawHelper.TextAlignment;

public class TextManager {
	public enum TextPosition {
		ABOVE,
		UNDER,
		LEFT,
		RIGHT,
		ABOVE_LEFT,
		ABOVE_RIGHT
	}	

	private TextPosition position = TextPosition.RIGHT;
	private List<InfoText> elements;
	private int x, y;
	private int lineWidth;
	private int spacing = 12;
  
	public TextManager() {
		this.elements = new ArrayList<>();
	}
  
	public void clear() {
		this.elements.clear();
	}
	
	public int getX() {
		return x;
	}
	
	public int getY() {
		return y;
	}
  
	public void draw(MatrixStack matrixStack) {
		this.elements.forEach(line -> {
			if (line.visible) line.draw(matrixStack);
		});
	}
	
	public TextManager updatePosition(ScreenPosition position) {
		int offset = ClientParams.positionOffset;
		MinecraftClient minecraft = MinecraftClient.getInstance();
		int screenW = minecraft.getWindow().getScaledWidth();
		int screenH = minecraft.getWindow().getScaledHeight();
		switch(position) {
			case TOP_LEFT:
				this.updatePosition(offset, offset);
				break;
			case TOP_CENTER:
				this.updatePosition(TextPosition.UNDER,
						screenW / 2 - lineWidth / 2, offset);
				break;
			case TOP_RIGHT:
				this.updatePosition(TextPosition.LEFT,
						screenW - offset, offset);
				break;
			case MIDDLE_LEFT:
				this.updatePosition(offset, screenH / 2 - (this.size() / 2) * spacing);
				break;
			case MIDDLE_RIGHT:
				this.updatePosition(TextPosition.LEFT,
						screenW - offset, screenH / 2 - (this.size() / 2) * spacing);
				break;
			case BOTTOM_LEFT:
				this.updatePosition(TextPosition.ABOVE_RIGHT,
						offset, screenH - offset - spacing);
				break;	
			case BOTTOM_RIGHT:
				this.updatePosition(TextPosition.ABOVE_LEFT,
						screenW - offset, screenH - offset - spacing);
				break;
		}
		
		return this;
	}
	
	public TextManager updatePosition(TextPosition pos, int x, int y) {
		if (position != pos || this.x != x || this.y != y) {
			this.position = pos;
			this.x = x;
			this.y = y;
			this.updateLines();
		}
		return this;
	}
	
	public TextManager updatePosition(int x, int y) {
		return updatePosition(position, x, y);
	}
  
	public TextManager updatePosition(TextPosition pos) {
		return updatePosition(pos, x, y);
	}
	
	private void updateLines() {
		int yp = y;
		int xp = x;
		
		if (position == TextPosition.ABOVE ||
			position == TextPosition.ABOVE_LEFT ||
			position == TextPosition.ABOVE_RIGHT) {
			
			yp -= spacing / 2;
		}
		if (position == TextPosition.LEFT ||
				   position == TextPosition.ABOVE_LEFT) {
			xp -= lineWidth;
		}
		for (InfoText line : elements) {
			if (line.fixed) continue;
			line.offset = ClientParams.positionOffset;
			if (position == TextPosition.ABOVE || position == TextPosition.UNDER) {
				line.alignment = TextAlignment.CENTER;
			} else if (position == TextPosition.LEFT ||
					   position == TextPosition.ABOVE_LEFT) {
				line.alignment = TextAlignment.RIGHT;
			}
			switch (line.alignment) {
		  		case CENTER: line.x = (xp + lineWidth / 2); break;
		  		case RIGHT: line.x = xp + lineWidth; break;
		  		default: line.x = xp;
			}
			if (position == TextPosition.LEFT ||
				position == TextPosition.ABOVE_LEFT) {
				
				line.x -= line.offsetX;
			} else {
				line.x += line.offsetX;
			}
		  
			line.y = yp + line.offsetY;
			if (position == TextPosition.ABOVE ||
				position == TextPosition.ABOVE_LEFT ||
				position == TextPosition.ABOVE_RIGHT) {
				
				yp -= spacing;
			} else {
				yp += spacing;
			}
		}
	}
  
	public void add(InfoText element) {
		this.elements.add(element);
	}
	
	public TextManager setLineWidth(int width) {
		this.lineWidth = width;
		return this;
	}
	
	public TextManager setSpacing(int spacing) {
		this.spacing = spacing;
		return this;
	}

	public void update() {
		this.elements.forEach(element -> element.update());
	}

	public int size() {
		return this.elements.size();
	}
}