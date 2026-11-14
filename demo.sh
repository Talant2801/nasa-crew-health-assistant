#!/usr/bin/env bash
# Demo flow for the Onboard Crew Health Assistant. Start the app first: ./mvnw spring-boot:run
set -euo pipefail
BASE="${BASE:-http://localhost:8080}"
pp() { if command -v jq >/dev/null; then jq "$@"; else cat; echo; fi; }

echo "== 0. Reset to defaults";            curl -s -X POST "$BASE/api/scenarios/reset" -o /dev/null -w "%{http_code}\n"
echo "== 1. Crew (expect 3 astronauts)";   curl -s "$BASE/api/crew" | pp .
echo "== 2. Mission (ISS)";                curl -s "$BASE/api/mission" | pp .
echo "== 3. Trigger solar storm (replayed NASA DONKI SEP event)"
curl -s -X POST "$BASE/api/scenarios/solar-storm" -o /dev/null -w "%{http_code}\n"
echo "== 4. Alerts (expect crew:RADIATION RED with spe-shelter card)"
curl -s "$BASE/api/alerts" | pp '.[] | {id, severity, card: .actionCard.id, escalationText}'
echo "== 5. Switch mission context to MARS"
curl -s -X POST "$BASE/api/mission/context" -H 'Content-Type: application/json' -d '{"env":"MARS"}' | pp .
echo "== 6. Alerts (expect Mars escalation text)"
curl -s "$BASE/api/alerts" | pp '.[] | {id, severity, escalationText}'
echo "== 7. Radiation module (expect projectedDoseMSv = 1620)"
curl -s "$BASE/api/crew/ortiz/modules" | pp '.[] | select(.bodySystem=="RADIATION")'
echo "== 8. Sleep-deficit scenario for Cmdr. Ortiz"
curl -s -X POST "$BASE/api/scenarios/sleep-deficit" -o /dev/null -w "%{http_code}\n"
curl -s "$BASE/api/alerts?crewId=ortiz" | pp '.[] | {id, severity, summary}'
