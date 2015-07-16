/*
 * Copyright (c) 2015 HolidayCheck AG.
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.holidaycheck.marathon.maven;

import static com.holidaycheck.marathon.maven.Utils.readApp;

import mesosphere.marathon.client.Marathon;
import mesosphere.marathon.client.MarathonClient;
import mesosphere.marathon.client.model.v2.App;
import mesosphere.marathon.client.model.v2.GetAppTasksResponse;
import mesosphere.marathon.client.model.v2.Task;

import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 * Sets maven parameters based on the app tasks.
 * 
 * For each task there will be at least two properties set, a &lt;prefix&gt;host 
 * and a &lt;prefix&gt;port followed by the index of the task (starting with 0).  
 * There can be many ports within a service, so the port property will have an 
 * additional -index set (starting with 0).
 * 
 * Ex:
 * prefix-host0 = somehost
 * prefix-port0-0 = 80
 * prefix-port0-1 = 443
 */
@Mojo(name = "apptasks", defaultPhase = LifecyclePhase.DEPLOY)
public class AppTasksMojo extends AbstractMarathonMojo {

    /**
     * URL of the marathon host as specified in pom.xml.
     */
    @Parameter(property = "marathonHost", required = true)
    private String marathonHost;

    /**
     * Prefix to use for the parameters.
     */
    @Parameter(property = "propertyPrefix", required = true)
    private String propertyPrefix;

    /**
     * Delay for N seconds before requesting tasks.  This is important because it could
     * take a while for your service to be provisioned and return something meaningful.
     */
    @Parameter(property = "delay", required = false)
    private long delay = 0;

    @Component
    private MavenProject project;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        final Marathon marathon = MarathonClient.getInstance(marathonHost);
        final App app = readApp(finalMarathonConfigFile);
        getLog().info("tasks in Marathon instance for " + app.getId());

        final ScheduledThreadPoolExecutor executor
                = new ScheduledThreadPoolExecutor(1);
        final Callable<Void> callable = new Callable<Void>() {
            @Override
            public Void call() {
                try {
                    if (appExists(marathon, app.getId())) {
                        getLog().info(app.getId() + " exists - getting app tasks");
                        getAppTasks(marathon, app);
                    } else {
                        getLog().warn(app.getId() + " does not exist");
                    }
                    return null;
                } catch (final MojoExecutionException e) {
                    getLog().error("Problem communicating with Marathon", e);
                }
                return null;
            }
        };
        final ScheduledFuture<Void> future = executor.schedule(callable, delay, TimeUnit.SECONDS);
        while (!future.isDone() && !future.isCancelled()) {
            //do nothing
        }
    }

    private void getAppTasks(Marathon marathon, App app) throws MojoExecutionException {
        try {
            final GetAppTasksResponse getAppTasksResponse = marathon.getAppTasks(app.getId());
            int taskCount = 0;
            for (final Task task : getAppTasksResponse.getTasks()) {
                final String hostPropertyName = propertyPrefix + "host" + taskCount;
                project.getProperties().put(hostPropertyName, task.getHost());
                getLog().info("Setting " + hostPropertyName + " = " + task.getHost());
                int portCount = 0;
                for (final Integer port : task.getPorts()) {
                    final String portPropertyName = propertyPrefix + "port"
                            + taskCount + "-" + portCount;
                    project.getProperties().put(portPropertyName, String.valueOf(port));
                    getLog().info("Setting " + portPropertyName + " = " + port);
                    portCount++;
                }
                taskCount++;
            }
        } catch (Exception deleteAppException) {
            throw new MojoExecutionException("Failed to get tasks for Marathon instance "
                    + marathonHost, deleteAppException);
        }
    }

}
