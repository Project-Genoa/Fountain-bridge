package micheal65536.fountain.utils.entities;

import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataType;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class SlimeBedrockEntityInstance extends MobBedrockEntityInstance
{
	public int size = 1;

	@Override
	protected void getEntityData(@NotNull Map<EntityDataType<?>, ?> entityData)
	{
		super.getEntityData(entityData);
		putEntityData(entityData, EntityDataTypes.SCALE, (float) this.size + 0.1f);
	}
}