package net.shadowfacts.blockrenderer;

import com.google.common.base.Joiner;
import com.google.common.collect.Sets;
import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiIngameMenu;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.BufferUtils;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

/**
 * @author shadowfacts
 */
@Mod(modid = BlockRenderer.modId, name = BlockRenderer.name, version = BlockRenderer.version, dependencies = "required-after:shadowmc@[3.4.2,);", acceptedMinecraftVersions = "[1.10.2]")
public class BlockRenderer {

	public static final String modId = "BlockRenderer";
	public static final String name = "Block Renderer";
	public static final String version = "@VERSION@";

	@Mod.Instance
	public static BlockRenderer instance;

	private static KeyBinding key;

	private static final DateFormat dateFormat = new SimpleDateFormat("yyy-MM-dd_HH.mm.ss");

	private static Map<String, StackIdentifier> fileStackMap = new HashMap<>();
	private static Map<StackIdentifier, int[]> uvMap = new HashMap<>();

	static String pendingBulkRender;

	private static boolean down = false;

	private int size;
	private float oldZLevel;

	@Mod.EventHandler
	public void preInit(FMLPreInitializationEvent event) {
		key = new KeyBinding("render", Keyboard.KEY_SEMICOLON, modId);
		ClientRegistry.registerKeyBinding(key);
		MinecraftForge.EVENT_BUS.register(this);
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public void onFrameStart(TickEvent.RenderTickEvent event) {
		if (event.phase == TickEvent.Phase.START) {

			if (pendingBulkRender != null) {
				bulkRender(pendingBulkRender, 32);
				pendingBulkRender = null;
			}

			if (Keyboard.isKeyDown(key.getKeyCode())) {
				if (!down) {
					down = true;
					Minecraft.getMinecraft().displayGuiScreen(GUIEnterModID.create(Minecraft.getMinecraft().currentScreen));
				}
			} else {
				down = false;
			}

		}
	}

	private void bulkRender(String mods, int size) {
		Minecraft.getMinecraft().displayGuiScreen(new GuiIngameMenu());
		Set<String> modids = Sets.newHashSet();
		for (String s : mods.split(",")) {
			modids.add(s.trim());
		}
		List<ItemStack> toRender = new ArrayList<>();
		List<ItemStack> li = new ArrayList<>();
		int rendered = 0;
		for (ResourceLocation id : Item.REGISTRY.getKeys()) {
			if (id != null && modids.contains(id.getResourceDomain()) || modids.contains("*")) {
				li.clear();
				Item i = Item.REGISTRY.getObject(id);
				try {
					i.getSubItems(i, i.getCreativeTab(), li);
				} catch (Throwable t) {
					t.printStackTrace();
				}
				toRender.addAll(li);
			}
		}
		File folder = new File("renders/" + dateFormat.format(new Date()) + "/");
		long lastUpdate = 0;
		String joined = Joiner.on(", ").join(modids);
		setRenderState(size);
		for (ItemStack stack : toRender) {
			if (Keyboard.isKeyDown(Keyboard.KEY_ESCAPE)) {
				break;
			}
			render(stack, folder);
			rendered++;
			if (Minecraft.getSystemTime() - lastUpdate > 33) {
				unsetRenderState();
				renderLoading(I18n.format("gui.rendering", toRender.size(), joined),
						I18n.format("gui.progress", rendered, toRender.size(), toRender.size() - rendered),
						stack, (float)rendered/toRender.size());
				lastUpdate = Minecraft.getSystemTime();
				setRenderState(size);
			}
		}
		if (rendered >= toRender.size()) {
			renderLoading(I18n.format("gui.rendered", toRender.size(), Joiner.on(", ").join(modids)), "", null, 1);
		} else {
			renderLoading(I18n.format("gui.renderCancelled"),
					I18n.format("gui.progress", rendered, toRender.size(), toRender.size() - rendered),
					null, (float)rendered/toRender.size());
		}
		unsetRenderState();

		stitch(folder);
		exportMap(folder);

		try {
			Thread.sleep(1500);
		} catch (InterruptedException ignored) {}
	}

	private void stitch(File folder) {

		File[] files = folder.listFiles();
		int total = files.length;
		int size = (int)Math.ceil(Math.sqrt(total));

		BufferedImage result = new BufferedImage(size * 32, size * 32, BufferedImage.TYPE_INT_ARGB);

		Graphics2D graphics  = result.createGraphics();

		try {
			for (int x = 0; x < size; x++) {
				for (int y = 0; y < size; y++) {
					int i = x * size + y;
					if (i < total) {

						File f = files[i];
						BufferedImage img = ImageIO.read(f);
						graphics.drawImage(img, x * 32, y * 32, 32, 32, null);

						uvMap.put(fileStackMap.get(f.getName()), new int[]{x * 32, y * 32});

					}
				}
			}

			File f = new File(folder, "sheet.png");
			ImageIO.write(result, "PNG", f);

			Minecraft.getMinecraft().thePlayer.addChatMessage(new TextComponentTranslation("msg.stitcher.success", f.getPath()));

		} catch (Exception e) {
			e.printStackTrace();
			Minecraft.getMinecraft().thePlayer.addChatMessage(new TextComponentTranslation("msg.stitcher.fail"));
		}
	}

	private void exportMap(File folder) {
		Map<String, Map<String, Map<String, int[]>>> map = new HashMap<>();
		for (StackIdentifier stack : uvMap.keySet()) {
			ResourceLocation id = stack.getID();

			String modid = id.getResourceDomain();
			String name = id.getResourcePath();

			if (!map.containsKey(modid)) {
				map.put(modid, new HashMap<>());
			}
			if (!map.get(modid).containsKey(name)) {
				map.get(modid).put(name, new HashMap<>());
			}

			map.get(modid).get(name).put(Integer.toString(stack.getMeta()), uvMap.get(stack));

		}

		String json = new GsonBuilder().setPrettyPrinting().create().toJson(map);
		String minJson = new Gson().toJson(map);
		try {
			File sheet = new File(folder, "sheet.json");
			File minSheet = new File(folder, "sheet-min.json");
			Files.write(json.getBytes(), sheet);
			Files.write(minJson.getBytes(), minSheet);
			Minecraft.getMinecraft().thePlayer.addChatMessage(new TextComponentTranslation("msg.stitcher.json.success", sheet.getPath()));
		} catch (IOException e) {
			e.printStackTrace();
			Minecraft.getMinecraft().thePlayer.addChatMessage(new TextComponentTranslation("msg.stitcher.json.fail"));
		}
	}

	private void renderLoading(String title, String subtitle, ItemStack is, float progress) {
		Minecraft mc = Minecraft.getMinecraft();
		mc.getFramebuffer().unbindFramebuffer();
		GlStateManager.pushMatrix();
		ScaledResolution res = new ScaledResolution(Minecraft.getMinecraft());
		mc.entityRenderer.setupOverlayRendering();
		// Draw the dirt background and status text...
		RenderUtils.drawBackground(res.getScaledWidth(), res.getScaledHeight());
		RenderUtils.drawCenteredString(mc.fontRendererObj, title, res.getScaledWidth()/2, res.getScaledHeight()/2-24, -1);
		RenderUtils.drawRect(res.getScaledWidth()/2-50, res.getScaledHeight()/2-1, res.getScaledWidth()/2+50, res.getScaledHeight()/2+1, 0xFF001100);
		RenderUtils.drawRect(res.getScaledWidth()/2-50, res.getScaledHeight()/2-1, (res.getScaledWidth()/2-50)+(int)(progress*100), res.getScaledHeight()/2+1, 0xFF55FF55);
		GlStateManager.pushMatrix();
		GlStateManager.scale(0.5f, 0.5f, 1);
		RenderUtils.drawCenteredString(mc.fontRendererObj, subtitle, res.getScaledWidth(), res.getScaledHeight()-20, -1);
		// ...and draw the tooltip.
		if (is != null) {
			try {
				List<String> list = is.getTooltip(mc.thePlayer, true);

				// This code is copied from the tooltip renderer, so we can properly center it.
				for (int i = 0; i < list.size(); ++i) {
					if (i == 0) {
						list.set(i, is.getRarity().rarityColor + list.get(i));
					} else {
						list.set(i, TextFormatting.GRAY + list.get(i));
					}
				}

				FontRenderer font = is.getItem().getFontRenderer(is);
				if (font == null) {
					font = mc.fontRendererObj;
				}
				int width = 0;

				for (String s : list) {
					int j = font.getStringWidth(s);

					if (j > width) {
						width = j;
					}
				}
				// End copied code.
				GlStateManager.translate((res.getScaledWidth()-width/2)-12, res.getScaledHeight()+30, 0);
				RenderUtils.drawHoveringText(list, 0, 0, font);
			} catch (Throwable t) {}
		}
		GlStateManager.popMatrix();
		GlStateManager.popMatrix();
		mc.updateDisplay();
		/*
		 * While OpenGL itself is double-buffered, Minecraft is actually *triple*-buffered.
		 * This is to allow shaders to work, as shaders are only available in "modern" GL.
		 * Minecraft uses "legacy" GL, so it renders using a separate GL context to this
		 * third buffer, which is then flipped to the back buffer with this call.
		 */
		mc.getFramebuffer().bindFramebuffer(false);
	}

	private String render(ItemStack stack, File folder) {
		Minecraft mc = Minecraft.getMinecraft();
		String filename = sanitize(stack.getDisplayName());
		GlStateManager.pushMatrix();
		GlStateManager.clearColor(0, 0, 0, 0);
		GlStateManager.clear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
		mc.getRenderItem().renderItemAndEffectIntoGUI(stack, 0, 0);
		GlStateManager.popMatrix();
		try {

			BufferedImage image = createFlipped(readPixels(size, size));

			File f = new File(folder, filename + ".png");
			int i = 2;
			while (f.exists()) {
				f = new File(folder, filename + "_" + i + ".png");
				i++;
			}
			Files.createParentDirs(f);
			f.createNewFile();
			ImageIO.write(image, "PNG", f);

			fileStackMap.put(f.getName(), new StackIdentifier(stack));

			return I18n.format("msg.render.success", f.getPath());

		} catch (Exception e) {
			e.printStackTrace();
			return I18n.format("msg.render.fail");
		}
	}

	private void setRenderState(int desiredSize) {
		Minecraft mc = Minecraft.getMinecraft();
		ScaledResolution res = new ScaledResolution(mc);

		size = Math.min(Math.min(mc.displayHeight, mc.displayWidth), desiredSize);

		mc.entityRenderer.setupOverlayRendering();
		RenderHelper.enableGUIStandardItemLighting();

		float scale = size / (16f * res.getScaleFactor());
		GlStateManager.translate(0, 0, -(scale * 100));

		GlStateManager.scale(scale, scale, scale);

		oldZLevel = mc.getRenderItem().zLevel;
		mc.getRenderItem().zLevel -= 50;

		GlStateManager.enableRescaleNormal();
		GlStateManager.enableColorMaterial();
		GlStateManager.enableDepth();
		GlStateManager.enableBlend();
		GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_SRC_ALPHA, GL11.GL_ONE);
		GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GlStateManager.disableAlpha();

	}

	private void unsetRenderState() {
		GlStateManager.disableLighting();
		GlStateManager.disableColorMaterial();
		GlStateManager.disableDepth();
		GlStateManager.disableBlend();

		Minecraft.getMinecraft().getRenderItem().zLevel = oldZLevel;
	}

	private String sanitize(String s) {
		return s.replaceAll("[^A-Za-z0-9-_]", "_");
	}

	public BufferedImage readPixels(int width, int height) throws InterruptedException {
		GL11.glReadBuffer(GL11.GL_BACK);
		ByteBuffer buf = BufferUtils.createByteBuffer(width * height * 4);
		GL11.glReadPixels(0, Minecraft.getMinecraft().displayHeight - height, width, height, GL12.GL_BGRA, GL11.GL_UNSIGNED_BYTE, buf);
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		int[] pixels = new int[width * height];
		buf.asIntBuffer().get(pixels);
		image.setRGB(0, 0, width, height, pixels, 0, width);
		return image;
	}

	private static BufferedImage createFlipped(BufferedImage image) {
		AffineTransform at = new AffineTransform();
		at.concatenate(AffineTransform.getScaleInstance(1, -1));
		at.concatenate(AffineTransform.getTranslateInstance(0, -image.getHeight()));
		return createTransformed(image, at);
	}

	private static BufferedImage createTransformed(BufferedImage image, AffineTransform at) {
		BufferedImage newImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = newImage.createGraphics();
		g.transform(at);
		g.drawImage(image, 0, 0, null);
		g.dispose();
		return newImage;
	}

}
