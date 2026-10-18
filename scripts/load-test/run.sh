#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/../.."
export MSYS_NO_PATHCONV=1

memory="${MEMORY:-1024m}"
steady="${STEADY:-6m}"
pretouch="${PRETOUCH:-false}"
network="foody-demo_default"
container="foody-load-test-api"
image="foody-api:load-test"
k6_image="grafana/k6:2.3.0"
out="target/load-test/$(date +%Y%m%d-%H%M%S)"

host_path() {
	if command -v cygpath >/dev/null 2>&1; then cygpath -w "$1"; else echo "$1"; fi
}

if ! docker compose ps --status running --services | grep -qx postgres; then
	echo "The demo stack must be up and seeded first: docker compose up -d --build --wait"
	exit 1
fi

mkdir -p "$out"
echo "Building the production image..."
docker build -q -t "$image" . >/dev/null
docker compose stop api >/dev/null

java_options="-Xlog:gc:stdout:uptime"
if [ "$pretouch" = "true" ]; then
	java_options="$java_options -XX:InitialRAMPercentage=50 -XX:+AlwaysPreTouch"
fi
rate_limits='{"foody":{"rate-limit":{"login":{"capacity":10000000},"login-per-email":{"capacity":10000000},"register":{"capacity":10000000},"refresh":{"capacity":10000000},"place-order":{"capacity":10000000},"request-cancellation":{"capacity":10000000},"authenticated":{"capacity":10000000},"anonymous":{"capacity":10000000}}}}'

docker rm -f "$container" >/dev/null 2>&1 || true
docker run -d --name "$container" --network "$network" \
	--memory "$memory" --memory-swap "$memory" --read-only --tmpfs /tmp --security-opt no-new-privileges:true \
	-v foody-demo_jwt-keys:/keys:ro \
	-e SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/foody \
	-e SPRING_DATASOURCE_USERNAME=foody \
	-e SPRING_DATASOURCE_PASSWORD="${POSTGRES_PASSWORD:-foody-demo}" \
	-e FOODY_SECURITY_JWT_PRIVATEKEY=file:/keys/jwt-private.pem \
	-e FOODY_SECURITY_JWT_PUBLICKEY=file:/keys/jwt-public.pem \
	-e LOGGING_STRUCTURED_FORMAT_CONSOLE=ecs \
	-e SPRING_APPLICATION_JSON="$rate_limits" \
	-e JAVA_TOOL_OPTIONS="$java_options" \
	"$image" >/dev/null

echo "Waiting for the production API ($memory, pretouch=$pretouch)..."
for _ in $(seq 1 60); do
	if docker exec "$container" wget -qO- http://127.0.0.1:8080/actuator/health >/dev/null 2>&1; then break; fi
	sleep 2
done

: > "$out/memory.csv"
(
	while [ ! -f "$out/stop" ]; do
		docker stats --no-stream --format "$(date +%s),{{.Name}},{{.MemUsage}}" "$container" foody-demo-postgres-1 >> "$out/memory.csv" 2>/dev/null || true
		sleep 4
	done
) &
sampler=$!

docker run --rm --network "$network" \
	-v "$(host_path "$PWD/scripts/load-test"):/scripts:ro" \
	-v "$(host_path "$PWD/$out"):/out" \
	-e API="http://$container:8080" -e STEADY="$steady" \
	"$k6_image" run --quiet --summary-export /out/summary.json /scripts/load.js | tee "$out/k6.txt" || true

touch "$out/stop"
wait "$sampler"
oom=$(docker inspect "$container" --format '{{.State.OOMKilled}}')
docker logs "$container" 2>&1 | grep -E 'GC\(' > "$out/gc.log" || true
docker rm -f "$container" >/dev/null
docker compose start api >/dev/null

peak=$(awk -F, -v name="$container" '$2 == name { split($3, usage, " / "); value = usage[1]; unit = value; gsub(/[0-9.]/, "", unit); gsub(/[A-Za-z]/, "", value); if (unit == "GiB") value *= 1024; if (unit == "KiB") value /= 1024; if (value > max) max = value } END { printf "%.0f", max }' "$out/memory.csv")
committed=$(grep -oE '[0-9]+M->[0-9]+M\([0-9]+M\)' "$out/gc.log" | awk -F'[->()M]+' '$3 > max { max = $3 } END { print max + 0 }')

echo
echo "API memory limit:        $memory"
echo "Peak container memory:   ${peak} MiB"
echo "Peak committed heap:     ${committed} MiB"
echo "Killed for memory (OOM): $oom"
echo "Raw data:                $out"
