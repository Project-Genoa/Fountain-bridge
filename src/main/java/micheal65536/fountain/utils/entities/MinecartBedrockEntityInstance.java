package micheal65536.fountain.utils.entities;

import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataType;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class MinecartBedrockEntityInstance extends BaseBedrockEntityInstance
{
	public int hurtDirection;
	public int hurtTicks;

	@Override
	protected void getEntityData(@NotNull Map<EntityDataType<?>, ?> entityData)
	{
		super.getEntityData(entityData);
		putEntityData(entityData, EntityDataTypes.STRUCTURAL_INTEGRITY, 0);
		putEntityData(entityData, EntityDataTypes.HURT_DIRECTION, this.hurtDirection);
		putEntityData(entityData, EntityDataTypes.HURT_TICKS, this.hurtTicks);
	}
}