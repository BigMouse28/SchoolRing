# SchoolRing

SchoolRing is a Java 21 desktop application for managing an automatic school
bell schedule. Its JavaFX interface includes weekday schedules, a calendar,
a live clock, and controls for creating, updating, deleting, and previewing
bell sounds. Schedules are stored locally in SQLite.

## Requirements

- JDK 21
- Visual Studio Code with the **Extension Pack for Java** and **Gradle for Java**
  extensions (or another Java IDE)

## Run from VS Code

1. Open the project root folder in VS Code.
2. Open **Terminal → New Terminal**.
3. From the project root, run:

   ```powershell
   .\gradlew.bat :app:run
   ```

   On macOS or Linux, run `./gradlew :app:run` instead.

You can also open the Gradle view in VS Code and run
`SchoolRing → app → Tasks → application → run`.

## Build and test

From the project root:

```powershell
.\gradlew.bat :app:build
.\gradlew.bat :app:test
```

Use `./gradlew` in place of `.\gradlew.bat` on macOS or Linux.

## Build a Windows executable

On Windows, install a full JDK 21 or newer, then run:

```powershell
.\gradlew.bat :app:packageWindows
```

The portable application bundle is created in
`app\build\package\windows\SchoolRing`. Run `SchoolRing.exe` from that folder.
The bundle includes its Java runtime, so users do not need to install Java.
Share or copy the **whole folder**, not just the `.exe`, because it uses the
bundled runtime and application libraries alongside the launcher.

To create a ZIP for distribution, run:

```powershell
.\gradlew.bat :app:packageWindowsZip
```

The ZIP is written to `app\build\package\SchoolRing-windows-x64.zip`. Build
this bundle on Windows; `jpackage` creates native packages for the host OS.

## Using the application

- Select a weekday tab or a date on the calendar to view that day's schedule.
- Use **Jadwal baru**, or **Baru** in the editor, to create an entry. Enter its
  time in 24-hour `HH:mm` format and a schedule name.
- Select an optional MP3/WAV file for the bell. Use **▶** next to the file
  field to preview it.
- Select a schedule to edit or delete it. A weekday cannot have two schedules
  at the same time.
- Click **Mulai otomatis** to check the current day's schedule and play the
  matching sound at its configured time. Automatic playback is off when the
  application starts.

The `schoolring.db` SQLite database is created in the application's working
directory. The SQLite JDBC driver and JavaFX dependencies are managed by
Gradle.
