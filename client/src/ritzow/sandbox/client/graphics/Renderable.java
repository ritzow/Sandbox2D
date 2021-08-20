package ritzow.sandbox.client.graphics;

public interface Renderable {
	//TODO make a special EntityRenderer interface with rendering commads that are controlled by the world renderer
	void render(ModelRenderer program, float exposure);
	float getWidth();
	float getHeight();
}
