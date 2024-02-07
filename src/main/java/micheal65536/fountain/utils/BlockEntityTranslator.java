package micheal65536.fountain.utils;

import com.github.steveice10.mc.protocol.data.game.level.block.BlockEntityInfo;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import micheal65536.fountain.registry.JavaBlocks;

public class BlockEntityTranslator
{
	@NotNull
	public static NbtMap translateBlockEntity(@NotNull JavaBlocks.BedrockMapping.BlockEntity blockEntityMapping, @Nullable BlockEntityInfo javaBlockEntityInfo)
	{
		switch (blockEntityMapping.type)
		{
			case "flower_pot":
			{
				NbtMapBuilder builder = NbtMap.builder();
				builder.putString("id", "FlowerPot");
				if (blockEntityMapping.contents != null)
				{
					builder.putCompound("PlantBlock", (NbtMap) blockEntityMapping.contents);
				}
				return builder.build();
			}
			default:
				throw new AssertionError();
		}
	}
}