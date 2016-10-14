package org.k;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public class DirlistContextListener implements ServletContextListener {
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        sce.getServletContext().getContextPath();
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {

    }
}
