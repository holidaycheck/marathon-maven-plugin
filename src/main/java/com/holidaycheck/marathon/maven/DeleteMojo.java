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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Deletes an instance via Marathon.
 */
@Mojo(name = "delete", defaultPhase = LifecyclePhase.DEPLOY)
public class DeleteMojo extends AbstractMarathonMojo {

    /**
     * URL of the marathon host as specified in pom.xml.
     */
    @Parameter(property = "marathonHost", required = true)
    private String marathonHost;


    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        final Marathon marathon = MarathonClient.getInstance(marathonHost);
        final App app = readApp(finalMarathonConfigFile);
        getLog().info("deleting Marathon instance for " + app.getId());
        if (appExists(marathon, app.getId())) {
            getLog().info(app.getId() + " already exists - will be updated");
            deleteApp(marathon, app);
        } else {
            getLog().warn(app.getId() + " does not exist - nothing to delete");
        }
    }

    private void deleteApp(Marathon marathon, App app) throws MojoExecutionException {
        try {
            marathon.deleteApp(app.getId());
        } catch (Exception deleteAppException) {
            throw new MojoExecutionException("Failed to delete Marathon instance "
                    + marathonHost, deleteAppException);
        }
    }

}
