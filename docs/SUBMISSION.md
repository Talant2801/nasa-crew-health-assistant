# Submission draft — Crew Health Assistant

Copy-paste material for the Space Apps project page. Fill the `[brackets]`; check field names and length limits
against the current submission form before pasting.

- **Project name:** Crew Health Assistant
- **Team:** [team name]
- **Challenge:** [challenge name]
- **Repository:** https://github.com/Talant2801/nasa-crew-health-assistant
- **Live demo:** [Render URL]
- **Demo video / slides:** [link]

## High-level summary (short)

Crew Health Assistant is an onboard decision-support tool that compares every astronaut with their own pre-flight
baseline, shows GREEN / AMBER / RED status per body system, and turns each problem into a clear action card:
what is happening, why it matters in space, and what to do now. Escalation advice adapts to where the crew is:
on the ISS they call the flight surgeon; in Mars transit, with a 24-minute round trip, the card tells them to start the
protocol themselves and message ground in parallel. Radiation status combines published dose rates with a replayed
real solar particle event from NASA DONKI. Every rule is transparent, every card cites its NASA source, and there is
no AI in the loop.

## Project details

**What it does**
- Personal baselines, not population averages: each indicator (resting heart rate, HRV, blood pressure, sleep, mood,
  exercise, near-vision self-check) is AMBER when it leaves the astronaut's own ± band or moves the wrong way 3 days
  in a row, and RED when it crosses a safety limit.
- Body-system tiles (cardiovascular, sleep, mental, bone & muscle, eyes, radiation) roll up to one status per
  astronaut, so the crew sees who needs attention first.
- Action cards with numbered "Do now" steps written for tired people in microgravity: big text, big buttons.
- Mission context switch (ISS / Moon / Mars) changes comms delay, dose rate and escalation wording live.
- Radiation module: current and projected mission dose against NASA's 600 mSv career limit; an active solar
  particle event turns the whole crew RED and opens the storm-shelter card.

**How it works**
Java 21 / Spring Boot REST backend with an in-memory, deterministic simulator (fixed seed) and a rule engine;
thresholds, cards and mission profiles live in JSON files, not code. Frontend is plain HTML/CSS/JavaScript with
Chart.js, tablet-first with a phone layout. Packaged as a Docker image.

**Why it matters**
The further crews go, the less they can lean on real-time ground support. Mars missions will need crews to notice
problems early and act on their own. This prototype shows how a simple, explainable tool can do that without
pretending to diagnose.

**Limitations (be upfront)**
Crew data is simulated. Indicator limits are illustrative, not clinical. The radiation model is a simplification
(mission days × published dose rate). It is decision support, not a diagnosis and not a medical device.

## NASA data and resources used

- NASA DONKI (Space Weather Database) — SEP event list, September 2017; the app replays the 2017-09-04 event.
- NASA-STD-3001 Volume 1 Rev C — radiation limits [V1 4029], [V1 4030], [V1 4031]; muscle, aerobic and bone
  countermeasure requirements [V1 4003], [V1 4024], [V1 4027].
- NASA-STD-3001 technical briefs (OCHMO): Radiation Protection (TB-020), Exercise Overview (TB-031),
  Sleep Accommodations, Behavioral Health Mishaps (TB-014).
- NASA Human Research Program risk pages: Cardiovascular Adaptations; Inadequate Sleep and Irregular Schedules;
  Spaceflight Associated Neuro-ocular Syndrome (SANS).
- Dose-rate measurements: Curiosity RAD cruise (Zeitlin et al. 2013), Chang'e 4 LND (Zhang et al. 2020),
  ISS dose overview (Z Med Phys 2024).

Full links: see "Data & Licenses" in the README.

## Use of AI

The code was generated with AI assistance (Claude Code by Anthropic) and reviewed by the team. The application
itself uses no AI at runtime: all statuses and recommendations come from rules and data files.

---

# Demo video script (~30 s)

Check the current length limit in the Space Apps rules. Record at 1280×800, app in ISS mode after **Demo → Reset**.

| Time | Screen | Voice-over |
|---|---|---|
| 0–6 s | Status view, crew cards and tiles | "Crew Health Assistant compares each astronaut with their *own* baseline. Here, Dr. Navarro's heart rate variability has dropped three days in a row." |
| 6–12 s | Demo → **Solar storm**; header turns red, radiation tile pulses | "We replay a real 2017 solar particle event from NASA's DONKI database. Radiation goes red for the whole crew." |
| 12–20 s | Click the Radiation tile → action card | "Every alert becomes an action card: what's happening, why it matters in space, and what to do now, with its NASA source." |
| 20–30 s | With the card open, click **Mars**; escalation box updates | "Now we're in Mars transit. With a 24-minute round trip, the card tells the crew to start the protocol themselves. Same data, the right advice for where they are." |

Longer cut (60–90 s): add the sleep-deficit scenario and the 30-day trend chart with the baseline band.
