package micheal65536.fountain.utils.entities;

import org.cloudburstmc.protocol.bedrock.data.AttributeData;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;
import java.util.Set;

public class LivingBedrockEntityInstance extends BaseBedrockEntityInstance
{
	public float health = 1.0f;
	public float maxHealth = 20.0f;

	public float movementSpeed = 0.1f;

	public boolean usingItem = false;

	@Override
	protected void getEntityFlags(@NotNull Set<EntityFlag> entityFlags)
	{
		super.getEntityFlags(entityFlags);
		if (this.usingItem)
		{
			// TODO: doesn't work
			entityFlags.add(EntityFlag.USING_ITEM);
		}
	}

	@Override
	protected void getEntityAttributes(@NotNull LinkedList<AttributeData> entityAttributes)
	{
		super.getEntityAttributes(entityAttributes);
		entityAttributes.add(new AttributeData("minecraft:health", 0.0f, this.maxHealth, this.health > 0.0f && this.health < 1.0f ? 1.0f : this.health));
		entityAttributes.add(new AttributeData("minecraft:movement", 0.0f, 1024.0f, this.movementSpeed));
	}
}