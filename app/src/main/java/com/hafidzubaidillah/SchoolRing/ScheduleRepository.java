package com.hafidzubaidillah.SchoolRing;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class ScheduleRepository {
    private final String jdbcUrl;

    public ScheduleRepository(Path databasePath) {
        jdbcUrl = "jdbc:sqlite:" + databasePath;
        initialize();
    }

    private void initialize() {
        String sql = """
                CREATE TABLE IF NOT EXISTS schedule_entries (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    day_of_week INTEGER NOT NULL CHECK (day_of_week BETWEEN 1 AND 7),
                    time TEXT NOT NULL,
                    title TEXT NOT NULL,
                    bell_path TEXT NOT NULL DEFAULT '',
                    UNIQUE (day_of_week, time)
                )
                """;
        try (Connection connection = connect(); Statement statement = connection.createStatement()) {
            statement.execute(sql);
        } catch (SQLException exception) {
            throw new IllegalStateException("Tidak dapat menyiapkan database.", exception);
        }
    }

    public List<ScheduleEntry> findByDay(int dayOfWeek) {
        String sql = """
                SELECT id, day_of_week, time, title, bell_path
                FROM schedule_entries
                WHERE day_of_week = ?
                ORDER BY time, id
                """;
        List<ScheduleEntry> entries = new ArrayList<>();
        try (Connection connection = connect(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, dayOfWeek);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    entries.add(new ScheduleEntry(
                            result.getLong("id"),
                            result.getInt("day_of_week"),
                            LocalTime.parse(result.getString("time")),
                            result.getString("title"),
                            result.getString("bell_path")));
                }
            }
            return entries;
        } catch (SQLException exception) {
            throw new IllegalStateException("Tidak dapat membaca jadwal dari database.", exception);
        }
    }

    public List<ScheduleEntry> findAll() {
        String sql = """
                SELECT id, day_of_week, time, title, bell_path
                FROM schedule_entries
                ORDER BY day_of_week, time, id
                """;
        List<ScheduleEntry> entries = new ArrayList<>();
        try (Connection connection = connect();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                entries.add(new ScheduleEntry(
                        result.getLong("id"),
                        result.getInt("day_of_week"),
                        LocalTime.parse(result.getString("time")),
                        result.getString("title"),
                        result.getString("bell_path")));
            }
            return entries;
        } catch (SQLException exception) {
            throw new IllegalStateException("Tidak dapat membaca semua jadwal dari database.", exception);
        }
    }

    public ScheduleEntry insert(int dayOfWeek, LocalTime time, String title, String bellPath) {
        String sql = """
                INSERT INTO schedule_entries (day_of_week, time, title, bell_path)
                VALUES (?, ?, ?, ?)
                """;
        try (Connection connection = connect();
                PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(statement, dayOfWeek, time, title, bellPath);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("Database tidak mengembalikan ID jadwal baru.");
                }
                return new ScheduleEntry(keys.getLong(1), dayOfWeek, time, title, bellPath);
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Tidak dapat menyimpan jadwal baru.", exception);
        }
    }

    public void update(long id, int dayOfWeek, LocalTime time, String title, String bellPath) {
        String sql = """
                UPDATE schedule_entries
                SET day_of_week = ?, time = ?, title = ?, bell_path = ?
                WHERE id = ?
                """;
        try (Connection connection = connect(); PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, dayOfWeek, time, title, bellPath);
            statement.setLong(5, id);
            if (statement.executeUpdate() == 0) {
                throw new IllegalArgumentException("Jadwal yang akan diubah tidak ditemukan.");
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Tidak dapat memperbarui jadwal.", exception);
        }
    }

    public void delete(long id) {
        try (Connection connection = connect();
                PreparedStatement statement = connection
                        .prepareStatement("DELETE FROM schedule_entries WHERE id = ?")) {
            statement.setLong(1, id);
            if (statement.executeUpdate() == 0) {
                throw new IllegalArgumentException("Jadwal yang akan dihapus tidak ditemukan.");
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Tidak dapat menghapus jadwal.", exception);
        }
    }

    private static void bind(PreparedStatement statement, int dayOfWeek, LocalTime time,
            String title, String bellPath) throws SQLException {
        statement.setInt(1, dayOfWeek);
        statement.setString(2, time.toString());
        statement.setString(3, title);
        statement.setString(4, bellPath);
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(jdbcUrl);
    }
}
