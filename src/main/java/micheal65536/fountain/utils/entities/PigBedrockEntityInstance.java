package micheal65536.fountain.utils.entities;

import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class PigBedrockEntityInstance extends AgeableBedrockEntityInstance
{
	public boolean saddled = false;

	@Override
	protected void getEntityFlags(@NotNull Set<EntityFlag> entityFlags)
	{
		super.getEntityFlags(entityFlags);
		if (this.saddled)
		{
			entityFlags.add(EntityFlag.SADDLED);
		}
	}
}