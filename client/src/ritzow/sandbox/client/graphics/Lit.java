package ritzow.sandbox.client.graphics;

import ritzow.sandbox.world.component.Positional;

public interface Lit extends Positional {
	Iterable<Light> lights();
}
