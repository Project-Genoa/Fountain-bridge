package micheal65536.fountain.utils.entities;

import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class MobBedrockEntityInstance extends LivingBedrockEntityInstance
{
	public boolean ai = true;

	@Override
	protected void getEntityFlags(@NotNull Set<EntityFlag> entityFlags)
	{
		super.getEntityFlags(entityFlags);
		if (!this.ai)
		{
			entityFlags.add(EntityFlag.NO_AI);
		}
	}
}