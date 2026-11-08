# Custom NPCs (Unofficial): NeoForge 1.21.11 port

A port of [CustomNPCs-Unofficial](https://github.com/BetaZavr/CustomNPCs-Unofficial)
(1.12.2, by Noppes / Goodbird / BetaZavr) to **NeoForge for Minecraft 1.21.11**.

Status: **stage 1 of 5**. The project scaffold loads; there is no gameplay content yet.
See [docs/PORTING_PLAN.md](docs/PORTING_PLAN.md).

## Build

Requires JDK 21.

```sh
./gradlew build        # jar in build/libs/
./gradlew runClient
./gradlew runServer
```

## License

CC BY-NC 3.0, the same as upstream. Gradle wrapper and build template come from the
NeoForged MDK (MIT, see `MDK_TEMPLATE_LICENSE.txt`).
