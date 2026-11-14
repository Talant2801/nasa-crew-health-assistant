package com.spaceapps.crewhealth.radiation;

import com.spaceapps.crewhealth.model.MissionContext;
import com.spaceapps.crewhealth.model.SpaceWeather;
import com.spaceapps.crewhealth.model.Status;
import org.springframework.stereotype.Service;

import static com.spaceapps.crewhealth.radiation.RadiationLimits.*;

@Service
public class RadiationService {

    public RadiationAssessment assess(MissionContext ctx, int missionDay, SpaceWeather weather) {
        double current = round2(missionDay * ctx.doseRateMSvPerDay());
        double projected = round2(ctx.missionLengthDays() * ctx.doseRateMSvPerDay());
        Status status;
        String card;
        String summary;
        if (weather != null && weather.activeSpe()) {
            status = Status.RED;
            card = "spe-shelter";
            summary = "Active solar particle event — shelter now. Dose so far " + current + " mSv.";
        } else if (current >= CAREER_LIMIT_MSV) {
            status = Status.RED;
            card = "dose-projection";
            summary = "Mission dose " + current + " mSv has reached the career limit of " + (int) CAREER_LIMIT_MSV + " mSv.";
        } else if (projected >= CAREER_LIMIT_MSV) {
            status = Status.AMBER;
            card = "dose-projection";
            summary = "Projected mission dose " + projected + " mSv exceeds the career limit of " + (int) CAREER_LIMIT_MSV + " mSv.";
        } else {
            status = Status.GREEN;
            card = null;
            summary = "Dose " + current + " mSv so far; projected " + projected + " mSv (limit " + (int) CAREER_LIMIT_MSV + " mSv).";
        }
        return new RadiationAssessment("RADIATION", status, current, projected, CAREER_LIMIT_MSV, SPE_LIMIT_MSV,
                weather != null && weather.activeSpe(), MODEL, summary, card);
    }

    private static double round2(double v) {
        return Math.round(v * 100) / 100.0;
    }
}
