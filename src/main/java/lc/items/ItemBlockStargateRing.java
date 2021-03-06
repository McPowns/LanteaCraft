package lc.items;

import lc.common.base.LCBlock;
import lc.common.base.LCItemBlock;
import lc.core.ResourceAccess;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;

/**
 * Stargate ring block item implementation.
 *
 * @author AfterLifeLochie
 *
 */
public class ItemBlockStargateRing extends LCItemBlock {

	/**
	 * Default constructor
	 *
	 * @param block
	 *            The parent block type.
	 */
	public ItemBlockStargateRing(Block block) {
		super((LCBlock) block);
	}

	@Override
	public IIcon getIconFromDamage(int i) {
		return blockType.getIcon(0, i);
	}

	@Override
	public int getMetadata(int i) {
		return i;
	}

	@Override
	public String getUnlocalizedName(ItemStack stack) {
		return subItemName(stack.getItemDamage());
	}

	@Override
	protected String getIconString() {
		return ResourceAccess.formatResourceName("${ASSET_KEY}:%s_${TEX_QUALITY}", getUnlocalizedName());
	}

	public static String subItemName(int i) {
		return "tile.stargateRing." + i;
	}
}
