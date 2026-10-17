#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../.."
mkdir -p .samsung-baseline
baseline='.samsung-baseline/homenet-1.0.8-baseline.jar'
if [ ! -f "$baseline" ]; then
 curl --fail --location --silent --show-error 'https://raw.githubusercontent.com/mchoe5699/Home_system_mod/aadc550616374d070d2cf157b219516d3b4e32c9/qwertys_homenet-1.0.8.jar' -o "$baseline"
fi
printf '%s  %s\n' 'f523a392449c967bdca61081f9425ed0cb706f7d2d03096b4964d732f8695593' "$baseline" | sha256sum --check --status
if [ -f .samsung-baseline/homenet-1.0.8-dev.jar ]; then exit 0; fi
${GRADLE_COMMAND:-gradle} -b samsung.gradle createMcpToSrg
cache_root="${GRADLE_USER_HOME:-$HOME/.gradle}/caches/forge_gradle"
renamer="$cache_root/maven_downloader/net/minecraftforge/ForgeAutoRenamingTool/1.0.2/ForgeAutoRenamingTool-1.0.2-all.jar"
if [ ! -f "$renamer" ]; then
 renamer='.samsung-baseline/ForgeAutoRenamingTool-1.0.2-all.jar'
 curl --fail --location --silent --show-error 'https://maven.minecraftforge.net/net/minecraftforge/ForgeAutoRenamingTool/1.0.2/ForgeAutoRenamingTool-1.0.2-all.jar' -o "$renamer"
fi
printf '%s  %s\n' 'a95a95065dc0919d3b705f9ba50404fead73b39dc2c57ac023a950855f3f5387' "$renamer" | sha256sum --check --status
minecraft_srg="$cache_root/mcp_repo/net/minecraft/joined/1.20.1-20230612.114412/joined-1.20.1-20230612.114412-srg.jar"
java -jar "$renamer" --input "$baseline" --output .samsung-baseline/homenet-1.0.8-dev.jar --map build/samsung/createMcpToSrg/output.tsrg --reverse --lib "$minecraft_srg" > .samsung-baseline/remap.log 2>&1
