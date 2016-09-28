package util;

import main.Exitable;
import main.Installable;

public class Synchronizer {
	public static void waitForSetup(Installable installable) {
		synchronized(installable) {
			while(!installable.isSetupComplete()) {
				try {
					installable.wait();
				} catch (InterruptedException e){
					e.printStackTrace();
				}
			}
		}
	}
	
	public static void waitForExit(Exitable exitable) {
		exitable.exit();
		synchronized(exitable) {
			while(!exitable.isFinished()) {
				try {
					exitable.wait();
				} catch (InterruptedException e){
					e.printStackTrace();
				}
			}
		}
	}
}
