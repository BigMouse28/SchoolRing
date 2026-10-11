# SchoolRing

SchoolRing is a Java 21 Gradle application. The `app` module includes JavaFX
21 (Controls and FXML) and the SQLite JDBC driver. Gradle downloads these
dependencies automatically.

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

> **Current application status:** The configured main class currently prints
> `Hello World!`; it does not yet start a JavaFX window. JavaFX is on the
> classpath, but a JavaFX UI still needs to be implemented in the main class.

## Build and test

From the project root:

```powershell
.\gradlew.bat :app:build
.\gradlew.bat :app:test
```

Use `./gradlew` in place of `.\gradlew.bat` on macOS or Linux.

## SQLite

The SQLite JDBC driver is included as a Gradle dependency. Connect to a
database file with a JDBC URL such as:

```java
Connection connection = DriverManager.getConnection("jdbc:sqlite:schoolring.db");
```

The database file is created in the application's working directory when the
connection is opened.
