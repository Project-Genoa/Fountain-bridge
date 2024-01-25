package micheal65536.fountain.utils.entities;

import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class SpiderBedrockEntityInstance extends MobBedrockEntityInstance
{
	public boolean climbing = false;

	@Override
	protected void getEntityFlags(@NotNull Set<EntityFlag> entityFlags)
	{
		super.getEntityFlags(entityFlags);
		if (this.climbing)
		{
			entityFlags.add(EntityFlag.WALL_CLIMBING);
		}
	}
}