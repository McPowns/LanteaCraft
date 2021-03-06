package lc.common.impl.registry;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import lc.api.components.IComponentRegistry;
import lc.api.components.IDefinitionRegistry;
import lc.api.defs.IContainerDefinition;
import lc.api.defs.ILanteaCraftRenderer;
import lc.common.LCLog;
import lc.common.base.LCBlock;
import lc.common.base.LCBlockRenderer;
import lc.common.base.LCItemBucket;
import lc.common.base.LCTile;
import lc.common.base.LCTileRenderer;
import lc.common.util.LCCreativeTabManager;
import lc.core.BuildInfo;
import lc.core.LCRuntime;
import net.minecraft.block.Block;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.inventory.Container;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.WeightedRandomChestContent;
import net.minecraftforge.common.ChestGenHooks;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;
import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler;
import cpw.mods.fml.client.registry.RenderingRegistry;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.common.registry.VillagerRegistry;
import cpw.mods.fml.common.registry.VillagerRegistry.IVillageTradeHandler;

/**
 * Global definition registry implementation.
 *
 * @author AfterLifeLochie
 *
 */
public class DefinitionRegistry implements IDefinitionRegistry {

	/**
	 * Types of renderers managed by this registry.
	 *
	 * @author AfterLifeLochie
	 */
	public static enum RendererType {
		/** Block type */
		BLOCK,
		/** Entity type */
		ENTITY,
		/** Tile type */
		TILE;
	}

	/** Pool of all known definitions. */
	private final Map<String, IContainerDefinition> definitionPool;
	/** Internal list of all registered Container instances. */
	private final Map<Integer, Class<? extends Container>> registeredContainers;
	/** Internal list of all registered GUI instances. */
	private final Map<Integer, Class<? extends GuiScreen>> registeredGUIs;
	/** Internal list of all registered renderers. */
	private final Map<RendererType, Map<Class<?>, Class<? extends ILanteaCraftRenderer>>> registeredRenderers;
	/** Internal list of all initialized renderers. */
	private final Map<RendererType, Map<Class<?>, ILanteaCraftRenderer>> initializedRenderers;

	/** Default constructor */
	public DefinitionRegistry() {
		definitionPool = new HashMap<String, IContainerDefinition>();
		registeredContainers = new HashMap<Integer, Class<? extends Container>>();
		registeredGUIs = new HashMap<Integer, Class<? extends GuiScreen>>();
		registeredRenderers = new HashMap<RendererType, Map<Class<?>, Class<? extends ILanteaCraftRenderer>>>();
		initializedRenderers = new HashMap<DefinitionRegistry.RendererType, Map<Class<?>, ILanteaCraftRenderer>>();
	}

	@Override
	public void addDefinition(IContainerDefinition definition) {
		if (definitionPool.containsKey(definition.getName().toLowerCase()))
			throw new RuntimeException("Attempt to overwrite existing definition " + definition.getName());
		definitionPool.put(definition.getName().toLowerCase(), definition);
	}

	@Override
	public IContainerDefinition getDefinition(String name) {
		return definitionPool.get(name.toLowerCase());
	}

	/**
	 * Initializes the registry
	 *
	 * @param runtime
	 *            The LanteaCraft runtime instance
	 * @param event
	 *            The FML event initializing the runtime
	 */
	public void init(LCRuntime runtime, FMLInitializationEvent event) {
		IComponentRegistry components = runtime.registries().components();
		LCLog.debug("Evaluating %s definitions for candidacy.", definitionPool.size());
		for (IContainerDefinition definition : definitionPool.values())
			if (definition instanceof BlockItemDefinition) {
				BlockItemDefinition element = (BlockItemDefinition) definition;
				if (components.isEnabled(element.getComponentOwner())) {
					LCLog.trace("Registering element %s, component %s enabled.", element.getName(),
							element.getComponentOwner());
					element.init(this);
				} else
					LCLog.trace("Dropping registration for element %s, component %s disabled.", element.getName(),
							element.getComponentOwner());
			} else
				LCLog.warn("Strange definition type %s, ignoring it.", definition.getClass().getName());
	}

	/**
	 * Register a Block with a given class and unlocalized name. The block will
	 * use the default {@link ItemBlock} structure when held as an item. The
	 * block will display in the default Creative tabs.
	 *
	 * @param classOf
	 *            The class of the block.
	 * @param unlocalizedName
	 *            The unlocalized name.
	 * @return The Block singleton.
	 */
	public <T extends Block> T registerBlock(Class<? extends T> classOf, String unlocalizedName) {
		return registerBlock(classOf, ItemBlock.class, unlocalizedName);
	}

	/**
	 * Register a block with a given class, a given item class and an
	 * unlocalized name. The block will display in the default Creative tabs.
	 *
	 * @param classOf
	 *            The class of the block.
	 * @param itemClassOf
	 *            The class of the item.
	 * @param unlocalizedName
	 *            The unlocalized name.
	 * @return The Block singleton.
	 */
	public <T extends Block> T registerBlock(Class<? extends T> classOf, Class<? extends ItemBlock> itemClassOf,
			String unlocalizedName) {
		return registerBlock(classOf, itemClassOf, unlocalizedName, LCCreativeTabManager.getTab("LanteaCraft"));
	}

	/**
	 * Register a block with a given class, a given item class, an unlocalized
	 * name and a display preference in CreativeTabs.
	 *
	 * @param classOf
	 *            The class of the block.
	 * @param itemClassOf
	 *            The class of the item.
	 * @param unlocalizedName
	 *            The unlocalized name.
	 * @param tab
	 *            The creative tab to place the Block into.
	 * @return The Block singleton.
	 */
	public <T extends Block> T registerBlock(Class<? extends T> classOf, Class<? extends ItemBlock> itemClassOf,
			String unlocalizedName, CreativeTabs tab) {
		LCLog.debug("Attempting to register block %s", unlocalizedName);
		try {
			Constructor<? extends T> ctor = classOf.getConstructor();
			T theMysteryBlock = ctor.newInstance();
			theMysteryBlock.setBlockName(unlocalizedName);
			theMysteryBlock.setCreativeTab(tab);
			GameRegistry.registerBlock(theMysteryBlock, itemClassOf, unlocalizedName);
			return theMysteryBlock;
		} catch (Throwable e) {
			LCLog.fatal("Failed to register block, an exception occured.", e);
			throw new RuntimeException(e);
		}
	}

	/**
	 * Registers an item with a given class and an unlocalized name.
	 *
	 * @param classOf
	 *            The class of the item.
	 * @param unlocalizedName
	 *            The unlocalized name.
	 * @return The Item singleton.
	 */
	public <T extends Item> T registerItem(Class<? extends T> classOf, String unlocalizedName) {
		return registerItem(classOf, unlocalizedName, LCCreativeTabManager.getTab("LanteaCraft"));
	}

	/**
	 * Registers an item with a given class and an unlocalized name.
	 *
	 * @param classOf
	 *            The class of the item.
	 * @param unlocalizedName
	 *            The unlocalized name.
	 * @param tab
	 *            The creative tab to place the Item into.
	 * @return The Item singleton.
	 */
	public <T extends Item> T registerItem(Class<? extends T> classOf, String unlocalizedName, CreativeTabs tab) {
		LCLog.debug("Attempting to register item " + unlocalizedName);
		try {
			Constructor<? extends T> ctor = classOf.getConstructor();
			T theMysteryItem = ctor.newInstance();
			theMysteryItem.setUnlocalizedName(unlocalizedName).setCreativeTab(tab);
			GameRegistry.registerItem(theMysteryItem, unlocalizedName);
			return theMysteryItem;
		} catch (Exception e) {
			LCLog.fatal("Failed to register item, an exception occured.", e);
			throw new RuntimeException(e);
		}
	}

	/**
	 * Registers a special bucket.
	 *
	 * @param hostOf
	 *            The host fluid.
	 * @param unlocalizedName
	 *            The unlocalized name for the bucket.
	 * @param bucketTextureName
	 *            The texture target for the bucket.
	 * @param tab
	 *            The creative tab to display the bucket on.
	 * @return The ItemSpecialBucket singleton for this host fluid.
	 */
	public LCItemBucket registerSpecialBucket(LCBlock hostOf, String unlocalizedName, String bucketTextureName,
			CreativeTabs tab) {
		LCLog.debug("Attempting to register SpecialBucket " + unlocalizedName);
		LCItemBucket bucket = new LCItemBucket(hostOf);
		bucket.setUnlocalizedName(unlocalizedName).setCreativeTab(tab);
		bucket.setTargetTexture(bucketTextureName);
		GameRegistry.registerItem(bucket, unlocalizedName);
		return bucket;
	}

	/**
	 * Registers a trade handler for a type of villager.
	 *
	 * @param villagerID
	 *            The villager type ID.
	 * @param handler
	 *            The handler to register.
	 */
	public void addTradeHandler(int villagerID, IVillageTradeHandler handler) {
		LCLog.debug("Registering trade handler for villager ID " + villagerID);
		VillagerRegistry.instance().registerVillageTradeHandler(villagerID, handler);
	}

	/**
	 * Registers an ore with the ore dictionary.
	 *
	 * @param name
	 *            The ore name.
	 * @param item
	 *            The ItemStack.
	 */
	public void registerOre(String name, ItemStack item) {
		LCLog.debug("Registering ore with name " + name);
		OreDictionary.registerOre(String.format("%s:%s", BuildInfo.modID, name), item);
	}

	/**
	 * Creates a new shaped recipe.
	 *
	 * @param product
	 *            The product ItemStack.
	 * @param params
	 *            The crafting arrangement.
	 */
	public void newRecipe(ItemStack product, Object... params) {
		LCLog.debug("Registering new generic recipe");
		GameRegistry.addRecipe(new ShapedOreRecipe(product, params));
	}

	/**
	 * Creates a new shapeless recipe.
	 *
	 * @param product
	 *            The product ItemStack.
	 * @param params
	 *            The crafting components.
	 */
	public void newShapelessRecipe(ItemStack product, Object... params) {
		LCLog.debug("Registering new generic shapeless recipe");
		GameRegistry.addRecipe(new ShapelessOreRecipe(product, params));
	}

	/**
	 * Registers a new smelting recipe.
	 *
	 * @param in
	 *            The input stack
	 * @param out
	 *            The output result
	 * @param xp
	 *            Quantity of XP earnt
	 */
	public void newSmeltingRecipe(ItemStack in, ItemStack out, float xp) {
		LCLog.debug("Registering new smelting recipe");
		FurnaceRecipes.smelting().func_151394_a(in, out, xp);
	}

	/**
	 * Registers a new chest item handler.
	 *
	 * @param stack
	 *            The ItemStack to add.
	 * @param minQty
	 *            The minimum random item quantity.
	 * @param maxQty
	 *            The maximum random item quantity.
	 * @param weight
	 *            The weighting of the random number generation for this random.
	 * @param category
	 *            The categories of chests this rule applies to.
	 */
	public void addRandomChestItem(ItemStack stack, int minQty, int maxQty, int weight, String... category) {
		WeightedRandomChestContent item = new WeightedRandomChestContent(stack, minQty, maxQty, weight);
		for (String element : category) {
			LCLog.debug("Adding new WeightedRandomChestContent for element " + element);
			ChestGenHooks.addItem(element, item);
		}
	}

	/**
	 * Register a block renderer
	 *
	 * @param renderer
	 *            A block renderer
	 */
	public void registerRenderer(ISimpleBlockRenderingHandler renderer) {
		// TODO: Fix rendering registry :<
		// renderer.renderID = RenderingRegistry.getNextAvailableRenderId();
		RenderingRegistry.registerBlockHandler(renderer);
	}

	/**
	 * Registers a TileEntity
	 *
	 * @param tileType
	 *            The tile class type.
	 * @param tileName
	 *            The tile name.
	 */
	public void registerTileEntity(Class<? extends LCTile> tileType, String tileName) {
		GameRegistry.registerTileEntity(tileType, tileName);
	}

	/**
	 * Registers a tile entity renderer
	 *
	 * @param teClass
	 *            The tile entity class
	 * @param renderer
	 *            The renderer object
	 */
	public void addTileEntityRenderer(Class<? extends TileEntity> teClass, Object renderer) {
		ClientRegistry.bindTileEntitySpecialRenderer(teClass, (TileEntitySpecialRenderer) renderer);
	}

	/**
	 * Registers a block renderer with the definition registry
	 *
	 * @param block
	 *            The block class
	 * @param renderer
	 *            The block renderer to bind
	 */
	public void registerBlockRenderer(Class<? extends LCBlock> block, Class<? extends LCBlockRenderer> renderer) {
		if (!registeredRenderers.containsKey(RendererType.BLOCK))
			registeredRenderers.put(RendererType.BLOCK, new HashMap<Class<?>, Class<? extends ILanteaCraftRenderer>>());
		registeredRenderers.get(RendererType.BLOCK).put(block, renderer);
	}

	/**
	 * Registers a tile entity renderer with the definition registry
	 *
	 * @param tile
	 *            The tile class
	 * @param renderer
	 *            The tile renderer to bind
	 */
	public void registerTileRenderer(Class<? extends LCTile> tile, Class<? extends LCTileRenderer> renderer) {
		if (!registeredRenderers.containsKey(RendererType.TILE))
			registeredRenderers.put(RendererType.TILE, new HashMap<Class<?>, Class<? extends ILanteaCraftRenderer>>());
		registeredRenderers.get(RendererType.TILE).put(tile, renderer);
	}

	/**
	 * Registers an entity renderer with the game registry
	 *
	 * @param entity
	 *            The entity class
	 * @param renderer
	 *            The renderer object
	 */
	public void registerEntityRenderer(Class<? extends Entity> entity, Object renderer) {
		RenderingRegistry.registerEntityRenderingHandler(entity, (Render) renderer);
	}

	/**
	 * Get a renderer for a class. If a dedicated renderer doesn't exist, null
	 * is returned.
	 *
	 * @param typeof
	 *            The element type.
	 * @param clazz
	 *            The element class.
	 * @return A dedicated renderer, or null if none is known.
	 */
	public ILanteaCraftRenderer getRendererFor(RendererType typeof, Class<?> clazz) {
		if (initializedRenderers.containsKey(typeof))
			for (Entry<Class<?>, ILanteaCraftRenderer> renderer : initializedRenderers.get(typeof).entrySet())
				if (renderer.getKey().equals(clazz))
					return renderer.getValue();
		if (!registeredRenderers.containsKey(typeof))
			return null;
		Map<Class<?>, Class<? extends ILanteaCraftRenderer>> typemap = registeredRenderers.get(typeof);
		for (Entry<Class<?>, Class<? extends ILanteaCraftRenderer>> type : typemap.entrySet())
			if (type.getKey().equals(clazz))
				try {
					ILanteaCraftRenderer renderer = type.getValue().getConstructor().newInstance();
					if (!initializedRenderers.containsKey(typeof))
						initializedRenderers.put(typeof, new HashMap<Class<?>, ILanteaCraftRenderer>());
					initializedRenderers.get(typeof).put(type.getValue(), renderer);
					return renderer;
				} catch (Throwable t) {
					LCLog.warn("Failed to initialize renderer.", t);
					return null;
				}
		return null;
	}

	/**
	 * Get a renderer from a class object.
	 *
	 * @param typeof
	 *            The type of renderer.
	 * @param type
	 *            The renderer's class
	 * @return The singleton of the renderer, or null if it doesn't exist.
	 */
	public ILanteaCraftRenderer getRenderer(RendererType typeof, Class<? extends ILanteaCraftRenderer> type) {
		if (initializedRenderers.containsKey(typeof))
			for (Entry<Class<?>, ILanteaCraftRenderer> renderer : initializedRenderers.get(typeof).entrySet())
				if (renderer.getValue().getClass().equals(type))
					return renderer.getValue();
		return null;
	}

	/**
	 * Register a container with the definition registry.
	 *
	 * @param id
	 *            The container ID
	 * @param cls
	 *            The container class
	 */
	public void registerContainer(int id, Class<? extends Container> cls) {
		LCLog.debug("Registering container with ID " + id + ", class " + cls.getCanonicalName());
		registeredContainers.put(id, cls);
	}

	/**
	 * Register a GUI with the definition registry.
	 *
	 * @param id
	 *            The GUI ID
	 * @param cls
	 *            The GUI class
	 */
	public void registerGui(int id, Class<? extends GuiScreen> cls) {
		LCLog.debug("Registering GUI with ID " + id + ", class " + cls.getCanonicalName());
		registeredGUIs.put(id, cls);
	}

	/**
	 * Get a registered container class
	 *
	 * @param id
	 *            The container ID
	 * @return The container class
	 */
	public Class<? extends Container> getRegisteredContainer(int id) {
		return registeredContainers.get(id);
	}

	/**
	 * Get a registered GUI class
	 *
	 * @param id
	 *            The GUI ID
	 * @return The GUI class
	 */
	public Class<? extends GuiScreen> getRegisteredGui(int id) {
		return registeredGUIs.get(id);
	}

}
