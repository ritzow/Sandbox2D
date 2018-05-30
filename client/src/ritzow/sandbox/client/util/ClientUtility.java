package ritzow.sandbox.client.util;

import ritzow.sandbox.client.graphics.Camera;

public class ClientUtility {
	public static float pixelHorizontalToWorld(Camera camera, int mouseX, int frameWidth, int frameHeight) {
		float worldX = (2f * mouseX) / frameWidth - 1f; //normalize the mouse coordinate
		worldX /= frameHeight/(float)frameWidth; 		//apply aspect ratio
		worldX /= camera.getZoom(); 					//apply zoom
		worldX += camera.getPositionX(); 				//apply camera position
		return worldX;
	}
	
	public static float pixelVerticalToWorld(Camera camera, int mouseY, int frameWidth, int frameHeight) {
		float worldY = -((2f * mouseY) / frameHeight - 1f);
		worldY /= camera.getZoom();
		worldY += camera.getPositionY();
		return worldY;
	}
	
	public static float getViewRightBound(Camera camera, int framebufferWidth, int framebufferHeight) {
		//far right of screen, after accounting for aspect ratio, in world coordinates
		return framebufferWidth/(camera.getZoom() * framebufferHeight) + camera.getPositionX();
	}
	
	public static float getViewLeftBound(Camera camera, int framebufferWidth, int framebufferHeight) {
		//far left of screen after accounting for aspect ratio, in world coordinates
		return -getViewRightBound(camera, framebufferWidth, framebufferHeight);
	}
	
	//width and height for any future need
	public static float getViewTopBound(Camera camera, int framebufferWidth, int framebufferHeight) {
		return 1/camera.getZoom() + camera.getPositionY();
	}
	
	//width and height for any future need
	public static float getViewBottomBound(Camera camera, int framebufferWidth, int framebufferHeight) {
		return -getViewTopBound(camera, framebufferWidth, framebufferHeight);
	}
}
