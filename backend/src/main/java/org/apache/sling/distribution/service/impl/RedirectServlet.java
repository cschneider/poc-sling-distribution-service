package org.apache.sling.distribution.service.impl;

import java.io.IOException;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

//import org.osgi.service.component.annotations.Component;
//import org.osgi.service.http.whiteboard.propertytypes.HttpWhiteboardServletPattern;

//@Component(service=Servlet.class)
//@HttpWhiteboardServletPattern("/index.html")
public class RedirectServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.addHeader("Location", "/ui/index.html");
        resp.setStatus(301);
    }

}
