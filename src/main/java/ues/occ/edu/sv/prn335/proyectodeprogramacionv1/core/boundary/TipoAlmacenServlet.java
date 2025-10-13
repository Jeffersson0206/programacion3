package ues.occ.edu.sv.prn335.proyectodeprogramacionv1.core.boundary;

import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import ues.occ.edu.sv.prn335.proyectodeprogramacionv1.core.control.TipoAlmacenDAO;
import ues.occ.edu.sv.prn335.proyectodeprogramacionv1.core.entity.TipoAlmacen;

import java.io.IOException;
import java.io.PrintWriter;

@WebServlet(name = "TipoAlmacenamiento", urlPatterns = "web/tipo_almacen")
public class TipoAlmacenServlet extends HttpServlet {

    @Inject
    TipoAlmacenDAO taDao;

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // Fragmentos HTML para construir la respuesta
        String arriba = """
                <html>
                <head>
                </head>
                <body>
                """;

        String abajo = """
                </body>
                </html>        
                """;

        StringBuilder sb = new StringBuilder();
        String nombre = req.getParameter("nombre");

        // Validación básica del parámetro recibido
        if (nombre == null) {
            sb.append("<p>Debe agregar el nombre del tipo de almacen</p>");
        } else {
            // Construcción del objeto y simulación de persistencia
            TipoAlmacen tipoAlmacen = new TipoAlmacen();
            tipoAlmacen.setNombre(nombre);
            tipoAlmacen.setActivo(true);
            tipoAlmacen.setObsevaciones("creado en clase 26 de agosto 2025");

            try {
                // Persistencia omitida: falta taDao.crear(tipoAlmacen);
                sb.append("<p>Agregado con éxito</p>");
                taDao.crear(tipoAlmacen);
            } catch (Exception ex) {
                sb.append("<p>Error: ").append(ex.getMessage()).append("</p>");
            }
        }

        // Envío de respuesta HTML al cliente
        try {
            PrintWriter writer = resp.getWriter();
            writer.println(arriba);
            writer.println(sb.toString());
            writer.println(abajo);
            writer.flush();
            writer.close();
        } catch (Exception ex) {
            // Silenciado: no se reporta el error al usuario ni al log
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // Renderiza un formulario HTML simple para crear un nuevo TipoAlmacen
        String htmlForm = """
                <html>
                <head>
                    <title>Agregar Tipo de Almacen</title>
                </head>
                <body>
                    <h2>Crear Nuevo Tipo de Almacen</h2>
                    <form action="web/tipo_almacen" method="post">
                        <label for="nombre">Nombre del tipo de almacen:</label><br>
                        <input type="text" id="nombre" name="nombre" required><br><br>
                        <input type="submit" value="Guardar">
                    </form>
                </body>
                </html>
                """;

        resp.setContentType("text/html");
        PrintWriter writer = resp.getWriter();
        writer.println(htmlForm);
    }
}
