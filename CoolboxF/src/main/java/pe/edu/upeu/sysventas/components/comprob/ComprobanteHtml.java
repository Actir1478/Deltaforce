package pe.edu.upeu.sysventas.components.comprob;

import pe.edu.upeu.sysventas.dto.comprobante.Comprobante;
import pe.edu.upeu.sysventas.dto.comprobante.ItemComprobante;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Arma la boleta como una página HTML sencilla.
 *
 * <p>Se usa en el ejecutable nativo, donde no existen AWT ni la impresión de Java: la boleta se
 * abre en el navegador y desde ahí se imprime o se guarda como PDF (Ctrl + P).
 */
public final class ComprobanteHtml {

    private ComprobanteHtml() {
    }

    public static String construir(Comprobante c, boolean imprimirAlAbrir) {
        StringBuilder sb = new StringBuilder(4096);
        sb.append("<!DOCTYPE html><html lang=\"es\"><head><meta charset=\"UTF-8\">");
        sb.append("<title>").append(esc(c.getTipoDocumento())).append(' ')
                .append(esc(c.getSerie())).append('-').append(esc(c.getNumero())).append("</title>");
        sb.append("<style>");
        sb.append("body{font-family:Arial,Helvetica,sans-serif;font-size:13px;color:#111;background:#f2f2f2;margin:0;padding:16px}");
        sb.append(".boleta{max-width:420px;margin:0 auto;background:#fff;padding:18px;border:1px solid #ccc}");
        sb.append(".centro{text-align:center}.empresa{font-size:16px;font-weight:bold}");
        sb.append(".tipo{border:1px solid #111;padding:6px;margin:10px 0;text-align:center;font-weight:bold}");
        sb.append("table{width:100%;border-collapse:collapse}");
        sb.append("th{border-top:1px solid #111;border-bottom:1px solid #111;padding:4px 2px;text-align:left}");
        sb.append("td{padding:3px 2px;vertical-align:top}.der{text-align:right}");
        sb.append(".totales td{padding:2px}.total td{font-weight:bold;font-size:15px;border-top:1px solid #111}");
        sb.append(".son{margin:8px 0;font-size:12px}.ahorro{color:#2e7d32;font-size:12px}");
        sb.append("hr{border:0;border-top:1px dashed #888;margin:10px 0}");
        sb.append("@media print{body{background:#fff;padding:0}.boleta{border:0;max-width:none}}");
        sb.append("</style></head><body><div class=\"boleta\">");

        sb.append("<div class=\"centro\"><div class=\"empresa\">").append(esc(c.getRazonSocial())).append("</div>");
        sb.append("RUC: ").append(esc(c.getRucEmisor())).append("<br>");
        sb.append(esc(c.getDireccion())).append("<br>");
        if (!vacio(c.getUbigeo())) sb.append(esc(c.getUbigeo())).append("<br>");
        if (!vacio(c.getTelefono())) sb.append("Tel: ").append(esc(c.getTelefono())).append("<br>");
        sb.append("</div>");

        sb.append("<div class=\"tipo\">").append(esc(c.getTipoDocumento())).append(" ELECTRÓNICA<br>")
                .append(esc(c.getSerie())).append(" - ").append(esc(c.getNumero())).append("</div>");

        sb.append("Fecha: ").append(esc(c.getFechaEmision())).append("&nbsp;&nbsp;Hora: ").append(esc(c.getHora())).append("<br>");
        sb.append("Cajero: ").append(esc(c.getCajero())).append("<br>");
        sb.append(esc(c.getTipoDocCliente())).append(": ").append(esc(c.getNroDocCliente())).append("<br>");
        sb.append("Cliente: ").append(esc(c.getNombreCliente())).append("<br>");
        if (!vacio(c.getDireccionCliente())) sb.append("Dirección: ").append(esc(c.getDireccionCliente())).append("<br>");
        sb.append("<hr>");

        sb.append("<table><tr><th>Cant.</th><th>Descripción</th><th class=\"der\">P. Unit.</th><th class=\"der\">Importe</th></tr>");
        if (c.getItems() != null) {
            for (ItemComprobante it : c.getItems()) {
                sb.append("<tr><td>").append(cantidad(it.getCantidad())).append("</td><td>")
                        .append(esc(it.getDescripcion())).append("</td><td class=\"der\">")
                        .append(dinero(it.getPrecioUnitario())).append("</td><td class=\"der\">")
                        .append(dinero(it.getSubtotal())).append("</td></tr>");
                if (it.getDescuento() != null && it.getDescuento().signum() > 0) {
                    sb.append("<tr><td></td><td colspan=\"3\" class=\"ahorro\">Ahorro por promoción: ")
                            .append(dinero(it.getDescuento())).append("</td></tr>");
                }
            }
        }
        sb.append("</table><hr>");

        sb.append("<table class=\"totales\">");
        sb.append("<tr><td>Op. gravadas</td><td class=\"der\">S/ ").append(dinero(c.getOpGravadas())).append("</td></tr>");
        sb.append("<tr><td>IGV (18%)</td><td class=\"der\">S/ ").append(dinero(c.getIgv())).append("</td></tr>");
        if (c.getTotalDescuento() != null && c.getTotalDescuento().signum() > 0) {
            sb.append("<tr><td class=\"ahorro\">Ahorro total</td><td class=\"der ahorro\">S/ ")
                    .append(dinero(c.getTotalDescuento())).append("</td></tr>");
        }
        sb.append("<tr class=\"total\"><td>TOTAL</td><td class=\"der\">S/ ").append(dinero(c.getTotal())).append("</td></tr>");
        sb.append("</table>");

        sb.append("<div class=\"son\">SON: ").append(esc(NumeroALetras.soles(c.getTotal()))).append("</div>");
        sb.append("<div class=\"centro\">¡Gracias por su compra!</div>");
        sb.append("</div>");
        if (imprimirAlAbrir) {
            sb.append("<script>window.onload=function(){window.print();};</script>");
        }
        sb.append("</body></html>");
        return sb.toString();
    }

    private static boolean vacio(String s) {
        return s == null || s.isBlank();
    }

    private static String dinero(BigDecimal v) {
        return (v == null ? BigDecimal.ZERO : v).setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private static String cantidad(Double v) {
        if (v == null) return "";
        return v == Math.floor(v) ? String.valueOf(v.longValue()) : String.valueOf(v);
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}