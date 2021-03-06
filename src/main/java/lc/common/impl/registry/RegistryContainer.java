package lc.common.impl.registry;

import lc.api.components.IComponentRegistry;
import lc.api.components.IDefinitionRegistry;
import lc.api.components.IRecipeRegistry;
import lc.api.components.IRegistryContainer;

/**
 * Registry container implementation.
 *
 * @author AfterLifeLochie
 *
 */
public class RegistryContainer implements IRegistryContainer {

	private ComponentRegistry components = new ComponentRegistry();
	private DefinitionRegistry definitions = new DefinitionRegistry();
	private RecipeRegistry recipes = new RecipeRegistry();

	@Override
	public IComponentRegistry components() {
		return components;
	}

	@Override
	public IDefinitionRegistry definitions() {
		return definitions;
	}

	@Override
	public IRecipeRegistry recipes() {
		return recipes;
	}

}
