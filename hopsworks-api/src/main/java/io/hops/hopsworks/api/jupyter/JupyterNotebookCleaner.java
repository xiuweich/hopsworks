/*
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

package io.hops.hopsworks.api.jupyter;

import io.hops.hopsworks.api.util.LivyController;
import io.hops.hopsworks.api.zeppelin.util.LivyMsg.Session;
import io.hops.hopsworks.common.dao.jupyter.JupyterProject;
import io.hops.hopsworks.common.dao.jupyter.config.JupyterProcessMgr;
import io.hops.hopsworks.common.dao.jupyter.config.JupyterFacade;
import io.hops.hopsworks.common.dao.project.service.ProjectServiceEnum;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Timer;
import java.sql.Date;
import java.util.List;

@Singleton
public class JupyterNotebookCleaner {

  private final static Logger LOGGER = Logger.getLogger(
      JupyterNotebookCleaner.class.getName());

  public final int connectionTimeout = 90 * 1000;// 30 seconds

  public int sessionTimeoutMs = 30 * 1000;//30 seconds

  @EJB
  private LivyController livyService;
  @EJB
  private JupyterFacade jupyterFacade;
  @EJB
  private JupyterProcessMgr jupyterProcessFacade;

  public JupyterNotebookCleaner() {
  }

  // Run once per hour
  @Schedule(persistent = false,
      minute = "0",
      hour = "*")
  public void execute(Timer timer) {

    // 1. Get all Running Jupyter Notebook Servers
    List<JupyterProject> servers = jupyterFacade.getAllNotebookServers();

    if (servers != null) {
      // 2. For each running Notebook Server, get the project_user and
      // then get the Livy sessions for that project_user
      for (JupyterProject jp : servers) {
        List<Session> sessions = livyService.getLivySessions(jp.getProjectId(), ProjectServiceEnum.JUPYTER);
        // 3. If there is an active livy session, update the lastModified column
        if (!sessions.isEmpty()) {
          jp.setLastAccessed(new Date(System.currentTimeMillis()));
          jupyterFacade.update(jp);
        }
        // 3a. TODO - Check if there is an active Python kernel for the notebook

        // If notebook hasn't been used in the last 2 hours, kill it.
        if (jp.getLastAccessed().before(
            new Date(System.currentTimeMillis() - (2 * 60 * 60 * 1000)))) {
        }

      }

      List<JupyterProject> notebooks = jupyterProcessFacade.getAllNotebooks();
      for (JupyterProject jp : notebooks) {
        if (!jupyterProcessFacade.pingServerJupyterUser(jp.getPid())) {
//          jupyterProcessFacade.killOrphanedWithPid(jp.getPid());
          int hdfsId = jp.getHdfsUserId();
//          String hdfsUser = hdfsUsersFacade.
//          jupyterProcessFacade.stopCleanly();
        }
      }

    } else {
      LOGGER.info("No Jupyter Notebook Servers running. Sleeping again.");
    }
  }

}
