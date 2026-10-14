package com.nesworkbench;

import com.nesworkbench.graphics.ChrTileDecoder;
import com.nesworkbench.graphics.TileBrowserModel;
import com.nesworkbench.graphics.TileGridLayout;
import com.nesworkbench.graphics.TileSheetPngExporter;
import com.nesworkbench.graphics.TileSheetRenderer;
import com.nesworkbench.mapping.TileMapping;
import com.nesworkbench.project.ProjectData;
import com.nesworkbench.project.ProjectFile;
import com.nesworkbench.project.ProjectFormatException;
import com.nesworkbench.rom.RomFormatException;
import com.nesworkbench.rom.RomImage;
import com.nesworkbench.rom.RomLoader;
import com.nesworkbench.rom.RomSummary;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.Slider;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToolBar;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * JavaFX shell for inspecting NES graphics. All parsing, layout, and rendering decisions live in
 * JavaFX-free classes; this class only wires them to controls.
 */
public final class App extends Application {
    private Stage stage;
    private final Label status = new Label("Ready — no ROM loaded");
    private final Label placeholder = new Label("Open a NES ROM to begin.");
    private final ImageView sheetView = new ImageView();
    private final TextArea metadata = new TextArea("No ROM loaded.");
    private final Button openButton = new Button("Open ROM…");
    private final Button exportButton = new Button("Export PNG…");
    private final ComboBox<Integer> pageBox = new ComboBox<>();
    private final Slider zoomSlider = new Slider(TileGridLayout.MIN_ZOOM, TileGridLayout.MAX_ZOOM, 4);
    private final Label zoomLabel = new Label("4x");
    private final CheckBox gridBox = new CheckBox("Grid");
    private final Button openProjectButton = new Button("Open Project…");
    private final Button saveProjectButton = new Button("Save Project…");
    private final Button undoButton = new Button("Undo");
    private final Button redoButton = new Button("Redo");
    private final Button moveEarlierButton = new Button("◀ Move");
    private final Button moveLaterButton = new Button("Move ▶");

    private Path romPath, projectPath;
    private RomImage image;
    private TileMapping savedMapping;
    private RomSummary summary;
    private TileBrowserModel model;
    private boolean updatingControls;

    @Override
    public void start(Stage stage) {
        this.stage = stage;
        Label title = new Label("NES Asset Workbench");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold;");
        Label subtitle = new Label("Local-first tools for inspecting and editing NES graphics");
        subtitle.setStyle("-fx-text-fill: #616161;");
        VBox heading = new VBox(6, title, subtitle);
        heading.setPadding(new Insets(18));

        configureControls();
        ToolBar toolbar = new ToolBar(openButton, new Separator(), new Label("CHR bank:"), pageBox,
                new Separator(), new Label("Zoom:"), zoomSlider, zoomLabel, gridBox, new Separator(), exportButton);

        ToolBar mappingBar = new ToolBar(openProjectButton, saveProjectButton, new Separator(), undoButton, redoButton,
                new Separator(), moveEarlierButton, moveLaterButton,
                new Label("Click a tile to select it, then Ctrl/Cmd+click another tile to swap them."));

        sheetView.setPickOnBounds(true);
        sheetView.setOnMouseClicked(this::onSheetClicked);
        placeholder.setWrapText(true);
        placeholder.setAlignment(Pos.CENTER);
        ScrollPane scroll = new ScrollPane(sheetView);
        StackPane workspace = new StackPane(scroll, placeholder);
        workspace.setStyle("-fx-background-color: #f4f4f4;");

        metadata.setEditable(false);
        metadata.setWrapText(true);
        metadata.setPrefColumnCount(34);
        metadata.setStyle("-fx-font-family: 'monospaced';");
        VBox side = new VBox(6, new Label("ROM metadata"), metadata);
        side.setPadding(new Insets(10));

        status.setPadding(new Insets(10, 14, 10, 14));
        BorderPane root = new BorderPane();
        root.setTop(new VBox(0, heading, new Separator(), toolbar, mappingBar));
        root.setCenter(workspace);
        root.setRight(side);
        root.setBottom(new VBox(new Separator(), status));

        setControlsEnabled(false);
        stage.setTitle("NES Asset Workbench");
        stage.setMinWidth(900);
        stage.setMinHeight(560);
        Scene scene = new Scene(root, 1100, 720);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.Z, KeyCombination.SHORTCUT_DOWN), this::undo);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.Y, KeyCombination.SHORTCUT_DOWN), this::redo);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.Z, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN), this::redo);
        stage.setScene(scene);
        stage.show();
    }

    private void configureControls() {
        openButton.setOnAction(e -> chooseAndOpenRom());
        exportButton.setOnAction(e -> exportPng());
        openProjectButton.setOnAction(e -> openProject());
        saveProjectButton.setOnAction(e -> saveProject());
        undoButton.setOnAction(e -> undo());
        redoButton.setOnAction(e -> redo());
        moveEarlierButton.setOnAction(e -> moveSelected(-1));
        moveLaterButton.setOnAction(e -> moveSelected(1));
        zoomSlider.setMajorTickUnit(1);
        zoomSlider.setMinorTickCount(0);
        zoomSlider.setSnapToTicks(true);
        zoomSlider.setShowTickMarks(true);
        zoomSlider.valueProperty().addListener((obs, old, now) -> {
            if (updatingControls || model == null) return;
            int zoom = (int) Math.round(now.doubleValue());
            if (zoom == model.zoom()) return;
            model.setZoom(zoom);
            zoomLabel.setText(zoom + "x");
            refreshSheet();
        });
        gridBox.setOnAction(e -> { if (model != null) { model.setGridOverlay(gridBox.isSelected()); refreshSheet(); } });
        pageBox.setConverter(new StringConverter<>() {
            @Override public String toString(Integer page) {
                if (page == null || model == null) return "";
                int first = page * TileBrowserModel.TILES_PER_PAGE;
                int last = Math.min(model.tileCount(), first + TileBrowserModel.TILES_PER_PAGE) - 1;
                return "Bank " + page + " (tiles " + first + "–" + last + ")";
            }
            @Override public Integer fromString(String s) { return null; }
        });
        pageBox.setOnAction(e -> {
            if (updatingControls || model == null || pageBox.getValue() == null) return;
            model.setPage(pageBox.getValue());
            refreshSheet();
        });
    }

    private void chooseAndOpenRom() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Open NES ROM");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("NES ROMs (*.nes)", "*.nes"));
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("All files", "*.*"));
        if (romPath != null && romPath.getParent() != null) chooser.setInitialDirectory(romPath.getParent().toFile());
        java.io.File file = chooser.showOpenDialog(stage);
        if (file != null && confirmDiscardChanges()) loadRom(file.toPath());
    }

    private void loadRom(Path path) {
        try {
            RomImage loaded = RomLoader.load(path);
            summary = RomSummary.of(loaded);
            model = new TileBrowserModel(summary.usesChrRam() ? java.util.List.of() : ChrTileDecoder.decode(loaded.chrRom()));
            image = loaded;
            romPath = path;
            projectPath = null;
            savedMapping = model.mapping();
        } catch (RomFormatException | IOException ex) {
            showError("Cannot open " + path.getFileName(), ex.getMessage());
            status.setText("Failed to open " + path.getFileName() + ": " + ex.getMessage());
            return; // keep whatever was loaded before
        }
        updatingControls = true;
        try {
            metadata.setText(summary.toText());
            pageBox.getItems().setAll(java.util.stream.IntStream.range(0, model.pageCount()).boxed().toList());
            if (model.pageCount() > 0) pageBox.setValue(0);
            zoomSlider.setValue(model.zoom());
            zoomLabel.setText(model.zoom() + "x");
            gridBox.setSelected(model.gridOverlay());
        } finally {
            updatingControls = false;
        }
        setControlsEnabled(true);
        refreshSheet();
        status.setText(summary.usesChrRam()
                ? "Loaded " + path.getFileName() + " — CHR-RAM cartridge, no CHR-ROM tiles to display"
                : "Loaded " + path.getFileName() + " — " + model.tileCount() + " tiles. Click a tile to select it.");
    }

    private void refreshSheet() {
        TileSheetRenderer.RenderedSheet sheet = model.render();
        if (sheet.width() == 0 || sheet.height() == 0) {
            sheetView.setImage(null);
            placeholder.setText("This cartridge uses CHR-RAM, so the ROM contains no CHR tile data to display.");
            placeholder.setVisible(true);
        } else {
            WritableImage image = new WritableImage(sheet.width(), sheet.height());
            image.getPixelWriter().setPixels(0, 0, sheet.width(), sheet.height(),
                    PixelFormat.getIntArgbInstance(), sheet.argb(), 0, sheet.width());
            sheetView.setImage(image);
            placeholder.setVisible(false);
        }
        int selected = model.selectedTile();
        if (selected >= 0) status.setText(summary.describeMapping(selected, model.originalTileAt(selected)));
        syncEditControls();
    }

    /** Brings page box, edit buttons, and window title in line with the model after any change. */
    private void syncEditControls() {
        updatingControls = true;
        try {
            if (model.pageCount() > 0 && !Integer.valueOf(model.page()).equals(pageBox.getValue())) pageBox.setValue(model.page());
        } finally {
            updatingControls = false;
        }
        int selected = model.selectedTile();
        undoButton.setDisable(!model.canUndo());
        redoButton.setDisable(!model.canRedo());
        moveEarlierButton.setDisable(selected <= 0);
        moveLaterButton.setDisable(selected < 0 || selected >= model.tileCount() - 1);
        stage.setTitle("NES Asset Workbench" + (romPath == null ? "" : " — " + romPath.getFileName()) + (isDirty() ? " *" : ""));
    }

    private boolean isDirty() { return model != null && savedMapping != null && !model.mapping().equals(savedMapping); }

    private boolean confirmDiscardChanges() {
        if (!isDirty()) return true;
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "The tile mapping has unsaved changes. Discard them?", ButtonType.OK, ButtonType.CANCEL);
        alert.setHeaderText("Unsaved mapping changes");
        alert.initOwner(stage);
        return alert.showAndWait().filter(b -> b == ButtonType.OK).isPresent();
    }

    private void onSheetClicked(MouseEvent event) {
        if (model == null) return;
        int x = (int) event.getX(), y = (int) event.getY();
        if (event.isShortcutDown() && model.selectedTile() >= 0) {
            int local = model.layout().tileIndexAt(x, y);
            if (local >= 0 && model.swapSelectedWith(model.firstTileOfPage() + local)) refreshSheet();
            return;
        }
        if (model.selectAt(x, y) >= 0) refreshSheet();
    }

    private void moveSelected(int delta) {
        if (model != null && model.selectedTile() >= 0 && model.moveSelectedBy(delta)) refreshSheet();
    }

    private void undo() { if (model != null && model.canUndo()) { model.undo(); refreshSheet(); } }

    private void redo() { if (model != null && model.canRedo()) { model.redo(); refreshSheet(); } }

    private void saveProject() {
        if (model == null || model.tileCount() == 0) { showError("Nothing to save", "This ROM has no CHR tiles to map."); return; }
        FileChooser chooser = projectChooser("Save project");
        String base = romPath.getFileName().toString().replaceFirst("(?i)\\.nes$", "");
        chooser.setInitialFileName(projectPath != null ? projectPath.getFileName().toString() : base + ProjectFile.EXTENSION);
        java.io.File file = chooser.showSaveDialog(stage);
        if (file == null) return;
        Path target = file.toPath();
        try {
            if (Files.exists(target) && Files.isSameFile(target, romPath)) {
                showError("Save refused", "The project target is the source ROM. Choose a different file.");
                return;
            }
            // The save dialog has already asked the user to confirm replacing an existing file.
            ProjectFile.write(target, ProjectData.forRom(romPath.getFileName().toString(), image, model.mapping()), true);
            projectPath = target;
            savedMapping = model.mapping();
            syncEditControls();
            status.setText("Saved project " + target.getFileName());
        } catch (IOException | IllegalArgumentException ex) {
            showError("Save failed", ex.getMessage());
            status.setText("Save failed: " + ex.getMessage());
        }
    }

    private void openProject() {
        if (model == null) { showError("Open a ROM first", "A project is applied to the ROM it was saved for. Open that ROM, then open the project."); return; }
        java.io.File file = projectChooser("Open project").showOpenDialog(stage);
        if (file == null) return;
        try {
            ProjectData data = ProjectFile.read(file.toPath());
            data.verifyAgainst(image);
            if (!confirmDiscardChanges()) return;
            model.replaceMapping(data.mapping());
            projectPath = file.toPath();
            savedMapping = model.mapping();
            refreshSheet();
            status.setText("Loaded project " + file.getName() + " — " + model.tileCount() + " tiles mapped.");
        } catch (ProjectFormatException | IOException ex) {
            showError("Cannot open " + file.getName(), ex.getMessage());
            status.setText("Failed to open project " + file.getName() + ": " + ex.getMessage());
        }
    }

    private FileChooser projectChooser(String title) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(title);
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("NES Workbench projects (*" + ProjectFile.EXTENSION + ")", "*" + ProjectFile.EXTENSION));
        Path dir = projectPath != null ? projectPath.getParent() : romPath == null ? null : romPath.getParent();
        if (dir != null) chooser.setInitialDirectory(dir.toFile());
        return chooser;
    }

    private void exportPng() {
        if (model == null || model.tileCount() == 0) { showError("Nothing to export", "This ROM has no CHR tile data."); return; }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export tile sheet as PNG");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG image (*.png)", "*.png"));
        String base = romPath.getFileName().toString().replaceFirst("(?i)\\.nes$", "");
        chooser.setInitialFileName(base + "-chr.png");
        if (romPath.getParent() != null) chooser.setInitialDirectory(romPath.getParent().toFile());
        java.io.File file = chooser.showSaveDialog(stage);
        if (file == null) return;
        Path target = file.toPath();
        try {
            if (Files.exists(target) && Files.isSameFile(target, romPath)) {
                showError("Export refused", "The export target is the source ROM. Choose a different file.");
                return;
            }
            // The save dialog has already asked the user to confirm replacing an existing file.
            TileSheetPngExporter.write(target, TileSheetPngExporter.toPng(model.tiles(),
                    TileGridLayout.DEFAULT_COLUMNS, TileSheetRenderer.GRAYSCALE), true);
            status.setText("Exported " + model.tileCount() + " tiles to " + target.getFileName());
        } catch (IOException | IllegalArgumentException ex) {
            showError("Export failed", ex.getMessage());
            status.setText("Export failed: " + ex.getMessage());
        }
    }

    private void setControlsEnabled(boolean enabled) {
        pageBox.setDisable(!enabled); zoomSlider.setDisable(!enabled); gridBox.setDisable(!enabled); exportButton.setDisable(!enabled);
        openProjectButton.setDisable(!enabled); saveProjectButton.setDisable(!enabled);
        if (!enabled) { undoButton.setDisable(true); redoButton.setDisable(true); moveEarlierButton.setDisable(true); moveLaterButton.setDisable(true); }
    }

    private void showError(String header, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message);
        alert.setHeaderText(header);
        alert.initOwner(stage);
        alert.showAndWait();
    }

    public static void main(String[] args) { launch(args); }
}
