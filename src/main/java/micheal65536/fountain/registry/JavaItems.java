package micheal65536.fountain.registry;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import micheal65536.fountain.DataFile;
import micheal65536.fountain.utils.FabricRegistryManager;

import java.util.HashMap;

public class JavaItems
{
	private static final HashMap<Integer, String> map = new HashMap<>();

	private static final HashMap<Integer, BedrockMapping> bedrockMap = new HashMap<>();
	private static final HashMap<String, BedrockMapping> bedrockNonVanillaMap = new HashMap<>();
	private static final HashMap<ItemNameAndAux, Integer> javaIdMap = new HashMap<>();
	private static final HashMap<ItemNameAndAux, String> javaIdNonVanillaMap = new HashMap<>();

	static
	{
		DataFile.load("registry/items_java.json", root ->
		{
			for (JsonElement element : root.getAsJsonArray())
			{
				int id = element.getAsJsonObject().get("id").getAsInt();
				String name = element.getAsJsonObject().get("name").getAsString();
				if (map.put(id, name) != null)
				{
					LogManager.getLogger().warn("Duplicate Java item ID {}", id);
				}

				JsonObject bedrockMapping = element.getAsJsonObject().get("bedrock").getAsJsonObject();
				if (bedrockMapping.has("ignore") && bedrockMapping.get("ignore").getAsBoolean())
				{
					continue;
				}
				String bedrockName = bedrockMapping.get("name").getAsString();
				int bedrockAux = bedrockMapping.has("aux") ? bedrockMapping.get("aux").getAsInt() : 0;
				boolean bedrockToolWear = bedrockMapping.has("wear") ? bedrockMapping.get("wear").getAsBoolean() : false;
				int bedrockId = BedrockItems.getId(bedrockName);
				if (bedrockId == 0)
				{
					LogManager.getLogger().warn("Cannot find Bedrock item for Java item {}", name);
				}
				else
				{
					bedrockMap.put(id, new BedrockMapping(bedrockId, bedrockAux, bedrockToolWear));
					if (javaIdMap.put(new ItemNameAndAux(bedrockName, bedrockAux), id) != null)
					{
						LogManager.getLogger().warn("Duplicate Bedrock item mapping {} {}", bedrockName, bedrockAux);
					}
				}
			}
		});

		DataFile.load("registry/items_java_nonvanilla.json", root ->
		{
			for (JsonElement element : root.getAsJsonArray())
			{
				String name = element.getAsJsonObject().get("name").getAsString();

				JsonObject bedrockMapping = element.getAsJsonObject().get("bedrock").getAsJsonObject();
				if (bedrockMapping.has("ignore") && bedrockMapping.get("ignore").getAsBoolean())
				{
					continue;
				}
				String bedrockName = bedrockMapping.get("name").getAsString();
				int bedrockAux = bedrockMapping.has("aux") ? bedrockMapping.get("aux").getAsInt() : 0;
				boolean bedrockToolWear = bedrockMapping.has("wear") ? bedrockMapping.get("wear").getAsBoolean() : false;
				int bedrockId = BedrockItems.getId(bedrockName);
				if (bedrockId == 0)
				{
					LogManager.getLogger().warn("Cannot find Bedrock item for Java item {}", name);
				}
				else
				{
					bedrockNonVanillaMap.put(name, new BedrockMapping(bedrockId, bedrockAux, bedrockToolWear));
					if (javaIdNonVanillaMap.put(new ItemNameAndAux(bedrockName, bedrockAux), name) != null)
					{
						LogManager.getLogger().warn("Duplicate Bedrock item mapping {} {}", bedrockName, bedrockAux);
					}
				}
			}
		});
	}

	@Deprecated
	@Nullable
	public static String getName(int id)
	{
		return getName(id, null);
	}

	@Deprecated
	@Nullable
	public static BedrockMapping getBedrockMapping(int javaId)
	{
		return getBedrockMapping(javaId, null);
	}

	@Deprecated
	public static int getJavaId(@NotNull String bedrockName, int bedrockAux)
	{
		return getJavaId(bedrockName, bedrockAux, null);
	}

	@Nullable
	public static String getName(int id, @Nullable FabricRegistryManager fabricRegistryManager)
	{
		String name = map.getOrDefault(id, null);
		if (name == null && fabricRegistryManager != null)
		{
			name = fabricRegistryManager.getItemName(id);
		}
		return name;
	}

	@Nullable
	public static BedrockMapping getBedrockMapping(int javaId, @Nullable FabricRegistryManager fabricRegistryManager)
	{
		BedrockMapping bedrockMapping = bedrockMap.getOrDefault(javaId, null);
		if (bedrockMapping == null && fabricRegistryManager != null)
		{
			String fabricName = fabricRegistryManager.getItemName(javaId);
			if (fabricName != null)
			{
				bedrockMapping = bedrockNonVanillaMap.getOrDefault(fabricName, null);
			}
		}
		return bedrockMapping;
	}

	public static int getJavaId(@NotNull String bedrockName, int bedrockAux, @Nullable FabricRegistryManager fabricRegistryManager)
	{
		ItemNameAndAux itemNameAndAux = new ItemNameAndAux(bedrockName, bedrockAux);
		int javaId = javaIdMap.getOrDefault(itemNameAndAux, -1);
		if (javaId == -1 && fabricRegistryManager != null)
		{
			String fabricName = javaIdNonVanillaMap.getOrDefault(itemNameAndAux, null);
			if (fabricName != null)
			{
				javaId = fabricRegistryManager.getItemId(fabricName);
			}
		}
		return javaId;
	}

	private static final class ItemNameAndAux
	{
		@NotNull
		public final String name;
		public final int aux;

		public ItemNameAndAux(@NotNull String name, int aux)
		{
			this.name = name;
			this.aux = aux;
		}

		@Override
		public boolean equals(Object obj)
		{
			if (obj instanceof ItemNameAndAux)
			{
				return this.name.equals(((ItemNameAndAux) obj).name) && this.aux == ((ItemNameAndAux) obj).aux;
			}
			else
			{
				return false;
			}
		}

		@Override
		public int hashCode()
		{
			return this.name.hashCode() ^ this.aux;
		}
	}

	public static final class BedrockMapping
	{
		public final int id;
		public final int aux;
		public final boolean toolWear;

		public BedrockMapping(int id, int aux, boolean toolWear)
		{
			this.id = id;
			this.aux = aux;
			this.toolWear = toolWear;
		}
	}
}