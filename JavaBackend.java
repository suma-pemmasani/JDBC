import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.sql.*;

public class JavaBackend {
    private static final String URL = "jdbc:mysql://localhost:3306/enterprise_db";
    private static final String USER = "root";
    private static final String PASSWORD = "password123"; 

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/api/employees", new EmployeesHandler());
        server.setExecutor(null); 
        System.out.println("Java JDBC Service running on http://localhost:8080/api/employees");
        server.start();
    }

    static class EmployeesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().set("Content-Type", "application/json");

            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                StringBuilder jsonResponse = new StringBuilder("[");
                String query = "SELECT id, name, role, salary FROM employees";

                try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
                     PreparedStatement stmt = conn.prepareStatement(query);
                     ResultSet rs = stmt.executeQuery()) {

                    boolean first = true;
                    while (rs.next()) {
                        if (!first) jsonResponse.append(",");
                        jsonResponse.append(String.format(
                            "{\"id\":%d,\"name\":\"%s\",\"role\":\"%s\",\"salary\":%.2f}",
                            rs.getInt("id"),
                            rs.getString("name"),
                            rs.getString("role"),
                            rs.getDouble("salary")
                        ));
                        first = false;
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                    String err = "{\"error\":\"Database connection failed\"}";
                    exchange.sendResponseHeaders(500, err.length());
                    try (OutputStream os = exchange.getResponseBody()) { os.write(err.getBytes()); }
                    return;
                }
                jsonResponse.append("]");

                byte[] responseBytes = jsonResponse.toString().getBytes();
                exchange.sendResponseHeaders(200, responseBytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(responseBytes);
                }
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        }
    }
}
