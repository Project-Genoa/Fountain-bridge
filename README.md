# Fountain

Bridge server to connect Minecraft Earth to a Minecraft Java edition server, inspired by Geyser.

## Building

This project uses the Maven build system. You will need to have Maven installed (or use an IDE that includes it).

Fountain uses a fork of the Cloudburst Protocol library that includes support for the Minecraft Earth protocol. You must first download this from https://github.com/Project-Genoa/Protocol and build and install it to your local Maven repository by running `./gradlew publishToMavenLocal`.

To build Fountain, run `mvn package` from the project root directory. The build output will be in `/target`.

## Usage

Using Fountain requires a Minecraft 1.20.4 server with the Fountain Fabric mod installed. Follow the instructions here to install the Fountain Fabric mod: https://github.com/Project-Genoa/Fountain-fabric.

Your Minecraft server must be running in offline mode and spawn protection must be disabled. Set `online-mode=false` and `spawn-protection=0` in the `server.properties` file.

Once your Minecraft Java edition server is set up and running, run the `fountain-<version>-jar-with-dependencies.jar` JAR file from the build output directory (see above). The `/data` directory from the source tree must be present when Fountain is run - run the JAR file from the project root directory or copy the JAR file and the `data` directory into the same directory.

Currently, you will need to give your player operator/admin privileges on the Minecraft server in order for Fountain to function correctly. Use `/op <player name>` in the server console after your player has connected for the first time.

## Tips

Set your game mode to creative if you are playing on a buildplate, and survival if you are playing an adventure. (The Fountain Fabric mod will take care of applying the appropriate Earth-specific behavior in each mode, such as allowing you to pick up blocks into your inventory in creative mode.)

Disable daylight cycle and set the time to noon with `/gamerule doDaylightCycle false` and `/time set noon` (or `/time set midnight`) in the server console.

Disable natural mob spawning with `/gamerule doMobSpawning false` in the server console. Use `/kill @e[type=!player]` to get rid of naturally-spawned mobs that appear outside of the buildplate area.

Each time you join the server you will have an empty inventory and hotbar (inventory is not synchronised with the Minecraft Earth API server). Use `/give` in the server console to give yourself items/blocks.

Minecraft sea level is at Y 63. In order to have the surface level of your buildplate appear at ground level in AR, set the Y offset in your API server buildplate response to 63.
