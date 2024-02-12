package micheal65536.fountain.registry;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import micheal65536.fountain.DataFile;

import java.util.HashMap;
import java.util.Map;

public class BedrockBlocks
{
	private static final HashMap<BlockNameAndState, Integer> stateToIdMap = new HashMap<>();
	private static final HashMap<Integer, BlockNameAndState> idToStateMap = new HashMap<>();

	public static final int AIR;
	public static final int WATER;

	static
	{
		DataFile.load("registry/blocks_bedrock.json", root ->
		{
			for (JsonElement element : root.getAsJsonArray())
			{
				int id = element.getAsJsonObject().get("id").getAsInt();
				String name = element.getAsJsonObject().get("name").getAsString();
				HashMap<String, Object> state = new HashMap<>();
				JsonObject stateObject = element.getAsJsonObject().get("state").getAsJsonObject();
				for (Map.Entry<String, JsonElement> entry : stateObject.entrySet())
				{
					JsonElement stateElement = entry.getValue();
					if (stateElement.getAsJsonPrimitive().isString())
					{
						state.put(entry.getKey(), stateElement.getAsString());
					}
					else
					{
						state.put(entry.getKey(), stateElement.getAsInt());
					}
				}
				BlockNameAndState blockNameAndState = new BlockNameAndState(name, state);
				if (stateToIdMap.put(blockNameAndState, id) != null)
				{
					LogManager.getLogger().warn("Duplicate Bedrock block name/state {}", name);
				}
				if (idToStateMap.put(id, blockNameAndState) != null)
				{
					LogManager.getLogger().warn("Duplicate Bedrock block ID {}", id);
				}
			}
		});

		AIR = BedrockBlocks.getId("minecraft:air", new HashMap<>());
		HashMap<String, Object> hashMap = new HashMap<>();
		hashMap.put("liquid_depth", 0);
		WATER = BedrockBlocks.getId("minecraft:water", hashMap);
	}

	public static int getId(@NotNull String name, @NotNull HashMap<String, Object> state)
	{
		BlockNameAndState blockNameAndState = new BlockNameAndState(name, state);
		return stateToIdMap.getOrDefault(blockNameAndState, -1);
	}

	@Nullable
	public static String getName(int id)
	{
		BlockNameAndState blockNameAndState = idToStateMap.getOrDefault(id, null);
		return blockNameAndState != null ? blockNameAndState.name : null;
	}

	@Nullable
	public static HashMap<String, Object> getState(int id)
	{
		BlockNameAndState blockNameAndState = idToStateMap.getOrDefault(id, null);
		if (blockNameAndState == null)
		{
			return null;
		}
		HashMap<String, Object> state = new HashMap<>();
		blockNameAndState.state.forEach((key, value) -> state.put(key, value));
		return state;
	}

	@Nullable
	public static NbtMap getStateNbt(int id)
	{
		BlockNameAndState blockNameAndState = idToStateMap.getOrDefault(id, null);
		if (blockNameAndState == null)
		{
			return null;
		}
		NbtMapBuilder builder = NbtMap.builder();
		blockNameAndState.state.forEach((key, value) ->
		{
			if (value instanceof String)
			{
				builder.putString(key, (String) value);
			}
			else if (value instanceof Integer)
			{
				builder.putInt(key, (int) value);
			}
			else
			{
				throw new AssertionError();
			}
		});
		return builder.build();
	}

	private static final class BlockNameAndState
	{
		@NotNull
		public final String name;
		@NotNull
		public final HashMap<String, Object> state;

		public BlockNameAndState(@NotNull String name, @NotNull HashMap<String, Object> state)
		{
			this.name = name;
			this.state = state;
		}

		@Override
		public boolean equals(Object obj)
		{
			if (obj instanceof BlockNameAndState)
			{
				return this.name.equals(((BlockNameAndState) obj).name) && this.state.equals(((BlockNameAndState) obj).state);
			}
			else
			{
				return false;
			}
		}

		@Override
		public int hashCode()
		{
			return this.name.hashCode() ^ this.state.hashCode();
		}
	}
}