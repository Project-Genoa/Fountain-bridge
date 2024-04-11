package micheal65536.fountain.registry;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;

import micheal65536.fountain.DataFile;

import java.util.LinkedList;

public class EarthEntitiesRegistry
{
	private static final LinkedList<EntityInfo> entities = new LinkedList<>();

	static
	{
		DataFile.load("registry/entities.json", root ->
		{
			for (JsonElement element : root.getAsJsonArray())
			{
				JsonObject object = element.getAsJsonObject();
				EntityInfo entityInfo = new EntityInfo(
						object.get("id").getAsString(),
						object.get("bid").getAsString(),
						object.get("rid").getAsInt(),
						object.get("summonable").getAsBoolean(),
						object.get("hasSpawnEgg").getAsBoolean()
				);
				entities.add(entityInfo);
			}
		});
	}

	@NotNull
	public static EntityInfo[] getEntities()
	{
		return entities.toArray(EntityInfo[]::new);
	}

	public static final class EntityInfo
	{
		@NotNull
		public final String id;
		@NotNull
		public final String bid;
		public final int rid;
		public final boolean summonable;
		public final boolean hasSpawnEgg;

		private EntityInfo(@NotNull String id, @NotNull String bid, int rid, boolean summonable, boolean hasSpawnEgg)
		{
			this.id = id;
			this.bid = bid;
			this.rid = rid;
			this.summonable = summonable;
			this.hasSpawnEgg = hasSpawnEgg;
		}
	}
}