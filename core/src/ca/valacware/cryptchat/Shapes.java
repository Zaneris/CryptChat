package ca.valacware.cryptchat;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

class Shapes extends ShapeRenderer {
	/**
	 * Draws a rectangle with rounded corners of the given radius.
	 */
	void roundedRect(float x, float y, float width, float height, float radius){
		// Central rectangle
		super.rect(x, y + radius, width, height - 2*radius);

		// Top/Bottom rectangles
		super.rect(x + radius, y, width - 2*radius, radius);
		super.rect(x + radius, y + height - radius, width - 2*radius, radius);


		// Four arches, clockwise
		super.arc(x + radius, y + radius, radius, 180f, 90f);
		super.arc(x + width - radius, y + radius, radius, 270f, 90f);
		super.arc(x + width - radius, y + height - radius, radius, 0f, 90f);
		super.arc(x + radius, y + height - radius, radius, 90f, 90f);
	}
}