package ritzow.sandbox.client.util;

import ritzow.sandbox.client.graphics.Camera;

public class ClientUtility {
	public static float mouseHorizontalToWorld(Camera camera, float mouseX, int frameWidth, int frameHeight) {
		float worldX = (2f * mouseX) / frameWidth - 1f; //normalize the mouse coordinate
		worldX /= frameHeight/(float)frameWidth; 		//apply aspect ratio
		worldX /= camera.getZoom(); 					//apply zoom
		worldX += camera.getPositionX(); 				//apply camera position
		return worldX;
	}
	
	public static float mouseVerticalToWorld(Camera camera, float mouseY, int frameHeight) {
		float worldY = -((2f * mouseY) / frameHeight - 1f);
		worldY /= camera.getZoom();
		worldY += camera.getPositionY();
		return worldY;
	}
}
