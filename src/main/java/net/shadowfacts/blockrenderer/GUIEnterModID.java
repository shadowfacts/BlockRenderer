package net.shadowfacts.blockrenderer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.shadowfacts.shadowmc.ui.UIKeyInteractable;
import net.shadowfacts.shadowmc.ui.element.button.UIButtonText;
import net.shadowfacts.shadowmc.ui.element.textfield.UITextField;
import net.shadowfacts.shadowmc.ui.element.view.UIStackView;
import net.shadowfacts.shadowmc.ui.style.UIAttribute;
import net.shadowfacts.shadowmc.ui.style.UIVerticalLayoutMode;
import net.shadowfacts.shadowmc.ui.util.UIBuilder;
import org.lwjgl.input.Keyboard;

/**
 * @author shadowfacts
 */
public class GUIEnterModID {

	public static GuiScreen create(GuiScreen parent) {

		UIStackView stack = new UIStackView("stack");
		stack.setStyle(UIAttribute.VERTICAL_LAYOUT, UIVerticalLayoutMode.TOP);
		stack.setStyle(UIAttribute.STACK_SPACING, 10);
		stack.setStyle(UIAttribute.MARGIN_TOP, 10);

		UITextField textField = new UITextField(s -> {}, "textField");
		stack.add(textField);

		UIButtonText render = new UIButtonText("Render", (btn, mouseBtn) -> {
			if (Minecraft.getMinecraft().theWorld != null) {
				BlockRenderer.pendingBulkRender = textField.getText();
			}
			Minecraft.getMinecraft().displayGuiScreen(parent);
			return true;
		}, "render");
		stack.add(render);

		UIButtonText cancel = new UIButtonText("Cancel", (btn, mouseBtn) -> {
			Minecraft.getMinecraft().displayGuiScreen(parent);
			return true;
		}, "cancel");
		stack.add(cancel);


		UIKeyInteractable handler = (keyCode, keyChar) -> {
			if (keyCode == Keyboard.KEY_ESCAPE) Minecraft.getMinecraft().displayGuiScreen(parent);
		};

		return new UIBuilder().add(stack).clearKeyHandlers().addKeyHandler(handler).createScreen();
	}

}
