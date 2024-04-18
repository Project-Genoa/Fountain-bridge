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
import micheal65536.fountain.utils.entities.ParrotBedrockEntityInstance;
import micheal65536.fountain.utils.entities.ParrotJavaEntityInstance;
import micheal65536.fountain.utils.entities.PigBedrockEntityInstance;
import micheal65536.fountain.utils.entities.PigJavaEntityInstance;
import micheal65536.fountain.utils.entities.PolarBearBedrockEntityInstance;
import micheal65536.fountain.utils.entities.PolarBearJavaEntityInstance;
import micheal65536.fountain.utils.entities.RabbitBedrockEntityInstance;
import micheal65536.fountain.utils.entities.RabbitJavaEntityInstance;
import micheal65536.fountain.utils.entities.SheepBedrockEntityInstance;
import micheal65536.fountain.utils.entities.SheepJavaEntityInstance;
import micheal65536.fountain.utils.entities.SpiderBedrockEntityInstance;
import micheal65536.fountain.utils.entities.SpiderJavaEntityInstance;
import micheal65536.fountain.utils.entities.ZombieBedrockEntityInstance;
import micheal65536.fountain.utils.entities.ZombieJavaEntityInstance;

import java.util.Map;
import java.util.Set;

public class EntityTranslator
{
	@Nullable
	public static EntityManager.JavaEntityInstance createEntityInstance(@NotNull EntityType entityType, ObjectData data, @NotNull FabricRegistryManager fabricRegistryManager)
	{
		return switch (entityType)
		{
			// TODO: other players

			case CHICKEN -> new AgeableJavaEntityInstance<>("minecraft:chicken", new AgeableBedrockEntityInstance());
			case COW -> new AgeableJavaEntityInstance<>("minecraft:cow", new AgeableBedrockEntityInstance());
			case PIG -> new PigJavaEntityInstance<>("minecraft:pig", new PigBedrockEntityInstance());
			case SHEEP -> new SheepJavaEntityInstance<>("minecraft:sheep", new SheepBedrockEntityInstance());
			case RABBIT -> new RabbitJavaEntityInstance<>("minecraft:rabbit", new RabbitBedrockEntityInstance());
			case OCELOT -> new AgeableJavaEntityInstance<>("minecraft:ocelot", new AgeableBedrockEntityInstance());
			case PARROT -> new ParrotJavaEntityInstance<>("minecraft:parrot", new ParrotBedrockEntityInstance());
			case POLAR_BEAR -> new PolarBearJavaEntityInstance<>("minecraft:polar_bear", new PolarBearBedrockEntityInstance());

			case SQUID -> new MobJavaEntityInstance<>("minecraft:squid", new MobBedrockEntityInstance());    // TODO: squid rotation
			case GLOW_SQUID -> new MobJavaEntityInstance<>("genoa:glow_squid", new MobBedrockEntityInstance());    // TODO: change drops in Fabric mod to produce regular ink sac
			case SALMON -> new MobJavaEntityInstance<>("minecraft:salmon", new MobBedrockEntityInstance());
			case TROPICAL_FISH -> new MobJavaEntityInstance<>("minecraft:tropicalfish", new MobBedrockEntityInstance());    // TODO: variants

			case CREEPER -> new CreeperJavaEntityInstance<>("minecraft:creeper", new CreeperBedrockEntityInstance());
			case SKELETON -> new MobJavaEntityInstance<>("minecraft:skeleton", new MobBedrockEntityInstance());
			case SPIDER -> new SpiderJavaEntityInstance<>("minecraft:spider", new SpiderBedrockEntityInstance());
			case ZOMBIE -> new ZombieJavaEntityInstance<>("minecraft:zombie", new ZombieBedrockEntityInstance());

			// TODO: minecart

			case ITEM -> new ItemJavaEntityInstance(fabricRegistryManager);
			case FALLING_BLOCK ->
			{
				int javaBlockId = ((FallingBlockData) data).getId();
				JavaBlocks.BedrockMapping bedrockMapping = JavaBlocks.getBedrockMapping(javaBlockId);
				if (bedrockMapping == null)
				{
					LogManager.getLogger().warn("Falling block entity for block with no mapping {}", JavaBlocks.getName(javaBlockId));
					yield null;
				}
				yield new BaseJavaEntityInstanceWithSingleBedrockEntityInstance<>("minecraft:falling_block", new FallingBlockBedrockEntityInstance(bedrockMapping.id));
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

	@Nullable
	public static EntityManager.JavaEntityInstance createEntityInstance(@NotNull String customEntityName, ObjectData data, @NotNull FabricRegistryManager fabricRegistryManager)
	{
		// TODO: this list is incomplete, these are just the ones that have been implemented in the Fabric mod so far
		// TODO: it would be nice if this list could be combined with the list of "vanilla" mobs above
		return switch (customEntityName)
		{
			case "genoa:amber_chicken" -> new AgeableJavaEntityInstance<>("genoa:amber_chicken", new AgeableBedrockEntityInstance());
			case "genoa:bronzed_chicken" -> new AgeableJavaEntityInstance<>("genoa:bronzed_chicken", new AgeableBedrockEntityInstance());
			case "genoa:gold_crested_chicken" -> new AgeableJavaEntityInstance<>("genoa:gold_crested_chicken", new AgeableBedrockEntityInstance());
			case "genoa:midnight_chicken" -> new AgeableJavaEntityInstance<>("genoa:midnight_chicken", new AgeableBedrockEntityInstance());
			case "genoa:skewbald_chicken" -> new AgeableJavaEntityInstance<>("genoa:skewbald_chicken", new AgeableBedrockEntityInstance());
			case "genoa:stormy_chicken" -> new AgeableJavaEntityInstance<>("genoa:stormy_chicken", new AgeableBedrockEntityInstance());

			case "genoa:albino_cow" -> new AgeableJavaEntityInstance<>("genoa:albino_cow", new AgeableBedrockEntityInstance());
			case "genoa:ashen_cow" -> new AgeableJavaEntityInstance<>("genoa:ashen_cow", new AgeableBedrockEntityInstance());
			case "genoa:cookie_cow" -> new AgeableJavaEntityInstance<>("genoa:cookie_cow", new AgeableBedrockEntityInstance());
			case "genoa:cream_cow" -> new AgeableJavaEntityInstance<>("genoa:cream_cow", new AgeableBedrockEntityInstance());
			case "genoa:dairy_cow" -> new AgeableJavaEntityInstance<>("genoa:dairy_cow", new AgeableBedrockEntityInstance());
			case "genoa:pinto_cow" -> new AgeableJavaEntityInstance<>("genoa:pinto_cow", new AgeableBedrockEntityInstance());
			case "genoa:sunset_cow" -> new AgeableJavaEntityInstance<>("genoa:sunset_cow", new AgeableBedrockEntityInstance());

			case "genoa:mottled_pig" -> new PigJavaEntityInstance<>("genoa:mottled_pig", new PigBedrockEntityInstance());
			case "genoa:pale_pig" -> new PigJavaEntityInstance<>("genoa:pale_pig", new PigBedrockEntityInstance());
			case "genoa:piebald_pig" -> new PigJavaEntityInstance<>("genoa:piebald_pig", new PigBedrockEntityInstance());
			case "genoa:pink_footed_pig" -> new PigJavaEntityInstance<>("genoa:pink_footed_pig", new PigBedrockEntityInstance());
			case "genoa:sooty_pig" -> new PigJavaEntityInstance<>("genoa:sooty_pig", new PigBedrockEntityInstance());
			case "genoa:spotted_pig" -> new PigJavaEntityInstance<>("genoa:spotted_pig", new PigBedrockEntityInstance());

			case "genoa:bold_striped_rabbit" -> new RabbitJavaEntityInstance<>("genoa:bold_striped_rabbit", new RabbitBedrockEntityInstance());
			case "genoa:freckled_rabbit" -> new RabbitJavaEntityInstance<>("genoa:freckled_rabbit", new RabbitBedrockEntityInstance());
			case "genoa:harelequin_rabbit" -> new RabbitJavaEntityInstance<>("genoa:harelequin_rabbit", new RabbitBedrockEntityInstance());
			case "genoa:muddy_foot_rabbit" -> new RabbitJavaEntityInstance<>("genoa:muddy_foot_rabbit", new RabbitBedrockEntityInstance());
			case "genoa:vested_rabbit" -> new RabbitJavaEntityInstance<>("genoa:vested_rabbit", new RabbitBedrockEntityInstance());

			case "genoa:flecked_sheep" -> new SheepJavaEntityInstance<>("genoa:flecked_sheep", new SheepBedrockEntityInstance());
			case "genoa:inky_sheep" -> new SheepJavaEntityInstance<>("genoa:inky_sheep", new SheepBedrockEntityInstance());
			case "genoa:long_nosed_sheep" -> new SheepJavaEntityInstance<>("genoa:long_nosed_sheep", new SheepBedrockEntityInstance());
			case "genoa:patched_sheep" -> new SheepJavaEntityInstance<>("genoa:patched_sheep", new SheepBedrockEntityInstance());
			case "genoa:rainbow_sheep" -> new SheepJavaEntityInstance<>("genoa:rainbow_sheep", new SheepBedrockEntityInstance());
			case "genoa:rocky_sheep" -> new SheepJavaEntityInstance<>("genoa:rocky_sheep", new SheepBedrockEntityInstance());

			case "genoa:genoa_slime" -> new MobJavaEntityInstance<>("genoa:genoa_slime", new MobBedrockEntityInstance());
			case "genoa:genoa_slime_half" -> new MobJavaEntityInstance<>("genoa:genoa_slime_half", new MobBedrockEntityInstance());
			case "genoa:genoa_slime_quarter" -> new MobJavaEntityInstance<>("genoa:genoa_slime_quarter", new MobBedrockEntityInstance());
			case "genoa:tropical_slime" -> new MobJavaEntityInstance<>("genoa:tropical_slime", new MobBedrockEntityInstance());

			default -> null;
		};
	}
}