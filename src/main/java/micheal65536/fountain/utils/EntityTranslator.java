package micheal65536.fountain.utils;

import com.github.steveice10.mc.protocol.data.game.entity.object.FallingBlockData;
import com.github.steveice10.mc.protocol.data.game.entity.object.ObjectData;
import com.github.steveice10.mc.protocol.data.game.entity.type.EntityType;
import org.apache.logging.log4j.LogManager;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataType;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import micheal65536.fountain.registry.JavaBlocks;
import micheal65536.fountain.utils.entities.AgeableBedrockEntityInstance;
import micheal65536.fountain.utils.entities.AgeableJavaEntityInstance;
import micheal65536.fountain.utils.entities.BaseBedrockEntityInstance;
import micheal65536.fountain.utils.entities.BaseJavaEntityInstanceWithSingleBedrockEntityInstance;
import micheal65536.fountain.utils.entities.CreeperBedrockEntityInstance;
import micheal65536.fountain.utils.entities.CreeperJavaEntityInstance;
import micheal65536.fountain.utils.entities.FallingBlockBedrockEntityInstance;
import micheal65536.fountain.utils.entities.ItemJavaEntityInstance;
import micheal65536.fountain.utils.entities.MobBedrockEntityInstance;
import micheal65536.fountain.utils.entities.MobJavaEntityInstance;
import micheal65536.fountain.utils.entities.PufferfishBedrockEntityInstance;
import micheal65536.fountain.utils.entities.PufferfishJavaEntityInstance;
import micheal65536.fountain.utils.entities.RabbitBedrockEntityInstance;
import micheal65536.fountain.utils.entities.RabbitJavaEntityInstance;
import micheal65536.fountain.utils.entities.SheepBedrockEntityInstance;
import micheal65536.fountain.utils.entities.SheepJavaEntityInstance;
import micheal65536.fountain.utils.entities.SlimeBedrockEntityInstance;
import micheal65536.fountain.utils.entities.SlimeJavaEntityInstance;
import micheal65536.fountain.utils.entities.SpiderBedrockEntityInstance;
import micheal65536.fountain.utils.entities.SpiderJavaEntityInstance;
import micheal65536.fountain.utils.entities.ZombieBedrockEntityInstance;
import micheal65536.fountain.utils.entities.ZombieJavaEntityInstance;

import java.util.Map;
import java.util.Set;

public class EntityTranslator
{
	@Nullable
	public static EntityManager.JavaEntityInstance createEntityInstance(@NotNull EntityType entityType, ObjectData data)
	{
		return switch (entityType)
		{
			// TODO: other players

			case CHICKEN -> new AgeableJavaEntityInstance<>("minecraft:chicken", new AgeableBedrockEntityInstance());
			case COW -> new AgeableJavaEntityInstance<>("minecraft:cow", new AgeableBedrockEntityInstance());
			case PIG -> new AgeableJavaEntityInstance<>("minecraft:pig", new AgeableBedrockEntityInstance());
			case SHEEP -> new SheepJavaEntityInstance<>("minecraft:sheep", new SheepBedrockEntityInstance());
			case RABBIT -> new RabbitJavaEntityInstance<>("minecraft:rabbit", new RabbitBedrockEntityInstance());    // TODO: variants don't work

			case SQUID -> new MobJavaEntityInstance<>("minecraft:squid", new MobBedrockEntityInstance());    // TODO: squid rotation
			case COD -> new MobJavaEntityInstance<>("minecraft:cod", new MobBedrockEntityInstance());    // TODO: not visible
			case SALMON -> new MobJavaEntityInstance<>("minecraft:salmon", new MobBedrockEntityInstance());
			case TROPICAL_FISH -> new MobJavaEntityInstance<>("minecraft:tropicalfish", new MobBedrockEntityInstance());    // TODO: variants
			case PUFFERFISH -> new PufferfishJavaEntityInstance<>("minecraft:pufferfish", new PufferfishBedrockEntityInstance());    // TODO: not visible

			case CAVE_SPIDER -> new SpiderJavaEntityInstance<>("minecraft:cave_spider", new SpiderBedrockEntityInstance());    // TODO: not visible
			case CREEPER -> new CreeperJavaEntityInstance<>("minecraft:creeper", new CreeperBedrockEntityInstance());
			case DROWNED -> new ZombieJavaEntityInstance<>("minecraft:drowned", new ZombieBedrockEntityInstance());    // TODO: not visible
			case HUSK -> new ZombieJavaEntityInstance<>("minecraft:husk", new ZombieBedrockEntityInstance());    // TODO: not visible
			case SKELETON -> new MobJavaEntityInstance<>("minecraft:skeleton", new MobBedrockEntityInstance());
			case SLIME -> new SlimeJavaEntityInstance<>("minecraft:slime", new SlimeBedrockEntityInstance());    // TODO: not visible
			case SPIDER -> new SpiderJavaEntityInstance<>("minecraft:spider", new SpiderBedrockEntityInstance());
			case STRAY -> new MobJavaEntityInstance<>("minecraft:stray", new MobBedrockEntityInstance());    // TODO: not visible
			case ZOMBIE -> new ZombieJavaEntityInstance<>("minecraft:zombie", new ZombieBedrockEntityInstance());
			case ZOMBIE_VILLAGER -> new ZombieJavaEntityInstance<>("minecraft:zombie_villager", new ZombieBedrockEntityInstance());    // TODO: not visible

			case VILLAGER -> new AgeableJavaEntityInstance<>("minecraft:villager", new AgeableBedrockEntityInstance());    // TODO: not visible

			// TODO: minecart

			case ITEM -> new ItemJavaEntityInstance();
			case FALLING_BLOCK ->
			{
				int javaBlockId = ((FallingBlockData) data).getId();
				int bedrockBlockId = JavaBlocks.getBedrockId(javaBlockId);
				if (bedrockBlockId == -1)
				{
					LogManager.getLogger().warn("Falling block entity for block with no mapping {}", JavaBlocks.getName(javaBlockId));
					yield null;
				}
				yield new BaseJavaEntityInstanceWithSingleBedrockEntityInstance<>("minecraft:falling_block", new FallingBlockBedrockEntityInstance(bedrockBlockId));
			}
			case ARROW -> new BaseJavaEntityInstanceWithSingleBedrockEntityInstance<>("minecraft:arrow", new BaseBedrockEntityInstance());    // TODO: does Minecraft Earth have potion and spectral arrows?
			case EGG -> new BaseJavaEntityInstanceWithSingleBedrockEntityInstance<>("minecraft:egg", new BaseBedrockEntityInstance());    // TODO: movement is broken
			case SNOWBALL -> new BaseJavaEntityInstanceWithSingleBedrockEntityInstance<>("minecraft:snowball", new BaseBedrockEntityInstance());    // TODO: movement is broken
			case TNT -> new BaseJavaEntityInstanceWithSingleBedrockEntityInstance<>("minecraft:tnt", new BaseBedrockEntityInstance()    // TODO: needs tick for fuse, falls through ground???
			{
				@Override
				protected void getEntityData(@NotNull Map<EntityDataType<?>, ?> entityData)
				{
					super.getEntityData(entityData);
					putEntityData(entityData, EntityDataTypes.FUSE_TIME, 60);
				}

				@Override
				protected void getEntityFlags(@NotNull Set<EntityFlag> entityFlags)
				{
					super.getEntityFlags(entityFlags);
					entityFlags.add(EntityFlag.IGNITED);
				}
			});

			default -> null;
		};
	}
}