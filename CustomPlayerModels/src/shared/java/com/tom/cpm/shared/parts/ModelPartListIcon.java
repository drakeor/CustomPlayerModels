package com.tom.cpm.shared.parts;

import java.io.IOException;

import com.tom.cpm.shared.definition.ModelDefinition;
import com.tom.cpm.shared.definition.ModelDefinitionLoader;
import com.tom.cpm.shared.editor.Editor;
import com.tom.cpm.shared.io.IOHelper;
import com.tom.cpm.shared.skin.TextureProvider;

public class ModelPartListIcon implements IModelPart, IResolvedModelPart {
	private TextureProvider image;

	public ModelPartListIcon(IOHelper in, ModelDefinitionLoader loader) throws IOException {
		image = new TextureProvider(in, 32);
	}

	public ModelPartListIcon(Editor editor) {
		this.image = editor.listIconProvider;
	}

	@Override
	public IResolvedModelPart resolve() throws IOException {
		return this;
	}

	@Override
	public void apply(ModelDefinition def) {
		def.setListIconOverride(image);
	}

	@Override
	public void write(IOHelper dout) throws IOException {
		image.write(dout);
	}

	@Override
	public ModelPartType getType() {
		return ModelPartType.LIST_ICON;
	}
}
