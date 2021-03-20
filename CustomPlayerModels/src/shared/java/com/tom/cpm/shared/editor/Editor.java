package com.tom.cpm.shared.editor;

import static com.tom.cpm.shared.MinecraftObjectHolder.gson;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.tom.cpl.gui.IGui;
import com.tom.cpl.gui.UIColors;
import com.tom.cpl.gui.UpdaterRegistry;
import com.tom.cpl.gui.UpdaterRegistry.Updater;
import com.tom.cpl.gui.elements.MessagePopup;
import com.tom.cpl.math.Box;
import com.tom.cpl.math.Vec2i;
import com.tom.cpl.math.Vec3f;
import com.tom.cpl.math.Vec3i;
import com.tom.cpl.util.Image;
import com.tom.cpl.util.Pair;
import com.tom.cpm.shared.MinecraftClientAccess;
import com.tom.cpm.shared.animation.CustomPose;
import com.tom.cpm.shared.animation.IPose;
import com.tom.cpm.shared.animation.VanillaPose;
import com.tom.cpm.shared.config.Player;
import com.tom.cpm.shared.definition.ModelDefinition;
import com.tom.cpm.shared.editor.anim.AnimFrame;
import com.tom.cpm.shared.editor.anim.AnimationEncodingData;
import com.tom.cpm.shared.editor.anim.AnimationType;
import com.tom.cpm.shared.editor.anim.EditorAnim;
import com.tom.cpm.shared.editor.anim.IElem;
import com.tom.cpm.shared.editor.gui.EditorGui;
import com.tom.cpm.shared.editor.gui.PosPanel.ModeDisplType;
import com.tom.cpm.shared.editor.template.EditorTemplate;
import com.tom.cpm.shared.editor.template.TemplateArgHandler;
import com.tom.cpm.shared.editor.template.TemplateArgType;
import com.tom.cpm.shared.editor.template.TemplateSettings;
import com.tom.cpm.shared.editor.tree.TreeElement;
import com.tom.cpm.shared.editor.tree.TreeElement.VecType;
import com.tom.cpm.shared.editor.util.PlayerSkinLayer;
import com.tom.cpm.shared.editor.util.StoreIDGen;
import com.tom.cpm.shared.editor.util.ValueOp;
import com.tom.cpm.shared.io.ProjectFile;
import com.tom.cpm.shared.model.PlayerModelParts;
import com.tom.cpm.shared.model.PlayerPartValues;
import com.tom.cpm.shared.model.SkinType;
import com.tom.cpm.shared.util.TextureStitcher;

public class Editor {
	public static final int projectFileVersion = 1;
	public UpdaterRegistry updaterReg = new UpdaterRegistry();
	public Updater<Vec3f> setOffset = updaterReg.create();
	public Updater<Vec3f> setRot = updaterReg.create();
	public Updater<Vec3f> setPosition = updaterReg.create();
	public Updater<Vec3f> setSize = updaterReg.create();
	public Updater<Vec3f> setScale = updaterReg.create();
	public Updater<Float> setMCScale = updaterReg.create();
	public Updater<Boolean> setMirror = updaterReg.create();
	public Updater<String> updateName = updaterReg.create();
	public Updater<String> setModeBtn = updaterReg.create();
	public Updater<ModeDisplType> setModePanel = updaterReg.create();
	public Updater<Vec3i> setTexturePanel = updaterReg.create();
	public Updater<Boolean> setVis = updaterReg.create();
	public Updater<Boolean> setDelEn = updaterReg.create();
	public Updater<Boolean> setAddEn = updaterReg.create();
	public Updater<String> setNameDisplay = updaterReg.create();
	public Updater<Void> updateGui = updaterReg.create();
	public Updater<Boolean> setUndoEn = updaterReg.create();
	public Updater<Boolean> setRedoEn = updaterReg.create();
	public Updater<Boolean> setGlow = updaterReg.create();
	public Updater<Integer> setPenColor = updaterReg.create();
	public Updater<Integer> setPartColor = updaterReg.create();
	public Updater<Boolean> setReColor = updaterReg.create();
	public Updater<Integer> setAnimFrame = updaterReg.create();
	public Updater<Vec3f> setAnimPos = updaterReg.create();
	public Updater<Vec3f> setAnimRot = updaterReg.create();
	public Updater<Boolean> setFrameAddEn = updaterReg.create();
	public Updater<Boolean> setAnimDelEn = updaterReg.create();
	public Updater<Boolean> setFrameDelEn = updaterReg.create();
	public Updater<Integer> setAnimDuration = updaterReg.create();
	public Updater<Integer> setAnimColor = updaterReg.create();
	public Updater<Boolean> setAnimShow = updaterReg.create();
	public Updater<Boolean> setAnimPlayEn = updaterReg.create();
	public Updater<EditorAnim> setSelAnim = updaterReg.create();
	public Updater<Boolean> setHiddenEffect = updaterReg.create();
	public Updater<Boolean> setSkinEdited = updaterReg.create();
	public Updater<String> setReload = updaterReg.create();
	public Updater<Boolean> setAnimPlay = updaterReg.create();
	public Updater<Integer> setAnimPriority = updaterReg.create();
	public Updater<Void> gestureFinished = updaterReg.create();

	public Supplier<Vec2i> cursorPos;
	public int penColor = 0xffffff;
	public int drawMode = 0;
	public boolean drawAllUVs = false;

	public Vec3f position = new Vec3f(0.5f, 1, 0.5f);
	public Vec3f look = new Vec3f(0.25f, 0.5f, 0.25f);
	public float camDist = 64;
	public Stack<Runnable> undoQueue = new Stack<>();
	public Stack<Runnable> redoQueue = new Stack<>();
	public Runnable currentOp;
	public boolean renderPaint;
	public boolean renderBase = true;
	public boolean applyAnim;
	public boolean playFullAnim;
	public boolean playerTpose;
	public long playStartTime, gestureStartTime;
	private StoreIDGen storeIDgen;
	public AnimationEncodingData animEnc;

	public EditorGui frame;
	public TreeElement selectedElement;
	public List<ModelElement> elements = new ArrayList<>();
	public EditorAnim selectedAnim;
	public List<EditorAnim> animsToPlay = new ArrayList<>();
	public IPose poseToApply;
	public List<EditorAnim> animations = new ArrayList<>();
	public List<EditorTemplate> templates = new ArrayList<>();
	public TemplateSettings templateSettings;
	public SkinType skinType;
	public boolean customSkinType;
	public Image vanillaSkin;
	public boolean dirty;
	public ModelDefinition definition;
	public EditorTexture skinProvider = new EditorTexture();
	public EditorTexture renderTexture = new EditorTexture();
	public EditorTexture listIconProvider;
	public TextureStitcher stitcher = new TextureStitcher();
	public File file;
	public ProjectFile project = new ProjectFile();

	public Editor(EditorGui gui) {
		this.definition = new EditorDefinition(this);
		this.frame = gui;
	}

	public void setVec(Vec3f v, VecType object) {
		if(selectedElement != null) {
			selectedElement.setVec(v, object);
		}
	}

	public void setVec(ModelElement selectedElement, Vec3f v, VecType object) {
		switch (object) {
		case SIZE:
		{
			addUndo(new ValueOp<>(selectedElement, selectedElement.size, (a, b) -> a.size = b));
			selectedElement.size = v;
			boolean changed = false;
			if(selectedElement.size.x < 0) {
				selectedElement.size.x = 0;
				changed = true;
			}
			if(selectedElement.size.y < 0) {
				selectedElement.size.y = 0;
				changed = true;
			}
			if(selectedElement.size.z < 0) {
				selectedElement.size.z = 0;
				changed = true;
			}
			if(selectedElement.size.x > 25) {
				selectedElement.size.x = 25;
				changed = true;
			}
			if(selectedElement.size.y > 25) {
				selectedElement.size.y = 25;
				changed = true;
			}
			if(selectedElement.size.z > 25) {
				selectedElement.size.z = 25;
				changed = true;
			}
			currentOp = new ValueOp<>(selectedElement, selectedElement.size, (a, b) -> a.size = b);
			if(changed)setSize.accept(selectedElement.size);
		}
		break;
		case OFFSET:
			addUndo(new ValueOp<>(selectedElement, selectedElement.offset, (a, b) -> a.offset = b));
			selectedElement.offset = v;
			posLimit(selectedElement.offset, setOffset);
			currentOp = new ValueOp<>(selectedElement, selectedElement.offset, (a, b) -> a.offset = b);
			break;
		case ROTATION:
			addUndo(new ValueOp<>(selectedElement, selectedElement.rotation, (a, b) -> a.rotation = b));
			selectedElement.rotation = v;
			if(v.x < 0 || v.x > 360 || v.y < 0 || v.y > 360 || v.z < 0 || v.z > 360) {
				while(selectedElement.rotation.x < 0)selectedElement.rotation.x += 360;
				while(selectedElement.rotation.x > 360)selectedElement.rotation.x -= 360;
				while(selectedElement.rotation.y < 0)selectedElement.rotation.y += 360;
				while(selectedElement.rotation.y > 360)selectedElement.rotation.y -= 360;
				while(selectedElement.rotation.z < 0)selectedElement.rotation.z += 360;
				while(selectedElement.rotation.z > 360)selectedElement.rotation.z -= 360;
				setRot.accept(selectedElement.rotation);
			}
			currentOp = new ValueOp<>(selectedElement, selectedElement.rotation, (a, b) -> a.rotation = b);
			break;
		case POSITION:
			addUndo(new ValueOp<>(selectedElement, selectedElement.pos, (a, b) -> a.pos = b));
			selectedElement.pos = v;
			posLimit(selectedElement.pos, setPosition);
			currentOp = new ValueOp<>(selectedElement, selectedElement.pos, (a, b) -> a.pos = b);
			break;
		case SCALE:
			addUndo(new ValueOp<>(selectedElement, selectedElement.scale, (a, b) -> a.scale = b));
			selectedElement.scale = v;
			{
				boolean changed = false;
				if(selectedElement.scale.x < 0) {
					selectedElement.scale.x = 0;
					changed = true;
				}
				if(selectedElement.scale.y < 0) {
					selectedElement.scale.y = 0;
					changed = true;
				}
				if(selectedElement.scale.z < 0) {
					selectedElement.scale.z = 0;
					changed = true;
				}
				if(selectedElement.scale.x > 25) {
					selectedElement.scale.x = 25;
					changed = true;
				}
				if(selectedElement.scale.y > 25) {
					selectedElement.scale.y = 25;
					changed = true;
				}
				if(selectedElement.scale.z > 25) {
					selectedElement.scale.z = 25;
					changed = true;
				}
				if(changed)setScale.accept(selectedElement.scale);
			}
			currentOp = new ValueOp<>(selectedElement, selectedElement.scale, (a, b) -> a.scale = b);
			break;

		case TEXTURE:
		{

			addUndo(
					new ValueOp<>(selectedElement, selectedElement.u, (a, b) -> a.u = b),
					new ValueOp<>(selectedElement, selectedElement.v, (a, b) -> a.v = b),
					new ValueOp<>(selectedElement, selectedElement.textureSize, (a, b) -> a.textureSize = b)
					);
			selectedElement.u = (int) v.x;
			selectedElement.v = (int) v.y;
			selectedElement.textureSize = (int) v.z;
			boolean refreshGui = false;
			if(selectedElement.u < 0) {
				selectedElement.u = 0;
				refreshGui = true;
			}
			if(selectedElement.v < 0) {
				selectedElement.v = 0;
				refreshGui = true;
			}
			if(selectedElement.textureSize < 1) {
				selectedElement.textureSize = 1;
				refreshGui = true;
			}
			currentOp = new OpList(
					new ValueOp<>(selectedElement, selectedElement.u, (a, b) -> a.u = b),
					new ValueOp<>(selectedElement, selectedElement.v, (a, b) -> a.v = b),
					new ValueOp<>(selectedElement, selectedElement.textureSize, (a, b) -> a.textureSize = b)
					);
			if(selectedElement.texture && refreshGui)
				setTexturePanel.accept(new Vec3i(selectedElement.u, selectedElement.v, selectedElement.textureSize));
			markDirty();

		}
		break;
		default:
			break;
		}
		markDirty();
	}

	private void posLimit(Vec3f pos, Consumer<Vec3f> setter) {
		boolean changed = false;
		if(Math.abs(pos.x) > Vec3f.MAX_POS) {
			if(pos.x < 0)pos.x = -Vec3f.MAX_POS;
			else pos.x = Vec3f.MAX_POS;
			changed = true;
		}
		if(Math.abs(pos.y) > Vec3f.MAX_POS) {
			if(pos.y < 0)pos.y = -Vec3f.MAX_POS;
			else pos.y = Vec3f.MAX_POS;
			changed = true;
		}
		if(Math.abs(pos.z) > Vec3f.MAX_POS) {
			if(pos.z < 0)pos.z = -Vec3f.MAX_POS;
			else pos.z = Vec3f.MAX_POS;
			changed = true;
		}
		if(changed)setter.accept(pos);
	}

	public void setName(String name) {
		if(selectedElement != null) {
			addUndo(new ValueOp<>(selectedElement, selectedElement.getElemName(), TreeElement::setElemName));
			selectedElement.setElemName(name);
			currentOp = new ValueOp<>(selectedElement, selectedElement.getElemName(), TreeElement::setElemName);
			updateGui.accept(null);
			markDirty();
		}
	}

	public void switchMode() {
		if(selectedElement != null) {
			selectedElement.modeSwitch();
		}
	}

	public void setColor(int color) {
		updateValue(color, TreeElement::setElemColor);
	}

	public void setMcScale(float value) {
		updateValue(value, TreeElement::setMCScale);
	}

	public void addNew() {
		if(selectedElement != null) {
			selectedElement.addNew();
		}
	}

	public void deleteSel() {
		if(selectedElement != null) {
			selectedElement.delete();
		}
	}

	public void switchVis() {
		if(selectedElement != null)selectedElement.switchVis();
	}

	public void switchMirror() {
		if(selectedElement != null)selectedElement.switchEffect(Effect.MIRROR);
	}

	public void switchGlow() {
		if(selectedElement != null)selectedElement.switchEffect(Effect.GLOW);
	}

	public void switchHide() {
		if(selectedElement != null)selectedElement.switchEffect(Effect.HIDE);
	}

	public void setTexSize(int x, int y) {
		EditorTexture tex = getTextureProvider();
		if(tex != null) {
			markDirty();
			int sx = tex.size.x;
			int sy = tex.size.y;
			addUndo(() -> {
				tex.size.x = sx;
				tex.size.y = sy;
			});
			runOp(() -> {
				tex.size.x = x;
				tex.size.y = y;
			});
		}
	}

	public void switchReColorEffect() {
		if(selectedElement != null)selectedElement.switchEffect(Effect.RECOLOR);
	}

	public void drawPixel(int x, int y) {
		switch (drawMode) {
		case 0:
			setPixel(x, y, penColor | 0xff000000);
			break;

		case 1:
			setPixel(x, y, 0);
			break;

		default:
			break;
		}
	}

	public void setPixel(int x, int y, int color) {
		EditorTexture tex = getTextureProvider();
		if(tex != null) {
			Box box = selectedElement != null ? selectedElement.getTextureBox() : null;
			if(box != null) {
				if(!box.isInBounds(x, y))return;
			}
			Image img = tex.getImage();
			if(x < 0 || y < 0 || x >= img.getWidth() || y >= img.getHeight())return;
			int old = img.getRGB(x, y);
			if(old == color)return;
			if(!tex.isEdited())setSkinEdited.accept(true);
			addUndo(() -> {
				tex.setRGB(x, y, old);
				refreshTexture(tex);
			});
			runOp(() -> {
				tex.setRGB(x, y, color);
				refreshTexture(tex);
			});
			markDirty();
		}
	}

	public void markDirty() {
		setNameDisplay.accept((file == null ? frame.getGui().i18nFormat("label.cpm.new_project") : file.getName()) + "*");
		dirty = true;
		redoQueue.clear();
		setUndoEn.accept(true);
		setRedoEn.accept(false);
	}

	public void updateGui() {
		setOffset.accept(null);
		setRot.accept(null);
		setPosition.accept(null);
		setSize.accept(null);
		setScale.accept(null);
		setMCScale.accept(null);
		setMirror.accept(null);
		updateName.accept(null);
		setModeBtn.accept(null);
		setModePanel.accept(ModeDisplType.NULL);
		setTexturePanel.accept(null);
		setVis.accept(null);
		setDelEn.accept(false);
		setAddEn.accept(false);
		setGlow.accept(null);
		setPartColor.accept(null);
		setReColor.accept(null);
		setAnimPos.accept(null);
		setAnimRot.accept(null);
		setAnimFrame.accept(null);
		setAnimDelEn.accept(false);
		setFrameAddEn.accept(false);
		setFrameDelEn.accept(false);
		setAnimDuration.accept(null);
		setAnimColor.accept(null);
		setAnimShow.accept(null);
		setAnimPlayEn.accept(false);
		setHiddenEffect.accept(null);
		setVis.accept(false);
		setAnimPriority.accept(null);

		if(templateSettings != null) {
			templateSettings.templateArgs.forEach(TemplateArgHandler::applyToModel);
		}
		templates.forEach(EditorTemplate::applyToModel);
		if(selectedElement != null) {
			selectedElement.updateGui();
		}
		setNameDisplay.accept((file == null ? frame.getGui().i18nFormat("label.cpm.new_project") : file.getName()) + (dirty ? "*" : ""));
		setUndoEn.accept(!undoQueue.empty());
		setRedoEn.accept(!redoQueue.empty());
		if(selectedAnim != null) {
			AnimFrame selFrm = selectedAnim.getSelectedFrame();
			if(selFrm != null) {
				ModelElement selectedElement = getSelectedElement();
				setAnimFrame.accept(selectedAnim.getFrames().indexOf(selFrm));
				if(selectedElement != null) {
					IElem dt = selFrm.getData(selectedElement);
					if(dt == null) {
						if(selectedAnim.add) {
							setAnimPos.accept(new Vec3f());
							setAnimRot.accept(new Vec3f());
						} else if(selectedElement.type == ElementType.ROOT_PART){
							PlayerPartValues val = PlayerPartValues.getFor((PlayerModelParts) selectedElement.typeData, skinType);
							setAnimPos.accept(val.getPos());
							setAnimRot.accept(new Vec3f());
						} else {
							setAnimPos.accept(selectedElement.pos);
							setAnimRot.accept(selectedElement.rotation);
						}
						if(!selectedElement.texture || selectedElement.recolor) {
							setAnimColor.accept(selectedElement.rgb);
						}
						setAnimShow.accept(selectedElement.show);
					} else {
						if(!selectedElement.texture || selectedElement.recolor) {
							Vec3f c = dt.getColor();
							setAnimColor.accept((((int) c.x) << 16) | (((int) c.y) << 8) | ((int) c.z));
						}
						setAnimPos.accept(dt.getPosition());
						setAnimRot.accept(dt.getRotation());
						setAnimShow.accept(dt.isVisible());
					}
				}
				setFrameDelEn.accept(true);
			}
			setFrameAddEn.accept(true);
			setAnimDelEn.accept(true);
			setAnimDuration.accept(selectedAnim.duration);
			setAnimPlayEn.accept(selectedAnim.getFrames().size() > 1);
			setAnimPriority.accept(selectedAnim.priority);
		}
		setSelAnim.accept(selectedAnim);
		EditorTexture tex = getTextureProvider();
		setSkinEdited.accept(tex != null ? tex.isEdited() : false);
		setReload.accept(tex != null && tex.file != null ? tex.file.getName() : null);
		updateGui.accept(null);
	}

	public void loadDefaultPlayerModel() {
		project = new ProjectFile();
		elements.clear();
		animations.clear();
		templates.clear();
		templateSettings = null;
		skinProvider.free();
		skinProvider.texture = null;
		skinProvider.setEdited(false);
		if(listIconProvider != null)listIconProvider.free();
		listIconProvider = null;
		undoQueue.clear();
		redoQueue.clear();
		skinType = MinecraftClientAccess.get().getSkinType();
		Image skin = MinecraftClientAccess.get().getVanillaSkin(skinType);
		this.vanillaSkin = skin;
		customSkinType = false;
		skinProvider.setImage(new Image(skin));
		skinProvider.size = new Vec2i(skin.getWidth(), skin.getHeight());
		dirty = false;
		file = null;
		selectedElement = null;
		selectedAnim = null;
		currentOp = null;
		animEnc = null;
		storeIDgen = new StoreIDGen();
		Player profile = MinecraftClientAccess.get().getClientPlayer();
		profile.loadSkin(() -> {
			if(!customSkinType)skinType = profile.getSkinType();
			CompletableFuture<Image> img = profile.getSkin();
			this.vanillaSkin = MinecraftClientAccess.get().getVanillaSkin(skinType);
			img.thenAccept(s -> {
				if(!skinProvider.isEdited()) {
					if(s != null) {
						this.vanillaSkin = s;
						skinProvider.setImage(new Image(this.vanillaSkin));
					} else {
						skinProvider.setImage(new Image(this.vanillaSkin));
					}
					restitchTexture();
				}
			});
		});
		for(PlayerModelParts type : PlayerModelParts.values()) {
			if(type != PlayerModelParts.CUSTOM_PART)
				elements.add(new ModelElement(this, ElementType.ROOT_PART, type, frame.getGui()));
		}
		restitchTexture();
	}

	public void preRender() {
		elements.forEach(ModelElement::preRender);
		if(this.applyAnim && this.selectedAnim != null) {
			if(this.playFullAnim) {
				long playTime = MinecraftClientAccess.get().getPlayerRenderManager().getAnimationEngine().getTime();
				long currentStep = (playTime - this.playStartTime);
				this.selectedAnim.applyPlay(currentStep);
				if(currentStep > this.selectedAnim.duration && !this.selectedAnim.loop && this.selectedAnim.pose == null){
					this.playFullAnim = false;
					setAnimPlay.accept(false);
				}
			} else {
				this.selectedAnim.apply();
			}
		} else if(this.applyAnim && !animsToPlay.isEmpty()) {
			animsToPlay.sort((a, b) -> Integer.compare(a.priority, b.priority));
			for (EditorAnim anim : animsToPlay) {
				long playTime = MinecraftClientAccess.get().getPlayerRenderManager().getAnimationEngine().getTime();
				long currentStep = (playTime - (anim.pose == null ? this.gestureStartTime : this.playStartTime));
				anim.applyPlay(currentStep);
				if(currentStep > anim.duration && !anim.loop && anim.pose == null){
					gestureFinished.accept(null);
				}
			}
			animsToPlay.clear();
		}
	}

	public void save(File file) throws IOException {
		Map<String, Object> data = new HashMap<>();
		List<Map<String, Object>> lst = new ArrayList<>();
		data.put("elements", lst);
		for (ModelElement elem : elements) {
			Map<String, Object> map = new HashMap<>();
			lst.add(map);
			map.put("id", ((PlayerModelParts) elem.typeData).name().toLowerCase());
			map.put("show", elem.show);
			if(!elem.children.isEmpty()) {
				List<Map<String, Object>> list = new ArrayList<>();
				map.put("children", list);
				saveChildren(elem, list);
			}
			map.put("pos", elem.pos.toMap());
			map.put("rotation", elem.rotation.toMap());
			map.put("dup", elem.duplicated);
		}
		data.put("version", projectFileVersion);
		Map<String, Object> skinSize = new HashMap<>();
		data.put("skinSize", skinSize);
		skinSize.put("x", skinProvider.size.x);
		skinSize.put("y", skinProvider.size.y);
		data.put("skinType", skinType.getName());
		try(OutputStreamWriter os = new OutputStreamWriter(project.setAsStream("config.json"))) {
			gson.toJson(data, os);
		}
		if(skinProvider.texture != null && skinProvider.isEdited()) {
			try(OutputStream os = project.setAsStream("skin.png")) {
				skinProvider.texture.getImage().storeTo(os);
			}
		}
		project.clearFolder("animations");
		for (EditorAnim e : animations) {
			data = new HashMap<>();
			data.put("additive", e.add);
			data.put("name", e.displayName);
			if(e.pose instanceof CustomPose)data.put("name", ((CustomPose)e.pose).getName());
			data.put("duration", e.duration);
			data.put("priority", e.priority);
			data.put("loop", e.loop);
			data.put("frames", e.writeFrames());
			try(OutputStreamWriter os = new OutputStreamWriter(project.setAsStream("animations/" + e.filename))) {
				gson.toJson(data, os);
			}
		}
		if(animEnc != null) {
			data = new HashMap<>();
			data.put("freeLayers", animEnc.freeLayers.stream().map(l -> l.getLowerName()).collect(Collectors.toList()));
			data.put("defaultValues", animEnc.defaultLayerValue.entrySet().stream().map(e -> Pair.of(e.getKey().getLowerName(), e.getValue())).collect(Collectors.toMap(Pair::getKey, Pair::getValue)));
			try(OutputStreamWriter os = new OutputStreamWriter(project.setAsStream("anim_enc.json"))) {
				gson.toJson(data, os);
			}
		}
		if(!templates.isEmpty()) {
			data = new HashMap<>();
			lst = new ArrayList<>();
			data.put("templates", lst);
			for (EditorTemplate templ : templates) {
				Map<String, Object> t = new HashMap<>();
				lst.add(t);
				templ.store(t);
			}
			try(OutputStreamWriter os = new OutputStreamWriter(project.setAsStream("templates.json"))) {
				gson.toJson(data, os);
			}
		}
		if(templateSettings != null) {
			data = new HashMap<>();
			lst = new ArrayList<>();
			data.put("args", lst);
			data.put("texture", templateSettings.hasTex);
			for(TemplateArgHandler arg : templateSettings.templateArgs) {
				Map<String, Object> map = new HashMap<>();
				lst.add(map);
				map.put("name", arg.name);
				map.put("desc", arg.desc);
				map.put("type", arg.type.name().toLowerCase());
				if(arg.handler.requiresParts() && arg.effectedElems != null) {
					List<Number> partList = new ArrayList<>();
					arg.effectedElems.forEach(e -> partList.add(e.storeID));
					map.put("parts", partList);
				}
				Map<String, Object> m = new HashMap<>();
				map.put("data", m);
				arg.handler.saveProject(m);
			}
			List<Object> dispIds = new ArrayList<>();
			data.put("displayElems", dispIds);
			walkElements(elements, e -> {
				if(e.templateElement) {
					dispIds.add(e.storeID);
				}
			});
			try(OutputStreamWriter os = new OutputStreamWriter(project.setAsStream("template_settings.json"))) {
				gson.toJson(data, os);
			}
		}
		project.save(file);
		this.file = file;
		dirty = false;
		updateGui();
	}

	private void saveChildren(ModelElement modelElement, List<Map<String, Object>> lst) {
		for (ModelElement elem : modelElement.children) {
			Map<String, Object> map = new HashMap<>();
			lst.add(map);
			map.put("name", elem.name);
			map.put("show", elem.show);
			map.put("texture", elem.texture);
			map.put("textureSize", elem.textureSize);
			map.put("offset", elem.offset.toMap());
			map.put("pos", elem.pos.toMap());
			map.put("rotation", elem.rotation.toMap());
			map.put("size", elem.size.toMap());
			map.put("scale", elem.scale.toMap());
			map.put("u", elem.u);
			map.put("v", elem.v);
			map.put("color", Integer.toHexString(elem.rgb));
			map.put("mirror", elem.mirror);
			map.put("mcScale", elem.mcScale);
			map.put("glow", elem.glow);
			map.put("recolor", elem.recolor);
			map.put("hidden", elem.hidden);
			storeIDgen.setID(elem);
			map.put("storeID", elem.storeID);

			if(!elem.children.isEmpty()) {
				List<Map<String, Object>> list = new ArrayList<>();
				map.put("children", list);
				saveChildren(elem, list);
			}
		}
	}

	@SuppressWarnings("unchecked")
	public void load_blockbench(File file) throws IOException {
		
	}
	
	@SuppressWarnings("unchecked")
	public void load(File file) throws IOException {
		loadDefaultPlayerModel();
		project.load(file);
		Map<String, Object> data;
		try(InputStreamReader rd = new InputStreamReader(project.getAsStream("config.json"))) {
			data = (Map<String, Object>) gson.fromJson(rd, Object.class);
		}
		byte[] ze = project.getEntry("skin.png");
		if(ze != null) {
			skinProvider.setImage(Image.loadFrom(new ByteArrayInputStream(ze)));
			skinProvider.markDirty();
		}
		int fileVersion = ((Number) data.getOrDefault("version", 0)).intValue();
		if(fileVersion != projectFileVersion)throw new IOException("Unsupported file version try a newer version of the mod");
		List<Map<String, Object>> lst = (List<Map<String, Object>>) data.get("elements");
		this.file = file;
		for (Map<String, Object> map : lst) {
			String key = (String) map.get("id");
			ModelElement elem = null;
			if((boolean) map.getOrDefault("dup", false)) {
				for (ModelElement e : elements) {
					if(((PlayerModelParts) e.typeData).name().equalsIgnoreCase(key)) {
						elem = new ModelElement(this, ElementType.ROOT_PART, e.typeData, frame.getGui());
						elem.duplicated = true;
						elements.add(elem);
						break;
					}
				}
			} else {
				for (ModelElement e : elements) {
					if(((PlayerModelParts) e.typeData).name().equalsIgnoreCase(key)) {
						elem = e;
						break;
					}
				}
			}
			if(elem != null) {
				elem.show = (boolean) map.get("show");
				if(map.containsKey("children")) {
					loadChildren((List<Map<String, Object>>) map.get("children"), elem, fileVersion);
				}
				elem.pos = new Vec3f((Map<String, Object>) map.get("pos"), new Vec3f(0, 0, 0));
				elem.rotation = new Vec3f((Map<String, Object>) map.get("rotation"), new Vec3f(0, 0, 0));
			}
		}
		if(data.containsKey("skinType")) {
			customSkinType = true;
			skinType = SkinType.get((String) data.get("skinType"));
		}
		List<String> anims = project.listEntires("animations");
		Map<String, Object> skinTexSize = (Map<String, Object>) data.get("skinSize");
		skinProvider.size = new Vec2i(((Number)skinTexSize.get("x")).intValue(), ((Number)skinTexSize.get("y")).intValue());
		if(anims != null) {
			for (String anim : anims) {
				try(InputStreamReader rd = new InputStreamReader(project.getAsStream("animations/" + anim))) {
					data = (Map<String, Object>) gson.fromJson(rd, Object.class);
				}
				IPose pose = null;
				String displayName = (String) data.getOrDefault("name", "Unnamed");
				AnimationType type = null;
				String[] sp = anim.split("_", 2);
				if(sp[0].equals("v")) {
					String poseName = sp[1].endsWith(".json") ? sp[1].substring(0, sp[1].length() - 5) : sp[1];
					for(VanillaPose p : VanillaPose.VALUES) {
						if(poseName.startsWith(p.name().toLowerCase())) {
							pose = p;
							type = AnimationType.POSE;
							break;
						}
					}
				} else if(sp[0].equals("c")) {
					pose = new CustomPose(displayName);
					type = AnimationType.POSE;
				} else if(sp[0].equals("g")) {
					type = AnimationType.GESTURE;
				}
				if(type == null)continue;
				EditorAnim e = new EditorAnim(this, anim, type, false);
				e.displayName = displayName;
				e.pose = pose;
				e.add = (boolean) data.get("additive");
				animations.add(e);
				e.duration = ((Number)data.get("duration")).intValue();
				e.priority = ((Number)data.getOrDefault("priority", 0)).intValue();
				e.loop = ((boolean)data.getOrDefault("loop", false));
				List<Map<String, Object>> frames = (List<Map<String, Object>>) data.get("frames");
				for (Map<String,Object> map : frames) {
					e.loadFrame(map);
				}
			}
		}
		ze = project.getEntry("anim_enc.json");
		if(ze != null) {
			try(InputStreamReader rd = new InputStreamReader(new ByteArrayInputStream(ze))) {
				data = (Map<String, Object>) gson.fromJson(rd, Object.class);
			}
			animEnc = new AnimationEncodingData();
			((List<String>) data.get("freeLayers")).forEach(v -> animEnc.freeLayers.add(PlayerSkinLayer.getLayer(v)));
			((Map<String, Boolean>) data.get("defaultValues")).forEach((k, v) -> animEnc.defaultLayerValue.put(PlayerSkinLayer.getLayer(k), v));
		}
		ze = project.getEntry("templates.json");
		if(ze != null) {
			try(InputStreamReader rd = new InputStreamReader(new ByteArrayInputStream(ze))) {
				data = (Map<String, Object>) gson.fromJson(rd, Object.class);
			}
			lst = (List<Map<String, Object>>) data.get("templates");
			for (Map<String,Object> map : lst) {
				EditorTemplate templ = EditorTemplate.load(this, map);
				templates.add(templ);
			}
		}
		ze = project.getEntry("template_settings.json");
		if(ze != null) {
			templateSettings = new TemplateSettings(this);
			try(InputStreamReader rd = new InputStreamReader(new ByteArrayInputStream(ze))) {
				data = (Map<String, Object>) gson.fromJson(rd, Object.class);
			}
			templateSettings.hasTex = (boolean) data.get("texture");
			lst = (List<Map<String, Object>>) data.get("args");
			for (Map<String, Object> map : lst) {
				TemplateArgType type = TemplateArgType.lookup((String) map.get("type"));
				TemplateArgHandler arg = new TemplateArgHandler(this, (String) map.get("name"), (String) map.get("desc"), type);
				templateSettings.templateArgs.add(arg);
				arg.handler.loadProject((Map<String, Object>) map.get("data"));
				if(arg.handler.requiresParts() && arg.effectedElems != null) {
					List<Number> partList = (List<Number>) map.get("parts");
					partList.forEach(e -> walkElements(elements, elem -> {
						if(elem.storeID == e.longValue()) {
							arg.effectedElems.add(elem);
						}
					}));
				}
			}
			List<Long> templElems = ((List<Number>) data.get("displayElems")).stream().map(Number::longValue).collect(Collectors.toList());
			walkElements(elements, e -> {
				if(templElems.contains(e.storeID)) {
					e.templateElement = true;
				}
			});
		}
		restitchTexture();
		updateGui();
	}

	@SuppressWarnings("unchecked")
	private void loadChildren(List<Map<String, Object>> list, ModelElement parent, int fileVer) {
		for (Map<String, Object> map : list) {
			ModelElement elem = new ModelElement(this);
			elem.parent = parent;
			parent.children.add(elem);
			elem.name = (String) map.get("name");
			elem.show = (boolean) map.get("show");
			elem.texture = (boolean) map.get("texture");
			elem.textureSize = ((Number)map.get("textureSize")).intValue();
			elem.offset = new Vec3f((Map<String, Object>) map.get("offset"), new Vec3f());
			elem.pos = new Vec3f((Map<String, Object>) map.get("pos"), new Vec3f());
			elem.rotation = new Vec3f((Map<String, Object>) map.get("rotation"), new Vec3f());
			elem.size = new Vec3f((Map<String, Object>) map.get("size"), new Vec3f(1, 1, 1));
			elem.scale = new Vec3f((Map<String, Object>) map.get("scale"), new Vec3f(1, 1, 1));
			elem.u = ((Number)map.get("u")).intValue();
			elem.v = ((Number)map.get("v")).intValue();
			elem.rgb = Integer.parseUnsignedInt((String) map.get("color"), 16);
			elem.mirror = (boolean) map.get("mirror");
			elem.mcScale = ((Number) map.get("mcScale")).floatValue();
			elem.glow = (boolean) map.getOrDefault("glow", false);
			elem.recolor = (boolean) map.getOrDefault("recolor", false);
			elem.hidden = (boolean) map.getOrDefault("hidden", false);
			elem.storeID = ((Number) map.getOrDefault("storeID", 0)).longValue();

			if(map.containsKey("children")) {
				loadChildren((List<Map<String, Object>>) map.get("children"), elem, fileVer);
			}
		}
	}

	public void reloadSkin() {
		EditorTexture tex = getTextureProvider();
		if(tex != null && tex.file != null) {
			try {
				Image img = Image.loadFrom(tex.file);
				if(img.getWidth() > 512 || img.getHeight() > 512)
					throw new IOException(frame.getGui().i18nFormat("label.cpm.tex_size_too_big", 512));
				tex.setImage(img);
				tex.setEdited(true);
				setSkinEdited.accept(true);
				restitchTexture();
			} catch (IOException e) {
				e.printStackTrace();
				frame.openPopup(new MessagePopup(frame.getGui(), frame.getGui().i18nFormat("label.cpm.error"), frame.getGui().i18nFormat("error.cpm.img_load_failed", e.getLocalizedMessage())));
			}
		}
	}

	public void saveSkin(File f) {
		EditorTexture tex = getTextureProvider();
		if(tex != null) {
			try {
				tex.getImage().storeTo(f);
				tex.file = f;
			} catch (IOException e) {
				frame.openPopup(new MessagePopup(frame.getGui(), frame.getGui().i18nFormat("label.cpm.error"), frame.getGui().i18nFormat("error.cpm.img_save_failed", e.getLocalizedMessage())));
			}
		}
	}

	public void addUndo(Runnable r) {
		undoQueue.push(r);
	}

	public void addUndo(Runnable... r) {
		if(r.length == 1)undoQueue.push(r[0]);
		else undoQueue.push(new OpList(r));
	}

	public void appendCurrentOp(Runnable r) {
		if(currentOp == null)currentOp = r;
		else if(currentOp instanceof OpList) {
			((OpList)currentOp).rs.add(r);
		} else {
			currentOp = new OpList(currentOp, r);
		}
	}

	public void runOp(Runnable r) {
		currentOp = r;
		r.run();
	}

	public void runOp(Runnable... r) {
		if(r.length == 1) {
			currentOp = r[0];
		} else {
			currentOp = new OpList(r);
		}
		currentOp.run();
	}

	public void setCurrentOp(Runnable... r) {
		if(r.length == 1) {
			currentOp = r[0];
		} else {
			currentOp = new OpList(r);
		}
	}

	private static class OpList implements Runnable {
		private List<Runnable> rs;

		public OpList(Runnable... r) {
			rs = new ArrayList<>(Arrays.asList(r));
		}

		@Override
		public void run() {
			rs.forEach(Runnable::run);
		}
	}

	public <E, T> void updateValueOp(E elem, T currVal, T newVal, BiConsumer<E, T> setter, Updater<T> updater) {
		addUndo(new ValueOp<>(elem, currVal, setter));
		updater.accept(newVal);
		runOp(new ValueOp<>(elem, newVal, setter));
		markDirty();
	}

	public <T> void updateValue(T value, BiConsumer<TreeElement, T> func) {
		if(selectedElement != null)func.accept(selectedElement, value);
	}

	public void undo() {
		if(undoQueue.empty())return;
		Runnable r = undoQueue.pop();
		if(r != null) {
			if(currentOp != null) {
				redoQueue.add(currentOp);
				currentOp = r;
				r.run();
			}
		}
		updateGui();
	}

	public void redo() {
		if(redoQueue.empty())return;
		Runnable r = redoQueue.pop();
		if(r != null) {
			if(currentOp != null) {
				undoQueue.add(currentOp);
				currentOp = r;
				r.run();
			}
		}
		updateGui();
	}

	public void moveElement(ModelElement element, ModelElement to) {
		if(checkChild(element.children, to))return;
		ModelElement me = element.parent;
		addUndo(() -> {
			to.children.remove(element);
			element.parent = me;
			me.children.add(element);
		});
		runOp(() -> {
			element.parent.children.remove(element);
			element.parent = to;
			to.children.add(element);
		});
		selectedElement = null;
		markDirty();
		updateGui();
	}

	private boolean checkChild(List<ModelElement> elem, ModelElement to) {
		for (ModelElement modelElement : elem) {
			if(modelElement == to)return true;
			if(checkChild(elem, to))return true;
		}
		return false;
	}

	public static void walkElements(List<ModelElement> elem, Consumer<ModelElement> c) {
		for (ModelElement modelElement : elem) {
			c.accept(modelElement);
			walkElements(modelElement.children, c);
		}
	}

	public void setAnimRot(Vec3f v) {
		if(selectedAnim != null) {
			if(v.x < 0 || v.x > 360 || v.y < 0 || v.y > 360 || v.z < 0 || v.z > 360) {
				while(v.x < 0)  v.x += 360;
				while(v.x > 360)v.x -= 360;
				while(v.y < 0)  v.y += 360;
				while(v.y > 360)v.y -= 360;
				while(v.z < 0)  v.z += 360;
				while(v.z > 360)v.z -= 360;
				setAnimRot.accept(v);
			}
			selectedAnim.setRotation(v);
		}
	}

	public void setAnimPos(Vec3f v) {
		if(selectedAnim != null) {
			selectedAnim.setPosition(v);
		}
	}

	public void addNewAnim(IPose pose, String displayName, boolean add, boolean loop) {
		String fname = null;
		AnimationType type;
		if(pose instanceof VanillaPose) {
			fname = "v_" + ((VanillaPose)pose).name().toLowerCase() + "_" + displayName.replaceAll("[^a-zA-Z0-9\\.\\-]", "") + "_" + (storeIDgen.newId() % 10000) + ".json";
			type = AnimationType.POSE;
		} else if(pose != null) {
			fname = "c_" + ((CustomPose) pose).getName().replaceAll("[^a-zA-Z0-9\\.\\-]", "") + "_" + (storeIDgen.newId() % 10000) + ".json";
			type = AnimationType.POSE;
		} else {
			fname = "g_" + displayName.replaceAll("[^a-zA-Z0-9\\.\\-]", "") + "_" + (storeIDgen.newId() % 10000) + ".json";
			type = AnimationType.GESTURE;
		}
		EditorAnim anim = new EditorAnim(this, fname, type, true);
		anim.pose = pose;
		anim.add = add;
		anim.loop = loop;
		anim.displayName = displayName;
		addUndo(() -> {
			animations.remove(anim);
			selectedAnim = null;
		});
		runOp(() -> animations.add(anim));
		selectedAnim = anim;
		markDirty();
		updateGui();
	}

	public void delSelectedAnim() {
		if(selectedAnim != null) {
			EditorAnim anim = selectedAnim;
			addUndo(() -> animations.add(anim));
			runOp(() -> animations.remove(anim));
			selectedAnim = null;
			updateGui();
		}
	}

	public void addNewAnimFrame() {
		if(selectedAnim != null) {
			selectedAnim.addFrame();
			updateGui();
		}
	}

	public void delSelectedAnimFrame() {
		if(selectedAnim != null) {
			selectedAnim.deleteFrame();
			updateGui();
		}
	}

	public void setAnimDuration(int value) {
		if(selectedAnim == null)return;
		updateValueOp(selectedAnim, selectedAnim.duration, value, (a, b) -> a.duration = b, setAnimDuration);
	}

	public void setAnimPriority(int value) {
		if(selectedAnim == null)return;
		updateValueOp(selectedAnim, selectedAnim.priority, value, (a, b) -> a.priority = b, setAnimPriority);
	}

	public void animPrevFrm() {
		if(selectedAnim != null) {
			selectedAnim.prevFrame();
			updateGui();
		}
	}

	public void animNextFrm() {
		if(selectedAnim != null) {
			selectedAnim.nextFrame();
			updateGui();
		}
	}

	public void setAnimColor(int rgb) {
		if(selectedAnim != null) {
			selectedAnim.setColor(rgb);
			updateGui();
		}
	}

	public void switchAnimShow() {
		if(selectedAnim != null) {
			selectedAnim.switchVisible();
			updateGui();
		}
	}

	public void applyRenderPoseForAnim(Consumer<VanillaPose> func) {
		if(applyAnim && selectedAnim != null && selectedAnim.pose instanceof VanillaPose) {
			func.accept((VanillaPose) selectedAnim.pose);
		} else if(applyAnim && poseToApply != null && poseToApply instanceof VanillaPose) {
			func.accept((VanillaPose) poseToApply);
		}
		poseToApply = null;
	}

	public UIColors colors() {
		return frame.getGui().getColors();
	}

	public void addSkinLayer() {
		OpList l = new OpList();
		OpList u = new OpList(() -> selectedElement = null);
		for(PlayerModelParts p : PlayerModelParts.VALUES) {
			for (ModelElement el : elements) {
				if(el.type == ElementType.ROOT_PART && el.typeData == p) {
					ModelElement elem = new ModelElement(this);
					l.rs.add(() -> el.children.add(elem));
					u.rs.add(() -> el.children.remove(elem));
					elem.parent = el;
					PlayerPartValues val = PlayerPartValues.getFor(p, skinType);
					elem.size = val.getSize();
					elem.offset = val.getOffset();
					elem.texture = true;
					elem.u = val.u2;
					elem.v = val.v2;
					elem.name = frame.getGui().i18nFormat("label.cpm.layer_" + val.layer.getLowerName());
					elem.mcScale = 0.25F;
					break;
				}
			}
		}
		runOp(l);
		addUndo(u);
		markDirty();
		updateGui();
	}

	public void convertModel() {
		OpList l = new OpList();
		OpList u = new OpList(() -> selectedElement = null);
		for(PlayerModelParts p : PlayerModelParts.VALUES) {
			for (ModelElement el : elements) {
				if(el.type == ElementType.ROOT_PART && el.typeData == p) {
					if(el.show) {
						ModelElement elem = new ModelElement(this);
						l.rs.add(() -> el.children.add(elem));
						u.rs.add(() -> el.children.remove(elem));
						elem.parent = el;
						PlayerPartValues val = PlayerPartValues.getFor(p, skinType);
						elem.size = val.getSize();
						elem.offset = val.getOffset();
						elem.texture = true;
						elem.u = val.u;
						elem.v = val.v;
						elem.name = el.name;
						elem.generated = true;
						l.rs.add(() -> el.show = false);
						u.rs.add(() -> el.show = true);
					}
					break;
				}
			}
		}
		runOp(l);
		addUndo(u);
		markDirty();
		updateGui();
	}

	public void setupTemplateModel() {
		OpList l = new OpList();
		OpList u = new OpList(() -> selectedElement = null);
		for(PlayerModelParts p : PlayerModelParts.VALUES) {
			for (ModelElement el : elements) {
				if(el.type == ElementType.ROOT_PART && el.typeData == p) {
					if(el.show) {
						ModelElement elem = new ModelElement(this);
						l.rs.add(() -> el.children.add(elem));
						u.rs.add(() -> el.children.remove(elem));
						elem.parent = el;
						PlayerPartValues val = PlayerPartValues.getFor(p, skinType);
						elem.size = val.getSize();
						elem.offset = val.getOffset();
						elem.texture = false;
						elem.rgb = 0xffffff;
						elem.name = el.name;
						elem.templateElement = true;
						l.rs.add(() -> el.show = false);
						u.rs.add(() -> el.show = true);
					}
					break;
				}
			}
		}
		runOp(l);
		addUndo(u);
	}

	public boolean hasVanillaParts() {
		for(PlayerModelParts p : PlayerModelParts.VALUES) {
			for (ModelElement el : elements) {
				if(el.type == ElementType.ROOT_PART && el.typeData == p) {
					if(el.show)return true;
				}
			}
		}
		return false;
	}

	public IGui gui() {
		return frame.getGui();
	}

	public EditorTexture getTextureProvider() {
		return selectedElement != null ? selectedElement.getTexture() : skinProvider;
	}

	public ModelElement getSelectedElement() {
		return selectedElement instanceof ModelElement ? (ModelElement) selectedElement : null;
	}

	public void free() {
		skinProvider.free();
		renderTexture.free();
		if(listIconProvider != null)listIconProvider.free();
	}

	public void refreshTexture(EditorTexture tex) {
		if(stitcher.refresh(tex.getImage()))
			renderTexture.markDirty();
	}

	public void restitchTexture() {
		stitcher = new TextureStitcher();
		stitcher.setBase(skinProvider);
		templates.forEach(e -> e.stitch(stitcher));
		stitcher.finish(renderTexture);
		renderTexture.markDirty();
		if(stitcher.hasStitches() && hasVanillaParts()) {
			convertModel();
		}
	}
}
