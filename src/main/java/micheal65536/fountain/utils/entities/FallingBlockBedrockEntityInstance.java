package micheal65536.fountain.utils.entities;

import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataType;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag;
import org.jetbrains.annotations.NotNull;

import micheal65536.fountain.Main;

import java.util.Map;
import java.util.Set;

public class FallingBlockBedrockEntityInstance extends BaseBedrockEntityInstance
{
	public final int blockId;

	public FallingBlockBedrockEntityInstance(int blockId)
	{
		this.blockId = blockId;
	}

	@Override
	protected void getEntityData(@NotNull Map<EntityDataType<?>, ?> entityData)
	{
		super.getEntityData(entityData);
		putEntityData(entityData, EntityDataTypes.BLOCK, Main.BLOCK_DEFINITION_REGISTRY.getDefinition(this.blockId));
	}

	@Override
	protected void getEntityFlags(@NotNull Set<EntityFlag> entityFlags)
	{
		super.getEntityFlags(entityFlags);
		if (!this.gravity)
		{
			entityFlags.add(EntityFlag.NO_AI);
		}
	}
}