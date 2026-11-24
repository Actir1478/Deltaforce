package pe.edu.upeu.sysventas.config;

import org.h2.jdbcx.JdbcConnectionPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * Configuración de la base de datos H2 (embebida, en archivo o en memoria) con el pool de
 * conexiones propio de H2 ({@link JdbcConnectionPool}).
 *
 * <p>Se usa el pool de H2 en lugar de HikariCP porque HikariCP depende de reflexión interna
 * y no arranca dentro del ejecutable nativo (GraalVM native image).
 *
 * <p>Propiedades opcionales en {@code application.properties}:
 * <ul>
 *   <li>{@code db.url} (por defecto {@value #DEFAULT_URL})</li>
 *   <li>{@code db.username} (por defecto {@value #DEFAULT_USER})</li>
 *   <li>{@code db.password} (por defecto vacío)</li>
 *   <li>{@code db.ddl.auto} (por defecto {@code true})</li>
 *   <li>{@code db.ddl.script} (por defecto {@value #DEFAULT_SCRIPT})</li>
 * </ul>
 */
public final class DatabaseConfig {

    private static final Logger LOG = LoggerFactory.getLogger(DatabaseConfig.class);

    private static final String PROPERTIES_FILE = "application.properties";
    private static final String DEFAULT_URL = "jdbc:h2:file:./data/sysventas";
    private static final String DEFAULT_USER = "sa";
    private static final String DEFAULT_SCRIPT = "schema_sv.sql";

    private static volatile JdbcConnectionPool dataSource;

    private DatabaseConfig() {
    }

    /** Inicializa el pool y ejecuta el DDL una sola vez. */
    public static synchronized void init() {
        if (dataSource != null) {
            return;
        }
        try {
            Properties props = loadProperties();
            String url = resolverUrl(props.getProperty("db.url", DEFAULT_URL));

            JdbcConnectionPool ds = createDataSource(url, props);
            try {
                if (Boolean.parseBoolean(props.getProperty("db.ddl.auto", "true"))) {
                    // Se pueden indicar varios scripts separados por coma: primero las tablas, luego los datos
                    for (String script : props.getProperty("db.ddl.script", DEFAULT_SCRIPT).split(",")) {
                        if (!script.isBlank()) {
                            runScript(ds, script.trim());
                        }
                    }
                }
            } catch (RuntimeException e) {
                ds.dispose();
                throw e;
            }
            dataSource = ds;
            LOG.info("H2 listo: {}", url);
        } catch (RuntimeException e) {
            guardarError(e);
            throw e;
        }
    }

    public static DataSource getDataSource() {
        if (dataSource == null) {
            init();
        }
        return dataSource;
    }

    public static Connection getConnection() throws SQLException {
        return getDataSource().getConnection();
    }

    /** Cierra el pool. Una base H2 en memoria se pierde al cerrar su última conexión. */
    public static synchronized void shutdown() {
        if (dataSource != null) {
            dataSource.dispose();
            dataSource = null;
        }
    }

    /**
     * Carpeta de datos de la aplicación. En desarrollo (mvn / IDE) es {@code ./data}, relativa al
     * directorio de trabajo. Cuando corre empaquetada con jpackage (la propiedad
     * {@code jpackage.app-path} la define el lanzador) usa {@code <carpeta del usuario>/Coolbox/data},
     * porque la carpeta de instalación (Program Files) no permite escribir.
     */
    public static String dataDir() {
        if (System.getProperty("jpackage.app-path") != null) {
            return java.nio.file.Paths.get(System.getProperty("user.home"), "Coolbox", "data")
                    .toAbsolutePath().toString().replace('\\', '/');
        }
        return java.nio.file.Paths.get("data").toAbsolutePath().toString().replace('\\', '/');
    }

    private static String resolverUrl(String url) {
        if (url.startsWith("jdbc:h2:file:./data/")) {
            return "jdbc:h2:file:" + dataDir() + url.substring("jdbc:h2:file:./data".length());
        }
        return url;
    }

    private static JdbcConnectionPool createDataSource(String url, Properties props) {
        try {
            Class.forName("org.h2.Driver");
            JdbcConnectionPool pool = JdbcConnectionPool.create(
                    url,
                    props.getProperty("db.username", DEFAULT_USER),
                    props.getProperty("db.password", ""));
            // App de escritorio con un solo usuario: un pool pequeño es suficiente.
            pool.setMaxConnections(5);
            return pool;
        } catch (Throwable t) {
            throw new IllegalStateException("No fue posible abrir la base de datos H2: " + url, t);
        }
    }

    /** Guarda el error en un archivo junto al .exe (el ejecutable de ventana no muestra la consola). */
    private static void guardarError(Throwable t) {
        try (java.io.PrintWriter pw = new java.io.PrintWriter("error-h2.log", "UTF-8")) {
            t.printStackTrace(pw);
        } catch (Exception ignore) {
        }
    }

    private static Properties loadProperties() {
        try (InputStream in = DatabaseConfig.class.getResourceAsStream("/" + PROPERTIES_FILE)) {
            if (in == null) {
                throw new IllegalStateException("No se encontró " + PROPERTIES_FILE);
            }
            Properties props = new Properties();
            props.load(in);
            return props;
        } catch (IOException e) {
            throw new IllegalStateException("Error leyendo " + PROPERTIES_FILE, e);
        }
    }

    /** Ejecuta el script DDL (idempotente) sentencia por sentencia. */
    private static void runScript(DataSource ds, String script) {
        try (InputStream in = DatabaseConfig.class.getResourceAsStream("/" + script)) {
            if (in == null) {
                LOG.warn("No se encontró el script DDL {}", script);
                return;
            }
            String sql = new String(in.readAllBytes(), StandardCharsets.UTF_8)
                    .lines()
                    .filter(line -> !line.strip().startsWith("--"))
                    .collect(Collectors.joining("\n"));

            try (Connection conn = ds.getConnection(); Statement stmt = conn.createStatement()) {
                for (String statement : sql.split(";")) {
                    if (!statement.isBlank()) {
                        stmt.execute(statement);
                    }
                }
            }
            LOG.info("DDL ejecutado: {}", script);
        } catch (IOException | SQLException e) {
            throw new IllegalStateException("Error ejecutando el script " + script, e);
        }
    }
}