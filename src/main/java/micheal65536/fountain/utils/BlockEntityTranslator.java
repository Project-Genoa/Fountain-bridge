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
	public static NbtMap translateBlockEntity(int x, int y, int z, @NotNull JavaBlocks.BedrockMapping.BlockEntity blockEntityMapping, @Nullable BlockEntityInfo javaBlockEntityInfo)
	{
		switch (blockEntityMapping.type)
		{
			case "flower_pot":
			{
				NbtMapBuilder builder = createBaseBuilder("FlowerPot", x, y, z);
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

	@NotNull
	private static NbtMapBuilder createBaseBuilder(@NotNull String type, int x, int y, int z)
	{
		return NbtMap.builder()
				.putString("id", type)
				.putInt("x", x)
				.putInt("y", y)
				.putInt("z", z)
				.putBoolean("isMovable", false);
	}
}