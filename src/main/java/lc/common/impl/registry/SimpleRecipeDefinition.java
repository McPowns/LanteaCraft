package lc.common.impl.registry;

import java.util.HashMap;
import java.util.Map;

import lc.api.components.RecipeType;
import lc.api.defs.IContainerDefinition;
import lc.api.defs.IDefinitionReference;
import lc.api.defs.IGameDef;
import lc.api.defs.IRecipeDefinition;
import lc.common.LCLog;
import net.minecraft.item.ItemStack;

public class SimpleRecipeDefinition implements IRecipeDefinition {

	private String name;
	private RecipeType type;

	private HashMap<Integer, Object> inputs;
	private HashMap<Integer, Object> outputs;

	private HashMap<Integer, ItemStack> stackInputs;
	private HashMap<Integer, ItemStack> stackOutputs;
	private HashMap<Integer, Boolean> use;

	public SimpleRecipeDefinition(String name, RecipeType type, Object output, String grid, Object... inputs) {
		this.name = name;
		this.type = type;
		this.inputs = new HashMap<Integer, Object>();
		outputs = new HashMap<Integer, Object>();
		use = new HashMap<Integer, Boolean>();

		for (int i = 0; i < 9; i++)
			use.put(i, true);
		outputs.put(0, output);
		char[] cells = grid.toCharArray();
		for (int i = 0; i < cells.length; i++) {
			char cell = cells[i];
			if (Character.isDigit(cell))
				this.inputs.put(i, inputs[Character.getNumericValue(cell)]);
		}
	}

	@Override
	public void evaluateRecipe() {
		stackInputs = new HashMap<Integer, ItemStack>();
		for (int i = 0; i < inputs.size(); i++) {
			Object val = inputs.get(i);
			if (!(val instanceof ItemStack))
				stackInputs.put(i, resolve(val));
			else
				stackInputs.put(i, (ItemStack) val);
		}

		stackOutputs = new HashMap<Integer, ItemStack>();
		for (int i = 0; i < outputs.size(); i++) {
			Object val = outputs.get(i);
			if (!(val instanceof ItemStack))
				stackOutputs.put(i, resolve(val));
			else
				stackOutputs.put(i, (ItemStack) val);
		}
	}

	private ItemStack resolve(Object val) {
		if (val instanceof DefinitionReference) {
			DefinitionReference reference = (DefinitionReference) val;
			IGameDef def = reference.reference();
			if (def == null) {
				LCLog.fatal("Invalid reference, cannot resolve recipe.");
				return null;
			}
			Object[] params = reference.parameters();
			Integer count = null, metadata = null;
			if (params != null) {
				if (params.length >= 1)
					count = (Integer) params[0];
				if (params.length == 2)
					metadata = (Integer) params[1];
			}
			if (def instanceof IContainerDefinition) {
				IContainerDefinition blockItemDef = (IContainerDefinition) def;
				if (blockItemDef.getBlock() != null)
					return new ItemStack(blockItemDef.getBlock(), count == null ? 1 : count, metadata == null ? 0
							: metadata);
				else if (blockItemDef.getItem() != null)
					return new ItemStack(blockItemDef.getItem(), count == null ? 1 : count, metadata == null ? 0
							: metadata);
			} else {
				LCLog.fatal("Unsupported definition type %s.", def.getClass().getName());
				return null;
			}
		}
		LCLog.fatal("Cannot resolve object of type %s into ItemStack.", val.getClass().getName());
		return null;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public RecipeType getType() {
		return type;
	}

	@Override
	public Map<Integer, ItemStack> getInputStacks() {
		return stackInputs;
	}

	@Override
	public Map<Integer, Boolean> getInputConsumption() {
		return use;
	}

	@Override
	public Map<Integer, ItemStack> getOutputStacks() {
		return stackOutputs;
	}

	@Override
	public IDefinitionReference ref() {
		return new DefinitionReference(this);
	}

}
