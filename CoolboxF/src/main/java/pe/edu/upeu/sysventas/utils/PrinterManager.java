package pe.edu.upeu.sysventas.utils;

import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import java.io.IOException;

/**
 * Obtiene la impresora (térmica ESC/POS) que usa la aplicación.
 * Usa la impresora predeterminada de Windows; si no hay, toma la primera disponible.
 * Para forzar una en concreto, define la propiedad del sistema {@code printer.name}
 * (por ejemplo {@code -Dprinter.name="POS-80"}).
 */
public final class PrinterManager {

    private static final PrinterManager INSTANCE = new PrinterManager();

    private PrinterManager() {
    }

    public static PrinterManager getInstance() {
        return INSTANCE;
    }

    public PrintService getPrintService() throws IOException {
        PrintService[] servicios = PrintServiceLookup.lookupPrintServices(null, null);

        String nombre = System.getProperty("printer.name");
        if (nombre != null && !nombre.isBlank()) {
            for (PrintService s : servicios) {
                if (s.getName().equalsIgnoreCase(nombre.trim())) {
                    return s;
                }
            }
            throw new IOException("No se encontró la impresora: " + nombre);
        }

        PrintService porDefecto = PrintServiceLookup.lookupDefaultPrintService();
        if (porDefecto != null) {
            return porDefecto;
        }
        if (servicios.length > 0) {
            return servicios[0];
        }
        throw new IOException("No hay impresoras instaladas en el sistema.");
    }
}
