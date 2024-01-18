package micheal65536.fountain.utils;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import org.apache.logging.log4j.LogManager;
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
		JavaItems.BedrockMapping bedrockMapping = JavaItems.getBedrockMapping(javaId);
		if (bedrockMapping == null)
		{
			LogManager.getLogger().warn("Attempt to translate item with no mapping " + JavaItems.getName(javaId));
			return builder.build();
		}
		builder.definition(bedrockCodecHelper.getItemDefinitions().getDefinition(bedrockMapping.id));

		if (bedrockMapping.toolWear)
		{
			CompoundTag nbt = itemStack.getNbt();
			int damage = nbt != null && nbt.contains("Damage") ? (int) nbt.get("Damage").getValue() : 0;
			builder.damage(damage);
		}
		else
		{
			builder.damage(bedrockMapping.aux);
		}

		builder.count(itemStack.getAmount());

		return builder.build();
	}
}