package graphics.ui.element;

import main.GameState;
import util.ResourceManager;

public class PlayerIconElement extends InterfaceElement {
	
	public PlayerIconElement() {
		
		ModelElement background = new ModelElement(ResourceManager.getModel("red_square"));
		background.graphics().scale().setX(0.25f);
		background.graphics().scale().setY(0.25f);
		background.position().setX(-0.025f);
		background.position().setY(-0.025f);
		
		children.add(background);
		
		ModelElement playerIcon = new ModelElement(GameState.getPlayer().graphics().getModel());
		playerIcon.graphics().scale().setX(0.25f);
		playerIcon.graphics().scale().setY(0.25f);
		playerIcon.position().setX(0.025f);
		playerIcon.position().setY(0.025f);
		
		children.add(playerIcon);
	}

	@Override
	public boolean isWorldElement() {
		return false;
	}
	
}
