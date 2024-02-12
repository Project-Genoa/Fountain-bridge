package micheal65536.fountain.utils;

import com.github.steveice10.mc.protocol.data.game.level.block.BlockEntityInfo;
import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.github.steveice10.opennbt.tag.builtin.IntTag;
import org.apache.logging.log4j.LogManager;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import micheal65536.fountain.registry.BedrockBlocks;
import micheal65536.fountain.registry.JavaBlocks;

public class BlockEntityTranslator
{
	@Nullable
	public static NbtMap translateBlockEntity(@NotNull JavaBlocks.BedrockMapping.BlockEntity blockEntityMapping, @Nullable BlockEntityInfo javaBlockEntityInfo)
	{
		switch (blockEntityMapping.type)
		{
			case "bed":
			{
				NbtMapBuilder builder = NbtMap.builder();
				builder.putString("id", "Bed");
				builder.putByte("color", (byte) switch (((JavaBlocks.BedrockMapping.BedBlockEntity) blockEntityMapping).color)
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
			case "flower_pot":
			{
				NbtMapBuilder builder = NbtMap.builder();
				builder.putString("id", "FlowerPot");
				NbtMap contents = ((JavaBlocks.BedrockMapping.FlowerPotBlockEntity) blockEntityMapping).contents;
				if (contents != null)
				{
					builder.putCompound("PlantBlock", contents);
				}
				return builder.build();
			}
			case "moving_block":
			{
				NbtMapBuilder builder = NbtMap.builder();

				builder.putString("id", "MovingBlock");

				if (javaBlockEntityInfo == null)
				{
					LogManager.getLogger().debug("Not sending moving block entity data until server provides data");
					return null;
				}
				CompoundTag javaNbt = javaBlockEntityInfo.getNbt();

				if (!javaNbt.contains("blockStateId"))
				{
					LogManager.getLogger().warn("Moving block entity data did not contain numeric block state ID");
					return null;
				}
				int javaBlockId = ((IntTag) javaNbt.get("blockStateId")).getValue();
				JavaBlocks.BedrockMapping bedrockMapping = JavaBlocks.getBedrockMapping(javaBlockId);
				if (bedrockMapping == null)
				{
					LogManager.getLogger().warn("Moving block entity contained block with no mapping {}", JavaBlocks.getName(javaBlockId));
					return null;
				}

				NbtMapBuilder movingBlockBuilder = NbtMap.builder();
				movingBlockBuilder.putString("name", BedrockBlocks.getName(bedrockMapping.id));
				movingBlockBuilder.putCompound("states", BedrockBlocks.getStateNbt(bedrockMapping.id));
				builder.putCompound("movingBlock", movingBlockBuilder.build());

				if (bedrockMapping.waterlogged)
				{
					NbtMapBuilder movingBlockExtraBuilder = NbtMap.builder();
					movingBlockExtraBuilder.putString("name", BedrockBlocks.getName(BedrockBlocks.WATER));
					movingBlockExtraBuilder.putCompound("states", BedrockBlocks.getStateNbt(BedrockBlocks.WATER));
					builder.putCompound("movingBlockExtra", movingBlockExtraBuilder.build());
				}

				if (bedrockMapping.blockEntity != null)
				{
					NbtMap blockEntityNbt = BlockEntityTranslator.translateBlockEntity(bedrockMapping.blockEntity, null);
					if (blockEntityNbt != null)
					{
						builder.putCompound("movingEntity", blockEntityNbt.toBuilder().putInt("x", javaBlockEntityInfo.getX()).putInt("y", javaBlockEntityInfo.getY()).putInt("z", javaBlockEntityInfo.getZ()).putBoolean("isMovable", false).build());
					}
				}

				if (!javaNbt.contains("basePos"))
				{
					LogManager.getLogger().warn("Moving block entity data did not contain piston base position");
					return null;
				}
				CompoundTag basePosTag = javaNbt.get("basePos");
				builder.putInt("pistonPosX", ((IntTag) basePosTag.get("x")).getValue());
				builder.putInt("pistonPosY", ((IntTag) basePosTag.get("y")).getValue());
				builder.putInt("pistonPosZ", ((IntTag) basePosTag.get("z")).getValue());

				return builder.build();
			}
			case "piston":
			{
				JavaBlocks.BedrockMapping.PistonBlockEntity pistonBlockEntity = (JavaBlocks.BedrockMapping.PistonBlockEntity) blockEntityMapping;
				NbtMapBuilder builder = NbtMap.builder();
				builder.putString("id", "PistonArm");
				builder.putByte("State", (byte) (pistonBlockEntity.extended ? 2 : 0));
				builder.putByte("NewState", (byte) (pistonBlockEntity.extended ? 2 : 0));
				builder.putFloat("Progress", pistonBlockEntity.extended ? 1.0f : 0.0f);
				builder.putFloat("LastProgress", pistonBlockEntity.extended ? 1.0f : 0.0f);
				builder.putBoolean("Sticky", pistonBlockEntity.sticky);
				return builder.build();
			}
			default:
				throw new AssertionError();
		}
	}
}