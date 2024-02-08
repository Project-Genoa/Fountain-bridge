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
			case "bed":
			{
				NbtMapBuilder builder = NbtMap.builder();
				builder.putString("id", "Bed");
				builder.putByte("color", (byte) switch ((String) blockEntityMapping.contents)
				{
					case "white" -> 0;
					case "orange" -> 1;
					case "magenta" -> 2;
					case "light_blue" -> 3;
					case "yellow" -> 4;
					case "lime" -> 5;
					case "pink" -> 6;
					case "gray" -> 7;
					case "light_gray" -> 8;
					case "cyan" -> 9;
					case "purple" -> 10;
					case "blue" -> 11;
					case "brown" -> 12;
					case "green" -> 13;
					case "red" -> 14;
					case "black" -> 15;
					default -> 0;
				});
				return builder.build();
			}
			default:
				throw new AssertionError();
		}
	}
}