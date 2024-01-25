package micheal65536.fountain.utils.entities;

import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataType;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Set;

public class AgeableBedrockEntityInstance extends MobBedrockEntityInstance
{
	private final float adultSize;
	private final float babySize;

	public boolean baby = false;

	public AgeableBedrockEntityInstance()
	{
		this(1.0f, 0.55f);
	}

	public AgeableBedrockEntityInstance(float adultSize, float babySize)
	{
		this.adultSize = adultSize;
		this.babySize = babySize;
	}

	@Override
	protected void getEntityData(@NotNull Map<EntityDataType<?>, ?> entityData)
	{
		super.getEntityData(entityData);
		putEntityData(entityData, EntityDataTypes.SCALE, this.baby ? this.babySize : this.adultSize);
	}

	@Override
	protected void getEntityFlags(@NotNull Set<EntityFlag> entityFlags)
	{
		super.getEntityFlags(entityFlags);
		if (this.baby)
		{
			entityFlags.add(EntityFlag.BABY);
		}
	}
}