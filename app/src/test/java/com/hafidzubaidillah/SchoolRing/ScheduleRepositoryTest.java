package com.hafidzubaidillah.SchoolRing;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ScheduleRepositoryTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void savesReadsUpdatesAndDeletesEntries() {
        ScheduleRepository repository = new ScheduleRepository(temporaryDirectory.resolve("test.db"));

        ScheduleEntry created = repository.insert(1, LocalTime.of(7, 15), "Jam Pelajaran 1", "bell.mp3");
        assertEquals(List.of(created), repository.findByDay(1));
        assertEquals(List.of(created), repository.findAll());

        repository.update(created.id(), 2, LocalTime.of(8, 0), "Upacara", "anthem.wav");
        ScheduleEntry updated = new ScheduleEntry(created.id(), 2, LocalTime.of(8, 0), "Upacara", "anthem.wav");
        assertEquals(List.of(), repository.findByDay(1));
        assertEquals(List.of(updated), repository.findByDay(2));

        repository.delete(created.id());
        assertEquals(List.of(), repository.findAll());
    }

    @Test
    void rejectsDuplicateTimeWithinOneDay() {
        ScheduleRepository repository = new ScheduleRepository(temporaryDirectory.resolve("duplicate.db"));
        repository.insert(1, LocalTime.of(9, 0), "Istirahat", "bell.mp3");

        assertThrows(IllegalStateException.class,
                () -> repository.insert(1, LocalTime.of(9, 0), "Upacara", "bell.mp3"));
    }

    @Test
    void dataSurvivesRepositoryRecreation() {
        Path database = temporaryDirectory.resolve("persistent.db");
        ScheduleRepository firstRepository = new ScheduleRepository(database);
        ScheduleEntry entry = firstRepository.insert(5, LocalTime.of(10, 30), "Istirahat", "");

        ScheduleRepository reopenedRepository = new ScheduleRepository(database);
        assertEquals(List.of(entry), reopenedRepository.findByDay(5));
    }
}
