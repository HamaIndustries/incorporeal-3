package agency.highlysuspect.incorporeal.platform.forge.client;

import agency.highlysuspect.incorporeal.client.IncClientItemProperties;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.IItemRenderProperties;

import vazkii.botania.client.render.tile.TEISR;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Literally a copy paste from Botania, changing the backing map used for teisr shenanigans.
 * Not gonna pretend to know how this works, and I hate forge's model BS so I don't really care to.
 * See IncBlockItemWithTEISRForgeMixin.
 */
public class IncForgeBlockEntityItemRendererHelper {
	// Nulls in ctor call are fine, we don't use those fields
	private static final BlockEntityWithoutLevelRenderer RENDERER = new BlockEntityWithoutLevelRenderer(null, null) {
		private final Map<Item, TEISR> renderers = new IdentityHashMap<>();
		
		@Override
		public void renderByItem(ItemStack stack, ItemTransforms.TransformType transform,
		                         PoseStack ps, MultiBufferSource buffers, int light, int overlay) {
			var renderer = renderers.computeIfAbsent(stack.getItem(), i -> {
				var block = Block.byItem(i);
				//return EntityRenderers.BE_ITEM_RENDERER_FACTORIES.get(block).apply(block);
				return IncClientItemProperties.BE_ITEM_RENDERER_FACTORIES.get(block).apply(block);
			});
			renderer.render(stack, transform, ps, buffers, light, overlay);
		}
	};
	
	private static final IItemRenderProperties PROPS = new IItemRenderProperties() {
		@Override
		public BlockEntityWithoutLevelRenderer getItemStackRenderer() {
			return IncForgeBlockEntityItemRendererHelper.RENDERER;
		}
	};
	
	public static void initItem(Consumer<IItemRenderProperties> consumer) {
		consumer.accept(PROPS);
	}
}
