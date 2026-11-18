package com.utp.demo;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@WebServlet("/api/pedidos")
public class ApiPedidosServlet extends HttpServlet {

    public static class Pedido {
        int id;
        String cliente;
        String producto;
        double precio;
        String estado;
        String hora;

        public Pedido(int id, String cliente, String producto, double precio, String estado, String hora) {
            this.id = id;
            this.cliente = cliente;
            this.producto = producto;
            this.precio = precio;
            this.estado = estado;
            this.hora = hora;
        }
    }

    private static final List<Pedido> pedidos = new ArrayList<>();
    private static final AtomicInteger contadorId = new AtomicInteger(101);
    private static final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm:ss");

    static {
        pedidos.add(new Pedido(contadorId.getAndIncrement(), "Carlos Mendoza", "Cappuccino Vainilla", 12.50, "Listo", "09:30:15"));
        pedidos.add(new Pedido(contadorId.getAndIncrement(), "Ana Lucía Torres", "Frappuccino Mocha", 15.00, "En preparación", "09:42:10"));
    }

    private String escaparJson(String texto) {
        if (texto == null) return "";
        return texto.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json;charset=UTF-8");

        String appEnv = System.getenv("APP_ENV") != null ? System.getenv("APP_ENV") : "DESARROLLO (Localhost)";
        String dbUrl = System.getenv("DB_URL") != null ? System.getenv("DB_URL") : "jdbc:postgresql://localhost:5432/cafe_local";
        String dbUser = System.getenv("DB_USER") != null ? System.getenv("DB_USER") : "postgres_local";
        String dbPass = System.getenv("DB_PASS") != null ? "******** (Segura en Render)" : "No configurada (Modo Local)";
        String port = System.getenv("PORT") != null ? System.getenv("PORT") : "8080";
        String javaVer = System.getProperty("java.version");

        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"config\":{");
        json.append("\"appEnv\":\"").append(escaparJson(appEnv)).append("\",");
        json.append("\"dbUrl\":\"").append(escaparJson(dbUrl)).append("\",");
        json.append("\"dbUser\":\"").append(escaparJson(dbUser)).append("\",");
        json.append("\"dbPass\":\"").append(escaparJson(dbPass)).append("\",");
        json.append("\"port\":\"").append(escaparJson(port)).append("\",");
        json.append("\"javaVersion\":\"").append(escaparJson(javaVer)).append("\"");
        json.append("},");

        json.append("\"pedidos\":[");
        synchronized (pedidos) {
            for (int i = 0; i < pedidos.size(); i++) {
                Pedido p = pedidos.get(i);
                json.append("{");
                json.append("\"id\":").append(p.id).append(",");
                json.append("\"cliente\":\"").append(escaparJson(p.cliente)).append("\",");
                json.append("\"producto\":\"").append(escaparJson(p.producto)).append("\",");
                json.append("\"precio\":").append(p.precio).append(",");
                json.append("\"estado\":\"").append(escaparJson(p.estado)).append("\",");
                json.append("\"hora\":\"").append(escaparJson(p.hora)).append("\"");
                json.append("}");
                if (i < pedidos.size() - 1) json.append(",");
            }
        }
        json.append("]}");

        resp.getWriter().write(json.toString());
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json;charset=UTF-8");

        String accion = req.getParameter("accion");
        if ("cambiarEstado".equals(accion)) {
            int id = Integer.parseInt(req.getParameter("id"));
            synchronized (pedidos) {
                for (Pedido p : pedidos) {
                    if (p.id == id) {
                        p.estado = "Listo".equals(p.estado) ? "Entregado" : "Listo";
                        break;
                    }
                }
            }
            resp.getWriter().write("{\"ok\":true}");
            return;
        }

        if ("eliminar".equals(accion)) {
            int id = Integer.parseInt(req.getParameter("id"));
            synchronized (pedidos) {
                pedidos.removeIf(p -> p.id == id);
            }
            resp.getWriter().write("{\"ok\":true}");
            return;
        }

        String cliente = req.getParameter("cliente");
        String producto = req.getParameter("producto");
        String precioStr = req.getParameter("precio");

        if (cliente != null && !cliente.isBlank() && producto != null && !producto.isBlank()) {
            double precio = 10.0;
            try {
                precio = Double.parseDouble(precioStr);
            } catch (Exception ignored) {}

            Pedido nuevo = new Pedido(
                contadorId.getAndIncrement(),
                cliente.trim(),
                producto.trim(),
                precio,
                "En preparación",
                LocalTime.now().format(fmt)
            );
            synchronized (pedidos) {
                pedidos.add(0, nuevo);
            }
        }
        resp.getWriter().write("{\"ok\":true}");
    }
}
