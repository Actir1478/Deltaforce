package pe.edu.upeu.sysventas.controller;


import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.print.PrinterJob;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import pe.edu.upeu.sysventas.components.comprob.ComprobanteHtml;
import pe.edu.upeu.sysventas.components.comprob.ComprobantePdfService;
import pe.edu.upeu.sysventas.components.comprob.ComprobanteView;
import pe.edu.upeu.sysventas.dto.comprobante.Comprobante;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;


public class MainComprobanteController {

    @FXML private ScrollPane       scrollVisor;
    @FXML private Button           btnExportarPdf;
    @FXML private Button           btnImprimir;

    // El servicio de PDF usa java.awt (OpenPDF), que no existe en el ejecutable nativo.
    // Por eso se crea solo al exportar, para que el visor de la boleta abra igual.

    private VBox   vistaActual;
    private Comprobante boletaActual;

    @FXML
    public void initialize() {
        btnExportarPdf.setDisable(true);
        btnImprimir.setDisable(true);
        // mostrarBoleta() se llama desde pasarDatos(), cuando ya hay boleta
    }


    public void pasarDatos(Comprobante boleta) {
        this.boletaActual = boleta;
        mostrarBoleta();
    }

    public void mostrarBoleta() {
        btnExportarPdf.setDisable(true);
        btnImprimir.setDisable(true);
        Task<VBox> task = new Task<>() {
            @Override
            protected VBox call() {
                return new ComprobanteView(boletaActual).construir();
            }
        };

        task.setOnSucceeded(e -> Platform.runLater(() -> {
            vistaActual = task.getValue();

            // Centrar la boleta en el ScrollPane con fondo gris
            VBox wrapper = new VBox(vistaActual);
            wrapper.setStyle("-fx-background-color: #F2F2F2; -fx-padding: 16;");
            wrapper.setAlignment(javafx.geometry.Pos.TOP_CENTER);

            scrollVisor.setContent(wrapper);
            btnExportarPdf.setDisable(false);
            btnImprimir.setDisable(false);
        }));

        task.setOnFailed(e -> Platform.runLater(() -> {
            Throwable ex = task.getException();
            mostrarError("Error", "No se pudo generar la boleta",
                    ex == null ? "Error desconocido"
                            : (ex.getMessage() != null ? ex.getMessage() : ex.toString()));
        }));

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    // Acciones
    @FXML
    private void onExportarPdf() {
        if (boletaActual == null) return;

        // En el ejecutable nativo no hay AWT (OpenPDF): se abre la boleta en el navegador
        if (esEjecutableNativo()) {
            avisarGuardarPdf();
            abrirBoletaEnNavegador(true);
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Guardar Comprobante como PDF");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Archivo PDF (*.pdf)", "*.pdf"));
        chooser.setInitialFileName(
                "boleta_" + boletaActual.getSerie() + "_" + boletaActual.getNumero() + ".pdf");

        File archivo = chooser.showSaveDialog(btnExportarPdf.getScene().getWindow());
        if (archivo == null) return;
        btnExportarPdf.setDisable(true);

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                new ComprobantePdfService().exportar(boletaActual, archivo.toPath());
                return null;
            }
        };

        task.setOnSucceeded(e -> Platform.runLater(() -> {
            btnExportarPdf.setDisable(false);
            abrirArchivo(archivo);
        }));

        task.setOnFailed(e -> Platform.runLater(() -> {
            btnExportarPdf.setDisable(false);
            Throwable ex = task.getException();
            if (ex instanceof Error) {
                // Falta una librería nativa (AWT): alternativa por el navegador
                avisarGuardarPdf();
                abrirBoletaEnNavegador(true);
                return;
            }
            String detalle = ex == null ? "Error desconocido"
                    : (ex.getMessage() != null ? ex.getMessage() : ex.toString());
            mostrarError("Error al exportar PDF", "No se pudo generar el PDF", detalle);
        }));

        new Thread(task) {{ setDaemon(true); }}.start();
    }

    @FXML
    private void onImprimir() {
        if (vistaActual == null) return;

        // En el ejecutable nativo no hay AWT ni impresión de JavaFX: se imprime desde el navegador
        if (esEjecutableNativo()) {
            abrirBoletaEnNavegador(true);
            return;
        }

        try {
            PrinterJob job = PrinterJob.createPrinterJob();
            if (job == null) {
                mostrarError("Sin impresora",
                        "No se encontró ninguna impresora instalada",
                        "Instala una impresora o usa 'Exportar PDF'.");
                return;
            }

            boolean imprimir = job.showPrintDialog(btnImprimir.getScene().getWindow());
            if (imprimir) {
                job.printPage(vistaActual);
                job.endJob();
            }
        } catch (Throwable t) {
            // Si la impresión de JavaFX no está disponible, se usa el navegador
            abrirBoletaEnNavegador(true);
        }
    }

    /** true cuando el programa corre como ejecutable nativo (GraalVM native image). */
    private boolean esEjecutableNativo() {
        return System.getProperty("org.graalvm.nativeimage.imagecode") != null;
    }

    /** Guarda la boleta como HTML temporal y la abre en el navegador (opcionalmente con el diálogo de impresión). */
    private void abrirBoletaEnNavegador(boolean imprimirAlAbrir) {
        try {
            String nombre = ("boleta_" + boletaActual.getSerie() + "_" + boletaActual.getNumero())
                    .replaceAll("[^A-Za-z0-9_\\-]", "_") + ".html";
            Path archivo = Path.of(System.getProperty("java.io.tmpdir"), nombre);
            Files.writeString(archivo, ComprobanteHtml.construir(boletaActual, imprimirAlAbrir), StandardCharsets.UTF_8);
            abrirArchivo(archivo.toFile());
        } catch (Exception e) {
            mostrarError("Error", "No se pudo abrir la boleta",
                    e.getMessage() != null ? e.getMessage() : e.toString());
        }
    }

    private void avisarGuardarPdf() {
        Alert info = new Alert(Alert.AlertType.INFORMATION);
        info.setTitle("Exportar PDF");
        info.setHeaderText("La boleta se abrirá en el navegador");
        info.setContentText("En la ventana de impresión elige \"Guardar como PDF\" para guardarla,\n"
                + "o elige tu impresora para imprimirla.");
        info.showAndWait();
    }

    /** Abre el archivo con el programa predeterminado del sistema (sin usar java.awt.Desktop). */
    private void abrirArchivo(File archivo) {
        try {
            String so = System.getProperty("os.name", "").toLowerCase();
            if (so.contains("win")) {
                new ProcessBuilder("cmd", "/c", "start", "", archivo.getAbsolutePath()).start();
            } else if (so.contains("mac")) {
                new ProcessBuilder("open", archivo.getAbsolutePath()).start();
            } else {
                new ProcessBuilder("xdg-open", archivo.getAbsolutePath()).start();
            }
        } catch (Exception ignored) {
        }
    }


    private void mostrarError(String titulo, String header, String detalle) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(titulo);
        alert.setHeaderText(header);
        alert.setContentText(detalle);
        alert.showAndWait();
    }
}