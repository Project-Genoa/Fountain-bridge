package micheal65536.fountain.utils.entities;

import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataType;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class ParrotBedrockEntityInstance extends AgeableBedrockEntityInstance
{
	public int variant = 0;

	@Override
	protected void getEntityData(@NotNull Map<EntityDataType<?>, ?> entityData)
	{
		super.getEntityData(entityData);
		putEntityData(entityData, EntityDataTypes.VARIANT, this.variant);
	}
}