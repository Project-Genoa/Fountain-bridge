package micheal65536.fountain.utils;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import org.apache.logging.log4j.LogManager;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.protocol.bedrock.codec.BedrockCodecHelper;
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import micheal65536.fountain.registry.JavaItems;

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

		int javaId = itemStack.getId();
		int bedrockId = JavaItems.getBedrockId(javaId);
		if (bedrockId == 0)
		{
			LogManager.getLogger().warn("Attempt to translate item with no mapping " + JavaItems.getName(javaId));
			return builder.build();
		}
		builder.definition(bedrockCodecHelper.getItemDefinitions().getDefinition(bedrockId));

		NbtMap nbtMap = JavaItems.getBedrockNBT(javaId);
		if (nbtMap != null)
		{
			builder.tag(nbtMap);
		}

		// TODO: tool damage

		builder.count(itemStack.getAmount());

		return builder.build();
	}
}