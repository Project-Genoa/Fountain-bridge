package micheal65536.fountain.utils;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import org.apache.logging.log4j.LogManager;
import org.cloudburstmc.protocol.bedrock.codec.BedrockCodecHelper;
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import micheal65536.fountain.registry.BedrockItemPalette;
import micheal65536.fountain.registry.ItemMappings;
import micheal65536.fountain.registry.JavaItemPalette;

public final class ItemTranslator
{
	@NotNull
	public static ItemData translateJavaToBedrock(@Nullable ItemStack itemStack, @NotNull BedrockCodecHelper bedrockCodecHelper)
	{
		ItemData.Builder builder = ItemData.builder();

		if (itemStack == null)
		{
			return builder.build();
		}

		int javaItemId = itemStack.getId();
		String javaName = JavaItemPalette.getName(javaItemId);
		String bedrockName = ItemMappings.getBedrockName(javaName);
		int bedrockItemId = BedrockItemPalette.getId(bedrockName);
		if (bedrockItemId == 0)
		{
			LogManager.getLogger().warn("Cannot find Bedrock item ID for " + bedrockName);
			return builder.build();
		}
		builder.definition(bedrockCodecHelper.getItemDefinitions().getDefinition(bedrockItemId));

		// TODO: NBT data

		builder.count(itemStack.getAmount());

		return builder.build();
	}
}