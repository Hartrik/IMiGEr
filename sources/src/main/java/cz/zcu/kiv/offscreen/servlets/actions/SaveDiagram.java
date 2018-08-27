package cz.zcu.kiv.offscreen.servlets.actions;

import com.google.common.base.Strings;
import cz.zcu.kiv.offscreen.user.DB;
import cz.zcu.kiv.offscreen.user.Diagram;
import org.apache.commons.lang.StringUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class SaveDiagram extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        // user is not logged in
        if (request.getSession().getAttribute("logged_user_id") == null) {
            response.sendError(response.SC_UNAUTHORIZED);
            return;
        }

        Integer loggedUserId = Integer.parseInt(request.getSession().getAttribute("logged_user_id").toString());
        String name = request.getParameter("name");
        String graphJson = request.getParameter("graph_json");
        String isPublic = StringUtils.defaultIfBlank(request.getParameter("public"), "0");

        // input parameters are invalid
        if (Strings.isNullOrEmpty(name) || Strings.isNullOrEmpty(graphJson)) {
            response.sendError(response.SC_BAD_REQUEST);
            return;
        }

        DB db = new DB(getServletContext());
        Diagram diagram;

        if (request.getParameter("diagram_id") == null) {
            // new diagram
            diagram = new Diagram(db);

        } else {
            // diagram exists
            Integer diagramId = Integer.parseInt(request.getParameter("diagram_id"));
            diagram = new Diagram(db, diagramId);

            // user is not owner of the diagram
            if (loggedUserId != diagram.getUserId()) {
                response.sendError(response.SC_UNAUTHORIZED);
                return;
            }
        }


        Map<String, String> diagramParams = new HashMap<>();
        diagramParams.put("name", name);
        diagramParams.put("public", isPublic);
        diagramParams.put("graph_json", graphJson);
        diagramParams.put("user_id", loggedUserId.toString());

        diagram.update(diagramParams);
    }
}