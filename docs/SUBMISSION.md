# Submission — Crew Health Assistant

Paste-ready text for the Space Apps 2026 project page, in the order of the form fields.
Deadline: **Sunday 15 November, 23:59 local time of your Local Event** (projects saved after that are not judged).

- **Team:** [team name] · **Members:** [names]
- **Challenge:** Create Health Monitoring Software for Astronauts on Space Missions
  (https://www.spaceappschallenge.org/2026/challenges/health-monitoring-software/)

---

## Project Name

Crew Health Assistant

## High-Level Project Summary

Crew Health Assistant is a visual, tablet-first tool that lets astronauts check their own health during a mission and
tells them what to do about it. It compares every crew member with their **own** pre-flight baseline, not population
averages, and color-codes each body system GREEN / AMBER / RED: heart, sleep, mood, bone & muscle, eyes and radiation.
Every problem opens an action card explaining what is happening, why it matters in space, and 2–3 simple "do now"
steps, each citing a NASA source. Because distance from Earth changes who can help, escalation advice adapts to the
mission: on the ISS the crew calls the flight surgeon; in Mars transit, with a 24-minute round trip, the card tells
them to start the protocol themselves and message ground in parallel. A radiation module replays a real 2017 solar
particle event from NASA's DONKI database. All rules are transparent and kept in data files, with no AI at runtime.
On long missions astronauts carry more of the responsibility for spotting problems early; this tool shows how that
can be done clearly and safely.

## Link to Project Demo

[YouTube / Drive link to docs/demo/crew-health-demo.mp4 — 29.4 s, captioned, no audio]
(GitHub copy: https://github.com/Talant2801/nasa-crew-health-assistant/blob/main/docs/demo/crew-health-demo.mp4)

## Link to Final Project

https://crew-health-assistant.onrender.com (live app; first load can take up to a minute while the free server wakes)
Source code: https://github.com/Talant2801/nasa-crew-health-assistant

## Detailed Project Description

**What it does**
- **Self-assessment at a glance.** One card per astronaut and six body-system tiles with color coding, the latest
  value, a plain-language comparison ("Resting heart rate 74 bpm — 16% above your baseline of 64") and a sparkline.
- **Personal baselines.** An indicator turns AMBER when it leaves the astronaut's own ± band or moves the wrong way
  3 days in a row, and RED when it crosses a safety limit. This catches slow drifts that a single reading would miss.
- **Guidance, not just numbers.** Each alert opens an action card: *What's happening*, *Why it matters in space*,
  numbered *Do now* steps written for tired people in microgravity (big text, big buttons), *Escalation*, and a
  linked NASA source.
- **When to involve flight surgeons.** RED or urgent cards say "contact the flight surgeon"; the wording changes with
  the mission context (ISS live link, lunar surface, Mars transit at 12 min one-way) and updates live while the card
  is open. AMBER routine cards say "monitor and re-measure tomorrow", prompting the next measurement.
- **Dynamic charts.** Tapping a tile opens a 30-day chart per indicator with the personal baseline band and the safety
  limit drawn in.
- **Radiation.** Current and projected mission dose against NASA's 600 mSv career limit; an active solar particle
  event turns the whole crew RED and opens the storm-shelter card.
- **Demo scenarios** (solar storm, sleep deficit, reset) let judges see the rules react in seconds.

**How it addresses the challenge**

| Challenge asks for | Crew Health Assistant |
|---|---|
| Visual tool for astronauts to assess their own health | Status view with per-system color coding and plain-language summaries |
| Guidance on treatments and health improvements | Action cards with "do now" steps based on NASA-STD-3001 briefs and HRP risk pages |
| Useful indicators that can be measured in space | Resting HR, HRV, systolic BP, sleep hours, mood check-in, exercise minutes, near-vision self-check |
| How spaceflight hazards affect indicators | "Why it matters in space" on every card: radiation, isolation, distance from Earth, altered gravity (fluid shift, SANS, bone loss) |
| When a result warrants crew or flight-surgeon attention | Explicit AMBER/RED rules plus mission-aware escalation text |
| Different environments (space station, spacecraft, Moon) | ISS / lunar surface / Mars transit switch: comms delay, dose rate and escalation change |
| Dynamic charts, color coding, simple explanations | 30-day trend charts with baseline band; GREEN/AMBER/RED everywhere; plain language in cards |

**How it works**
Java 21 / Spring Boot REST API with a deterministic simulator (fixed seed) and a rule engine. Thresholds, action cards
and mission profiles live in JSON files, so they can be reviewed and changed without touching code. The frontend is
plain HTML, CSS and JavaScript with Chart.js, tablet-first with a phone layout and 48 px touch targets. Packaged as a
Docker image and deployed on Render. 13 automated tests cover the rules, radiation model, API and data files.

**Benefits and what we hope to achieve**
Explainable rules a crew can trust and check, personal baselines that catch early drift, and advice that fits how far
the crew is from help. Next steps: crew self-entry and wearable feeds instead of simulated data, measurement reminders
in the daily schedule, clinician-reviewed thresholds, and offline operation on a crew tablet.

**Limitations**
Crew data is simulated. Indicator limits are illustrative, not clinical. The radiation model is simplified
(mission days × published dose rate). Decision support only, not a diagnosis and not a medical device.

**Tools:** Java 21, Spring Boot 3.5, Maven, JUnit 5, HTML/CSS/JavaScript, Chart.js, Docker, Render, GitHub.

## Use of Super Intelligence (SI)

Yes. The code, tests, documentation and the demo video's captions were written with AI assistance (Claude Code by
Anthropic) and reviewed by the team. This is stated in the README and as a visible label in the demo video. The demo
video is a real screen recording of the app, not generated imagery. The application itself uses no AI at runtime:
every status and recommendation comes from rules and data files. No NASA logos or branding are used.

## NASA Data

- **NASA DONKI** (Space Weather Database Of Notifications, Knowledge, Information) — solar energetic particle (SEP)
  events for September 2017 via api.nasa.gov; the app replays the 2017-09-04 event (cached file, no live calls).
- **NASA-STD-3001 Volume 1 Rev C** — career dose limit 600 mSv [V1 4030], design-reference solar particle event
  < 250 mSv [V1 4031], ALARA [V1 4029]; aerobic, muscle and bone requirements [V1 4003], [V1 4024], [V1 4027].
  https://standards.nasa.gov/sites/default/files/standards/NASA/C//nasa-std-3001-vol-1-rev-c-signature.pdf
- **NASA-STD-3001 technical briefs (OCHMO):** Radiation Protection (TB-020), Exercise Overview (TB-031),
  Sleep Accommodations, Behavioral Health Mishaps (TB-014).
- **NASA Human Research Program risk pages:** Cardiovascular Adaptations; Inadequate Sleep and Irregular Schedules;
  Spaceflight Associated Neuro-ocular Syndrome (SANS).
- **Curiosity RAD** (Mars Science Laboratory) cruise dose rate, Zeitlin et al. 2013, *Science*,
  https://doi.org/10.1126/science.1235989

## Space Agency Partner Data and Other References

- Lunar surface dose: Zhang et al. 2020, "First measurements of the radiation dose on the lunar surface",
  *Science Advances*, https://doi.org/10.1126/sciadv.aaz1334 (Chang'e 4 LND)
- ISS dose overview: *Z Med Phys* 2024, https://pmc.ncbi.nlm.nih.gov/articles/PMC10919966/
- Software: Spring Boot, Spring Framework, Jackson, Hibernate Validator, Apache Tomcat (Apache 2.0); JUnit 5 (EPL 2.0);
  Chart.js (MIT). Full list and all action-card source links: README "Data & Licenses".
- Tools: Claude Code (Anthropic) for AI-assisted coding; Render (hosting); GitHub (code).
- All crew members and readings are fictional. No copyrighted images are used; screenshots and video are of our app.
