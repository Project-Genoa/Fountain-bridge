package micheal65536.fountain.utils.entities;

import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataType;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Set;

public class WoolyCowBedrockEntityInstance extends AgeableBedrockEntityInstance
{
	public boolean sheared = false;

	@Override
	protected void getEntityFlags(@NotNull Set<EntityFlag> entityFlags)
	{
		super.getEntityFlags(entityFlags);
		if (this.sheared)
		{
			entityFlags.add(EntityFlag.SHEARED);
		}
	}
}