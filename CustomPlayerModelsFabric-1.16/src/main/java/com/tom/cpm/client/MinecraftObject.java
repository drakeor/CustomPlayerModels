package com.tom.cpm.client;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.PlayerModelPart;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.texture.TextureUtil;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.resource.Resource;
import net.minecraft.util.Identifier;

import com.tom.cpl.gui.IKeybind;
import com.tom.cpl.util.DynamicTexture.ITexture;
import com.tom.cpl.util.Image;
import com.tom.cpm.shared.MinecraftClientAccess;
import com.tom.cpm.shared.definition.ModelDefinitionLoader;
import com.tom.cpm.shared.model.SkinType;

public class MinecraftObject implements MinecraftClientAccess {
	private static final Identifier STEVE_SKIN = new Identifier("textures/entity/steve.png");
	private static final Identifier ALEX_SKIN = new Identifier("textures/entity/alex.png");

	private final MinecraftClient mc;
	private final PlayerRenderManager prm;
	private final ModelDefinitionLoader loader;
	public MinecraftObject(MinecraftClient mc, ModelDefinitionLoader loader) {
		this.mc = mc;
		prm = new PlayerRenderManager(loader);
		this.loader = loader;
	}

	@Override
	public Image getVanillaSkin(SkinType skinType) {
		Identifier loc;
		switch (skinType) {
		case SLIM:
			loc = ALEX_SKIN;
			break;

		case DEFAULT:
		case UNKNOWN:
		default:
			loc = STEVE_SKIN;
			break;
		}
		try(Resource r = mc.getResourceManager().getResource(loc)) {
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

	public static class DynTexture extends NativeImageBackedTexture implements ITexture {
		private final Identifier loc;
		private final MinecraftClient mc;
		private static Identifier bound_loc;

		public DynTexture(MinecraftClient mc) {
			super(1, 1, true);
			this.mc = mc;
			loc = mc.getTextureManager().registerDynamicTexture("cpm", this);
		}

		@Override
		public void bind() {
			bound_loc = loc;
			if(mc.getTextureManager().getTexture(loc) == null)
				mc.getTextureManager().registerTexture(loc, this);
		}

		@Override
		public void load(Image texture) {
			NativeImage ni = createFromBufferedImage(texture);
			setImage(ni);
			TextureUtil.allocate(this.getGlId(), ni.getWidth(), ni.getHeight());
			upload();
		}

		public static Identifier getBoundLoc() {
			return bound_loc;
		}

		@Override
		public void free() {
			mc.getTextureManager().destroyTexture(loc);
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
				ni.setPixelColor(x, y, (a << 24) | (b << 16) | (g << 8) | r);
			}
		}
		return ni;
	}

	@Override
	public void executeLater(Runnable r) {
		mc.execute(r);
	}

	@Override
	public ModelDefinitionLoader getDefinitionLoader() {
		return loader;
	}

	@Override
	public SkinType getSkinType() {
		return SkinType.get(DefaultSkinHelper.getModel(mc.getSession().getProfile().getId()));
	}

	@Override
	public void setEncodedGesture(int value) {
		Set<PlayerModelPart> s = mc.options.enabledPlayerModelParts;
		setEncPart(s, value, 0, PlayerModelPart.HAT);
		setEncPart(s, value, 1, PlayerModelPart.JACKET);
		setEncPart(s, value, 2, PlayerModelPart.LEFT_PANTS_LEG);
		setEncPart(s, value, 3, PlayerModelPart.RIGHT_PANTS_LEG);
		setEncPart(s, value, 4, PlayerModelPart.LEFT_SLEEVE);
		setEncPart(s, value, 5, PlayerModelPart.RIGHT_SLEEVE);
		mc.options.onPlayerModelPartChange();
	}

	private static void setEncPart(Set<PlayerModelPart> s, int value, int off, PlayerModelPart part) {
		if((value & (1 << off)) != 0)s.add(part);
		else s.remove(part);
	}

	@Override
	public Object getPlayerIDObject() {
		return mc.getSession().getProfile();
	}

	@Override
	public boolean isInGame() {
		return mc.player != null;
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
