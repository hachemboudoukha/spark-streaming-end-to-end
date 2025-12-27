#!/bin/bash

echo "=== [Cleanup] Cleaning environment before execution ==="

# 1. Stop and remove containers
echo "> Stopping and removing containers..."
docker-compose down -v --remove-orphans

# 2. Clean Spark Checkpoints
echo "> Removing Spark checkpoint directories..."
rm -rf checkpoint/
rm -rf target/
rm -rf project/target/
rm -rf project/project/

# 3. Clean Output data
echo "> Removing previous batch results (output/)..."
rm -rf output/*

# 4. Clean local .sbt artifacts (optional, but good for a fresh start)
# rm -rf ~/.sbt/boot/
# rm -rf ~/.ivy2/cache/

echo "=== [Cleanup] DONE! Environment is ready for a fresh start. ==="
