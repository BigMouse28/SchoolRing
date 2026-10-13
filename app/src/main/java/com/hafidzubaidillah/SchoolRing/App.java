package com.hafidzubaidillah.SchoolRing;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;

public class App extends Application {
    private static final String[] DAY_NAMES = {
            "Senin", "Selasa", "Rabu", "Kamis", "Jum'at", "Sabtu", "Minggu"
    };
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private ScheduleRepository repository;
    private TableView<ScheduleEntry> scheduleTable;
    private TextField timeField;
    private TextField titleField;
    private TextField bellField;
    private Label clockLabel;
    private Label monthLabel;
    private GridPane calendarGrid;
    private Button automaticButton;
    private Timeline clock;
    private LocalDate selectedDate;
    private YearMonth displayedMonth;
    private Long editingId;
    private boolean automaticActive;
    private String lastRungMinute = "";
    private MediaPlayer mediaPlayer;
    private Stage primaryStage;

    @Override
    public void start(Stage stage) {
        primaryStage = stage;
        repository = new ScheduleRepository(Path.of("SchoolRing.db"));
        scheduleTable = new TableView<>();
        timeField = new TextField();
        titleField = new TextField();
        bellField = new TextField();
        clockLabel = new Label();
        monthLabel = new Label();
        calendarGrid = new GridPane();
        automaticButton = new Button("▶  Mulai otomatis");
        clock = new Timeline(new KeyFrame(Duration.seconds(1), event -> updateClock()));
        selectedDate = LocalDate.now();
        displayedMonth = YearMonth.now();
        stage.setTitle("SchoolRing — Bel Sekolah Otomatis");
        stage.setMinWidth(850);
        stage.setMinHeight(620);
        stage.setScene(new Scene(buildMainView(), 1100, 750));
        stage.show();
        refreshSchedule();
        updateClock();
        clock.setCycleCount(Timeline.INDEFINITE);
        clock.play();
    }

    public String getGreeting() {
        return "SchoolRing";
    }

    private BorderPane buildMainView() {
        BorderPane root = new BorderPane();
        root.setTop(buildHeader());
        root.setCenter(buildWorkspace());
        root.getStylesheets().add(getClass().getResource("app.css").toExternalForm());
        return root;
    }

    private VBox buildHeader() {
        Label brand = new Label("SchoolRing");
        brand.getStyleClass().add("brand");
        Label version = new Label("1.1");
        version.setStyle("-fx-font-size: 17px; -fx-font-weight: bold; -fx-text-fill: #25344a;");
        HBox brandLine = new HBox(12, brand, version);
        brandLine.setAlignment(Pos.CENTER_LEFT);

        Label tagline = new Label("Bel Sekolah Otomatis  •  Gratis");
        tagline.getStyleClass().add("tagline");
        VBox header = new VBox(2, brandLine, tagline);
        header.getStyleClass().add("header");

        Button newButton = new Button("＋  Jadwal baru");
        newButton.setOnAction(event -> beginNewEntry());
        automaticButton.setTooltip(new Tooltip("Aktifkan atau jeda bel otomatis"));
        automaticButton.setOnAction(event -> toggleAutomatic());
        Button refreshButton = new Button("⟳  Muat ulang");
        refreshButton.setOnAction(event -> refreshSchedule());
        HBox toolbar = new HBox(10, newButton, automaticButton, refreshButton);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.getStyleClass().add("toolbar");
        return new VBox(header, toolbar);
    }

    private HBox buildWorkspace() {
        VBox schedulePane = buildSchedulePane();
        VBox sidebar = buildSidebar();
        sidebar.setPrefWidth(335);
        sidebar.setMinWidth(300);
        HBox.setHgrow(schedulePane, Priority.ALWAYS);
        HBox workspace = new HBox(schedulePane, sidebar);
        workspace.setFillHeight(true);
        return workspace;
    }

    private VBox buildSchedulePane() {
        HBox dayTabs = new HBox(4);
        dayTabs.getStyleClass().add("day-tabs");
        for (int day = 1; day <= 7; day++) {
            final int selectedDay = day;
            Button dayButton = new Button(DAY_NAMES[day - 1]);
            dayButton.getStyleClass().add("day-button");
            dayButton.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(dayButton, Priority.ALWAYS);
            dayButton.setOnAction(event -> selectDate(selectedDate.with(
                    java.time.DayOfWeek.of(selectedDay))));
            dayTabs.getChildren().add(dayButton);
        }

        TableColumn<ScheduleEntry, String> timeColumn = new TableColumn<>("Jam");
        timeColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().time().format(TIME_FORMAT)));
        timeColumn.setPrefWidth(95);
        TableColumn<ScheduleEntry, String> titleColumn = new TableColumn<>("Jadwal");
        titleColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().title()));
        titleColumn.setPrefWidth(220);
        titleColumn.setMinWidth(130);
        TableColumn<ScheduleEntry, String> bellColumn = new TableColumn<>("Bel");
        bellColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(fileName(cell.getValue().bellPath())));
        bellColumn.setPrefWidth(130);
        scheduleTable.getColumns().setAll(timeColumn, titleColumn, bellColumn);
        scheduleTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        scheduleTable.getStyleClass().add("schedule-table");
        scheduleTable.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, entry) -> {
                    if (entry != null) {
                        showEntry(entry);
                    }
                });
        VBox.setVgrow(scheduleTable, Priority.ALWAYS);
        VBox pane = new VBox(dayTabs, scheduleTable);
        pane.setPadding(new Insets(0, 10, 10, 10));
        VBox.setVgrow(scheduleTable, Priority.ALWAYS);
        return pane;
    }

    private VBox buildSidebar() {
        VBox calendarCard = buildCalendar();

        clockLabel.getStyleClass().add("digital-clock");
        Label clockCaption = new Label("WAKTU SEKARANG");
        clockCaption.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #7b8794;");
        VBox clockBox = new VBox(2, clockCaption, clockLabel);
        clockBox.setAlignment(Pos.CENTER);

        VBox editor = buildEditor();
        VBox sidebar = new VBox(9, calendarCard, clockBox, editor);
        sidebar.getStyleClass().add("side-panel");
        return sidebar;
    }

    private VBox buildCalendar() {
        Button previousMonth = new Button("‹");
        Button nextMonth = new Button("›");
        previousMonth.setOnAction(event -> {
            displayedMonth = displayedMonth.minusMonths(1);
            renderCalendar();
        });
        nextMonth.setOnAction(event -> {
            displayedMonth = displayedMonth.plusMonths(1);
            renderCalendar();
        });
        monthLabel.getStyleClass().add("month-title");
        HBox monthHeader = new HBox(8, previousMonth, monthLabel, nextMonth);
        monthHeader.setAlignment(Pos.CENTER);
        HBox.setHgrow(monthLabel, Priority.ALWAYS);
        monthLabel.setAlignment(Pos.CENTER);

        GridPane days = new GridPane();
        String[] weekDays = { "Min", "Sen", "Sel", "Rab", "Kam", "Jum", "Sab" };
        for (int column = 0; column < weekDays.length; column++) {
            Label label = new Label(weekDays[column]);
            label.setMaxWidth(Double.MAX_VALUE);
            label.setAlignment(Pos.CENTER);
            label.setStyle("-fx-font-size: 10px; -fx-font-weight: bold;");
            days.add(label, column, 0);
        }
        for (int column = 0; column < 7; column++) {
            ColumnConstraints constraints = new ColumnConstraints();
            constraints.setPercentWidth(100.0 / 7);
            days.getColumnConstraints().add(constraints);
        }
        calendarGrid.setHgap(2);
        calendarGrid.setVgap(2);
        VBox card = new VBox(6, monthHeader, days, calendarGrid);
        card.getStyleClass().add("calendar-card");
        renderCalendar();
        return card;
    }

    private VBox buildEditor() {
        Button newButton = new Button("Baru");
        newButton.setOnAction(event -> beginNewEntry());
        Button saveButton = new Button("Simpan");
        saveButton.setDefaultButton(true);
        saveButton.setOnAction(event -> saveEntry());
        Button cancelButton = new Button("Batal");
        cancelButton.setOnAction(event -> clearEditor());
        HBox actions = new HBox(8, newButton, saveButton, cancelButton);
        actions.setAlignment(Pos.CENTER);

        timeField.setPromptText("08:00");
        titleField.setPromptText("Contoh: Upacara");
        bellField.setPromptText("Pilih file suara (opsional)");
        Button browseButton = new Button("...");
        browseButton.setOnAction(event -> chooseBellFile());
        Button previewButton = new Button("▶");
        previewButton.setTooltip(new Tooltip("Putar pratinjau suara"));
        previewButton.setOnAction(event -> playBell(bellField.getText().trim()));
        HBox bellInput = new HBox(5, bellField, browseButton, previewButton);
        HBox.setHgrow(bellField, Priority.ALWAYS);

        VBox editor = new VBox(
                actions,
                fieldRow("Jam", timeField),
                fieldRow("Jadwal", titleField),
                fieldRow("Bel", bellInput));
        editor.getStyleClass().add("editor");
        Button deleteButton = new Button("Hapus jadwal");
        deleteButton.setMaxWidth(Double.MAX_VALUE);
        deleteButton.setOnAction(event -> deleteEntry());
        editor.getChildren().addAll(deleteButton,
                new Label("Format jam 24 jam. File suara dapat dipilih dalam format MP3/WAV."));
        editor.getChildren().get(editor.getChildren().size() - 1).getStyleClass().add("subtle");
        return editor;
    }

    private HBox fieldRow(String label, javafx.scene.Node field) {
        Label caption = new Label(label);
        caption.setPrefWidth(58);
        caption.setStyle("-fx-font-weight: bold;");
        HBox row = new HBox(7, caption, field);
        row.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(field, Priority.ALWAYS);
        return row;
    }

    private void selectDate(LocalDate date) {
        selectedDate = date;
        displayedMonth = YearMonth.from(date);
        renderCalendar();
        refreshSchedule();
    }

    private void refreshSchedule() {
        try {
            scheduleTable.setItems(FXCollections.observableArrayList(
                    repository.findByDay(selectedDate.getDayOfWeek().getValue())));
        } catch (IllegalStateException exception) {
            showError("Gagal memuat jadwal", exception.getMessage());
        }
    }

    private void renderCalendar() {
        monthLabel.setText(displayedMonth.getMonth().getDisplayName(TextStyle.FULL, Locale.forLanguageTag("id"))
                + " " + displayedMonth.getYear());
        calendarGrid.getChildren().clear();
        LocalDate first = displayedMonth.atDay(1);
        int startColumn = first.getDayOfWeek().getValue() % 7;
        for (int day = 1; day <= displayedMonth.lengthOfMonth(); day++) {
            LocalDate date = displayedMonth.atDay(day);
            Button dateButton = new Button(Integer.toString(day));
            dateButton.setMaxWidth(Double.MAX_VALUE);
            dateButton.setMinHeight(25);
            dateButton.setOnAction(event -> selectDate(date));
            if (date.equals(selectedDate)) {
                dateButton.setStyle("-fx-background-color: #ffe76a; -fx-border-color: #d60000; -fx-border-width: 2;");
            } else if (date.equals(LocalDate.now())) {
                dateButton.setStyle("-fx-font-weight: bold; -fx-border-color: #f0bd00;");
            } else if (date.getDayOfWeek() == java.time.DayOfWeek.SUNDAY) {
                dateButton.setStyle("-fx-text-fill: #c22;");
            }
            int offset = startColumn + day - 1;
            calendarGrid.add(dateButton, offset % 7, offset / 7);
        }
    }

    private void beginNewEntry() {
        editingId = null;
        scheduleTable.getSelectionModel().clearSelection();
        timeField.setText("09:00");
        titleField.clear();
        bellField.clear();
        titleField.requestFocus();
    }

    private void showEntry(ScheduleEntry entry) {
        editingId = entry.id();
        timeField.setText(entry.time().format(TIME_FORMAT));
        titleField.setText(entry.title());
        bellField.setText(entry.bellPath());
    }

    private void saveEntry() {
        LocalTime time;
        try {
            time = LocalTime.parse(timeField.getText().trim(), TIME_FORMAT);
        } catch (RuntimeException exception) {
            showError("Jam tidak valid", "Masukkan waktu dengan format HH:mm, misalnya 09:15.");
            return;
        }
        String title = titleField.getText().trim();
        if (title.isEmpty()) {
            showError("Jadwal belum diisi", "Masukkan nama kegiatan sebelum menyimpan.");
            titleField.requestFocus();
            return;
        }

        try {
            if (editingId == null) {
                ScheduleEntry created = repository.insert(
                        selectedDate.getDayOfWeek().getValue(), time, title, bellField.getText().trim());
                editingId = created.id();
            } else {
                repository.update(editingId, selectedDate.getDayOfWeek().getValue(),
                        time, title, bellField.getText().trim());
            }
            refreshSchedule();
            scheduleTable.getSelectionModel().select(editingId == null
                    ? null
                    : scheduleTable.getItems().stream()
                            .filter(entry -> entry.id() == editingId)
                            .findFirst().orElse(null));
        } catch (IllegalStateException | IllegalArgumentException exception) {
            showError("Jadwal tidak tersimpan", rootMessage(exception));
        }
    }

    private void deleteEntry() {
        if (editingId == null) {
            showError("Belum ada jadwal dipilih", "Pilih jadwal pada tabel yang ingin dihapus.");
            return;
        }
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION,
                "Hapus jadwal \"" + titleField.getText() + "\"?", ButtonType.CANCEL, ButtonType.OK);
        confirmation.setTitle("Konfirmasi hapus");
        confirmation.setHeaderText(null);
        confirmation.showAndWait().filter(response -> response == ButtonType.OK).ifPresent(response -> {
            try {
                repository.delete(editingId);
                clearEditor();
                refreshSchedule();
            } catch (IllegalStateException | IllegalArgumentException exception) {
                showError("Jadwal tidak terhapus", rootMessage(exception));
            }
        });
    }

    private void clearEditor() {
        editingId = null;
        scheduleTable.getSelectionModel().clearSelection();
        timeField.clear();
        titleField.clear();
        bellField.clear();
    }

    private void chooseBellFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Pilih suara bel");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Audio (MP3, WAV)", "*.mp3", "*.wav"),
                new FileChooser.ExtensionFilter("Semua file", "*.*"));
        File file = chooser.showOpenDialog(primaryStage);
        if (file != null) {
            bellField.setText(file.getAbsolutePath());
        }
    }

    private void toggleAutomatic() {
        automaticActive = !automaticActive;
        automaticButton.setText(automaticActive ? "⏸  Jeda otomatis" : "▶  Mulai otomatis");
        if (automaticActive) {
            checkSchedule();
        }
    }

    private void updateClock() {
        LocalTime now = LocalTime.now();
        clockLabel.setText(now.format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        if (automaticActive && now.getSecond() == 0) {
            checkSchedule();
        }
    }

    private void checkSchedule() {
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now().withSecond(0).withNano(0);
        String minuteKey = today + " " + now;
        if (minuteKey.equals(lastRungMinute)) {
            return;
        }
        try {
            repository.findByDay(today.getDayOfWeek().getValue()).stream()
                    .filter(entry -> entry.time().equals(now))
                    .findFirst()
                    .ifPresent(entry -> {
                        lastRungMinute = minuteKey;
                        playBell(entry.bellPath());
                    });
        } catch (IllegalStateException exception) {
            automaticActive = false;
            automaticButton.setText("▶  Mulai otomatis");
            showError("Bel otomatis dihentikan", exception.getMessage());
        }
    }

    private void playBell(String path) {
        if (path == null || path.isBlank()) {
            showError("File suara belum dipilih", "Pilih file MP3/WAV pada jadwal ini terlebih dahulu.");
            return;
        }
        File file = Path.of(path).toFile();
        if (!file.isFile()) {
            showError("File suara tidak ditemukan", "Periksa kembali lokasi file:\n" + path);
            return;
        }
        try {
            if (mediaPlayer != null) {
                mediaPlayer.stop();
                mediaPlayer.dispose();
            }
            mediaPlayer = new MediaPlayer(new Media(file.toURI().toString()));
            mediaPlayer.setOnError(() -> showError("Suara gagal diputar", mediaPlayer.getError().getMessage()));
            mediaPlayer.play();
        } catch (RuntimeException exception) {
            showError("Suara gagal diputar", rootMessage(exception));
        }
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message == null ? title : message);
        alert.show();
    }

    private static String fileName(String path) {
        return path == null || path.isBlank() ? "—" : Path.of(path).getFileName().toString();
    }

    private static String rootMessage(Throwable exception) {
        Throwable cause = exception;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause.getMessage() == null ? exception.getMessage() : cause.getMessage();
    }

    @Override
    public void stop() {
        clock.stop();
        if (mediaPlayer != null) {
            mediaPlayer.dispose();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
