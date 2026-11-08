# LarpClient — Minecraft 1.21.11 / Fabric (Yarn) / Java 21

RIGHT SHIFT = menu. Modules: KillAura, ESP, Target ESP, HUD (watermark + module list), Theme tab.
Config is saved to `config/larpclient.json` when the menu closes.

## Build
1. Easiest: generate an empty 1.21.11 project at https://fabricmc.net/develop/template and copy `src/` over it
   (or copy the Gradle wrapper from that template into this folder).
2. Needs JDK 21.
3. `./gradlew build`  ->  `build/libs/larpclient-1.0.0.jar`
4. Dev run: `./gradlew runClient`
5. Install: drop the jar into `.minecraft/mods` together with Fabric API 0.141.3+1.21.11 (or newer for 1.21.11).

If a version in gradle.properties doesn't resolve, take the current ones from https://fabricmc.net/develop.

## Build on GitHub (no local setup)
Upload this folder to a new GitHub repo (keep the `.github` folder), open the Actions tab,
wait for the "build" run, then download `larpclient-jar` from Artifacts.
