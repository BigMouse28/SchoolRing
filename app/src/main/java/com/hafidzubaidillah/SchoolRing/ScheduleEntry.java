package com.hafidzubaidillah.SchoolRing;

import java.time.LocalTime;

public record ScheduleEntry(long id, int dayOfWeek, LocalTime time, String title, String bellPath) {
}
