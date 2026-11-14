# Onboard Crew Health Assistant

NASA Space Apps Challenge 2026 prototype. An onboard health assistant for astronauts that compares each crew member
against their **own** pre-flight baseline, shows a GREEN / AMBER / RED status per body system, and turns every problem
into a concrete, rule-based action card. Escalation wording adapts to the mission context (ISS, lunar surface, Mars
transit), because "call the flight surgeon" means something very different with a 24-minute round-trip delay.
A whole-crew radiation module combines published dose rates with a replayed NASA DONKI solar particle event.
There is no LLM anywhere: every status and recommendation comes from transparent rules and thresholds kept in JSON data files.

> **Disclaimer:** Prototype with simulated crew data. Decision-support tool, not a diagnosis. Not a medical device.

## Run

Requires Java 21+.

```bash
./mvnw spring-boot:run          # http://localhost:8080 (override with PORT=xxxx)
./mvnw test                     # unit + API flow tests
./demo.sh                       # curl walkthrough of the demo (needs the app running; jq optional)
```

Docker:

```bash
docker build -t crew-health .
docker run -p 8080:8080 -e PORT=8080 crew-health
```

## How it works

| Rule | Logic |
|---|---|
| Indicator RED | latest value breaches the absolute limit (`absoluteLimit` / `limitDirection`) |
| Indicator AMBER | latest value outside personal baseline ± `bandPercent`, **or** the last 3 day-over-day changes all go in `badDirection` |
| Module / overall status | worst indicator in the body system / worst module (incl. RADIATION) |
| Radiation | RED if a solar particle event is active (→ `spe-shelter`); RED if current dose ≥ 600 mSv; AMBER if projected mission dose ≥ 600 mSv (→ `dose-projection`) |
| Radiation model | Simplified: mission days × published dose rate for the environment |
| Escalation text | mission-specific text when severity is RED or the card is `URGENT`, otherwise "Monitor and re-measure tomorrow." |

Thresholds, cards and mission profiles live in `src/main/resources/data/`:
`indicators.json`, `action-cards.json`, `missions.json`, `donki-sep.json`.
Seed data (3 fictional astronauts, 30 days of readings) is regenerated at startup with fixed random seed 42.
All state is in memory; `POST /api/scenarios/reset` restores the defaults.

## API

All endpoints are under `/api`, JSON, camelCase. CORS is open on `/api/**`.

| Method | Path | Description |
|---|---|---|
| GET | `/api/health` | `{ "status": "UP" }` |
| GET | `/api/mission` | Mission context: env, label, missionDay, missionLengthDays, commsDelaySeconds, escalation, doseRateMSvPerDay, doseSource, spaceWeather |
| POST | `/api/mission/context` | Body `{ "env": "ISS" \| "MOON" \| "MARS" }` → same as GET (missionDay kept) |
| GET | `/api/crew` | `[ { id, name, role, overallStatus, topIssue } ]` |
| GET | `/api/crew/{id}/modules` | Body-system modules with indicators (value, baseline, status, trend, summary, last7) + crew-wide RADIATION module |
| GET | `/api/crew/{id}/readings?indicator=HR_REST&days=30` | `{ indicator, unit, baseline, bandLow, bandHigh, absoluteLimit, series }` |
| GET | `/api/alerts?crewId={id}` | Derived alerts (RED first), each with full action card and escalation text. `crewId` optional; crew-wide alerts always included |
| POST | `/api/alerts/{id}/ack` | Acknowledge alert (ids like `navarro:HRV`, `crew:RADIATION`) → 204 |
| POST | `/api/scenarios/{name}` | `solar-storm`, `sleep-deficit`, `reset` → 204; unknown → 404 |

Crew ids: `ortiz`, `navarro`, `sato`. Unknown astronaut / indicator / scenario → 404 with `{ status, error, message }`.

## Data & Licenses

**Data sources**

- **NASA DONKI** (Space Weather Database Of Notifications, Knowledge, Information), SEP endpoint, via api.nasa.gov — US Government work, public domain.
  `data/donki-sep.json` is a cached response of
  `GET https://api.nasa.gov/DONKI/SEP?startDate=2017-09-01&endDate=2017-09-30` (September 2017 events).
  The app replays the first event; it **never** calls the live API at runtime.
- **NASA-STD-3001 Volume 1** (NASA Space Flight Human-System Standard) — career effective dose limit 600 mSv [V1 4030] and short-term/SPE limit 250 mSv [V1 4031].
- **Dose rates** (`data/missions.json`):
  - ISS: NASA, ISS effective dose ~0.2–0.5 mSv/day (PMC10919966)
  - Lunar surface: Chang'e 4 Lunar Lander Neutron & Dosimetry (LND), Zhang et al. 2020, *Science Advances* — 1369 µSv/day
  - Mars transit: Curiosity RAD cruise measurement — ~1.8 mSv/day
- Indicator absolute limits are **illustrative demo thresholds**, not clinical values. Action-card text is placeholder content pending review.
- All crew members and their readings are fictional and simulated. No real people's data.

**Software dependencies**

| Dependency | License |
|---|---|
| Spring Boot (starter-web, starter-validation, starter-test) | Apache 2.0 |
| Spring Framework | Apache 2.0 |
| Jackson | Apache 2.0 |
| Hibernate Validator (via starter-validation) | Apache 2.0 |
| Apache Tomcat (embedded, via starter-web) | Apache 2.0 |
| JUnit 5 | EPL 2.0 |
| AssertJ / Mockito / Hamcrest (via starter-test) | Apache 2.0 / MIT / BSD-3-Clause |
| Maven Wrapper | Apache 2.0 |

Lombok was listed as optional and is not used (plain Java records instead).

This project is licensed under the Apache License 2.0 — see [LICENSE](LICENSE).

## Use of AI

The code in this repository was generated with AI assistance (Claude Code by Anthropic) and reviewed by the team.
The application itself uses no AI/LLM at runtime.
