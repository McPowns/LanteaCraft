package lc.client;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.world.IBlockAccess;
import lc.api.rendering.IBlockRenderInfo;
import lc.common.base.LCBlock;
import lc.common.base.LCBlockRenderer;

public abstract class BlockVoidRenderer extends LCBlockRenderer {

	@Override
	public boolean renderInventoryBlock(Block block, RenderBlocks renderer, int metadata) {
		return false;
	}

	@Override
	public boolean renderWorldBlock(Block block, RenderBlocks renderer, IBlockAccess world, int x, int y, int z) {
		if (!(block instanceof LCBlock))
			return false;
		LCBlock lcb = (LCBlock) block;
		IBlockRenderInfo info = lcb.block();
		if (info == null)
			return false;
		return info.doProperty("noRender", world, world.getBlockMetadata(x, y, z), x, y, z, false);
	}
}
