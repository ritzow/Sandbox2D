package ritzow.sandbox.util;

/** Instances of Updatable can be added to a ClientUpdateManager to be updated repeatedly at a rate set by the ClientUpdateManager **/
public interface Updatable {
	public void update();
}