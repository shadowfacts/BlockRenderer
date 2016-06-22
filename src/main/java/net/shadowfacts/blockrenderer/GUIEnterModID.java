package net.shadowfacts.blockrenderer;

import net.minecraft.client.gui.GuiScreen;
import net.shadowfacts.shadowmc.gui.component.GUIComponentTextField;
import net.shadowfacts.shadowmc.gui.component.button.GUIButtonText;
import net.shadowfacts.shadowmc.gui.handler.ExitWindowKeyHandler;
import net.shadowfacts.shadowmc.gui.mcwrapper.GuiScreenWrapper;
import net.shadowfacts.shadowmc.gui.mcwrapper.MCBaseGUI;
import net.shadowfacts.shadowmc.util.MouseButton;
import org.lwjgl.input.Keyboard;

/**
 * @author shadowfacts
 */
public class GUIEnterModID extends MCBaseGUI {

	private GuiScreen parent;

	private GUIComponentTextField field;

	public GUIEnterModID(GuiScreenWrapper wrapper, GuiScreen parent) {
		super(wrapper);
		this.parent = parent;

		field = addChild(new GUIComponentTextField(0, 0, 200, 20, s -> {}));
		addChild(new GUIButtonText(0, 30, 100, 20, this::onRender, "Render"));
		addChild(new GUIButtonText(0, 60, 100, 20, this::onCancel, "Cancel"));

		keyHandlers.clear();
		keyHandlers.add(new ExitWindowKeyHandler(Keyboard.KEY_ESCAPE));
	}

	private boolean onRender(GUIButtonText button, MouseButton mouseButton) {
		if (mc.theWorld != null) {
			BlockRenderer.pendingBulkRender = field.getText();
		}
		mc.displayGuiScreen(parent);
		return true;
	}

	private boolean onCancel(GUIButtonText button, MouseButton mouseButton) {
		mc.displayGuiScreen(parent);
		return true;
	}

	public static GuiScreen create(GuiScreen parent) {
		GuiScreenWrapper wrapper = new GuiScreenWrapper();
		GUIEnterModID gui = new GUIEnterModID(wrapper, parent);
		wrapper.gui = gui;
		gui.setZLevel(0);
		return wrapper;
	}

}
