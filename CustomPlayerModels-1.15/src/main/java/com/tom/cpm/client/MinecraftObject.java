package com.tom.cpm.client;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import net.minecraft.client.GameSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.NativeImage;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.entity.player.PlayerModelPart;
import net.minecraft.resources.IResource;
import net.minecraft.util.ResourceLocation;

import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

import com.tom.cpl.gui.IKeybind;
import com.tom.cpl.util.DynamicTexture.ITexture;
import com.tom.cpl.util.Image;
import com.tom.cpm.shared.MinecraftClientAccess;
import com.tom.cpm.shared.definition.ModelDefinitionLoader;
import com.tom.cpm.shared.model.SkinType;

public class MinecraftObject implements MinecraftClientAccess {
	/** The default skin for the Steve model. */
	private static final ResourceLocation TEXTURE_STEVE = new ResourceLocation("textures/entity/steve.png");
	/** The default skin for the Alex model. */
	private static final ResourceLocation TEXTURE_ALEX = new ResourceLocation("textures/entity/alex.png");

	private final Minecraft mc;
	private final ModelDefinitionLoader loader;
	private final PlayerRenderManager prm;
	public MinecraftObject(Minecraft mc, ModelDefinitionLoader loader) {
		this.mc = mc;
		prm = new PlayerRenderManager(loader);
		this.loader = loader;
	}

	@Override
	public Image getVanillaSkin(SkinType skinType) {
		ResourceLocation loc;
		switch (skinType) {
		case SLIM:
			loc = TEXTURE_ALEX;
			break;

		case DEFAULT:
		case UNKNOWN:
		default:
			loc = TEXTURE_STEVE;
			break;
		}
		try(IResource r = mc.getResourceManager().getResource(loc)) {
			return Image.loadFrom(r.getInputStream());
		} catch (IOException e) {
		}
		return null;
	}

	@Override
	public PlayerRenderManager getPlayerRenderManager() {
		return prm;
	}

	@Override
	public ITexture createTexture() {
		return new DynTexture(mc);
	}

	public static class DynTexture extends DynamicTexture implements ITexture {
		private final ResourceLocation loc;
		private final Minecraft mc;
		private static ResourceLocation bound_loc;

		public DynTexture(Minecraft mc) {
			super(1, 1, true);
			this.mc = mc;
			loc = mc.getTextureManager().getDynamicTextureLocation("cpm", this);
		}

		@Override
		public void bind() {
			bound_loc = loc;
			if(mc.getTextureManager().getTexture(loc) == null)
				mc.getTextureManager().loadTexture(loc, this);
		}

		@Override
		public void load(Image texture) {
			NativeImage ni = createFromBufferedImage(texture);
			try {
				setTextureData(ni);
			} catch (Exception e) {
			}
			TextureUtil.prepareImage(this.getGlTextureId(), ni.getWidth(), ni.getHeight());
			updateDynamicTexture();
		}

		public static ResourceLocation getBoundLoc() {
			return bound_loc;
		}

		@Override
		public void free() {
			mc.getTextureManager().deleteTexture(loc);
		}
	}

	public static NativeImage createFromBufferedImage(Image texture) {
		NativeImage ni = new NativeImage(texture.getWidth(), texture.getHeight(), false);
		for(int y = 0;y<texture.getHeight();y++) {
			for(int x = 0;x<texture.getWidth();x++) {
				int rgb = texture.getRGB(x, y);
				int a = (rgb >> 24 & 255);
				int r = (rgb >> 16 & 255);
				int g = (rgb >> 8 & 255);
				int b = (rgb & 255);
				ni.setPixelRGBA(x, y, (a << 24) | (b << 16) | (g << 8) | r);
			}
		}
		return ni;
	}

	@Override
	public void executeLater(Runnable r) {
		mc.enqueue(r);
	}

	@Override
	public ModelDefinitionLoader getDefinitionLoader() {
		return loader;
	}

	@Override
	public SkinType getSkinType() {
		return SkinType.get(DefaultPlayerSkin.getSkinType(mc.getSession().getProfile().getId()));
	}

	@Override
	public void setEncodedGesture(int value) {
		Set<PlayerModelPart> s = ObfuscationReflectionHelper.getPrivateValue(GameSettings.class, mc.gameSettings, "field_178882_aU");
		setEncPart(s, value, 0, PlayerModelPart.HAT);
		setEncPart(s, value, 1, PlayerModelPart.JACKET);
		setEncPart(s, value, 2, PlayerModelPart.LEFT_PANTS_LEG);
		setEncPart(s, value, 3, PlayerModelPart.RIGHT_PANTS_LEG);
		setEncPart(s, value, 4, PlayerModelPart.LEFT_SLEEVE);
		setEncPart(s, value, 5, PlayerModelPart.RIGHT_SLEEVE);
		mc.gameSettings.sendSettingsToServer();
	}

	private static void setEncPart(Set<PlayerModelPart> s, int value, int off, PlayerModelPart part) {
		if((value & (1 << off)) != 0)s.add(part);
		else s.remove(part);
	}

	@Override
	public boolean isInGame() {
		return mc.player != null;
	}

	@Override
	public Object getPlayerIDObject() {
		return mc.getSession().getProfile();
	}

	@Override
	public List<IKeybind> getKeybinds() {
		return KeyBindings.kbs;
	}

	@Override
	public ServerStatus getServerSideStatus() {
		return mc.player != null ? ServerStatus.SKIN_LAYERS_ONLY : ServerStatus.OFFLINE;
	}
}
