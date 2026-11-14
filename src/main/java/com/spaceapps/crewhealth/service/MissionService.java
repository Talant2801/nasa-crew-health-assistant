package com.spaceapps.crewhealth.service;

import com.spaceapps.crewhealth.config.DataLoader;
import com.spaceapps.crewhealth.model.Environment;
import com.spaceapps.crewhealth.model.MissionContext;
import com.spaceapps.crewhealth.model.SpaceWeather;
import org.springframework.stereotype.Service;

/** In-memory mission state. */
@Service
public class MissionService {

    public static final Environment DEFAULT_ENV = Environment.ISS;
    public static final int DEFAULT_MISSION_DAY = 30;

    private final DataLoader data;
    private volatile Environment env = DEFAULT_ENV;
    private volatile int missionDay = DEFAULT_MISSION_DAY;
    private volatile SpaceWeather spaceWeather = SpaceWeather.quiet();

    public MissionService(DataLoader data) {
        this.data = data;
    }

    public MissionContext context() {
        return data.mission(env);
    }

    public int missionDay() {
        return missionDay;
    }

    public SpaceWeather spaceWeather() {
        return spaceWeather;
    }

    /** Keeps missionDay; dose rate, comms delay and escalation text follow the new environment. */
    public void setEnvironment(Environment env) {
        this.env = env;
    }

    public void setSpaceWeather(SpaceWeather spaceWeather) {
        this.spaceWeather = spaceWeather;
    }

    public synchronized void reset() {
        env = DEFAULT_ENV;
        missionDay = DEFAULT_MISSION_DAY;
        spaceWeather = SpaceWeather.quiet();
    }
}
