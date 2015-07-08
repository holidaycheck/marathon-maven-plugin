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

import mesosphere.marathon.client.Marathon;
import mesosphere.marathon.client.utils.MarathonException;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;

abstract class AbstractMarathonMojo extends AbstractMojo {

    @Parameter(defaultValue = "${session}", readonly = true)
    protected MavenSession session;

    @Parameter(defaultValue = "${mojoExecution}", readonly = true)
    protected MojoExecution execution;

    /**
     * Path to JSON file to write when processing Marathon config.
     * Default is ${project.build.directory}/marathon.json
     */
    @Parameter(property = "finalMarathonConfigFile",
            defaultValue = "${project.build.directory}/marathon.json")
    protected String finalMarathonConfigFile;
    
    protected boolean appExists(Marathon marathon, String appId) throws MojoExecutionException {
        try {
            marathon.getApp(appId);
            return true;
        } catch (MarathonException getAppException) {
            if (getAppException.getMessage().contains("404")) {
                return false;
            } else {
                throw new MojoExecutionException("Failed to check if an app " + appId + "exists",
                        getAppException);
            }
        } catch (Exception e) {
            throw new MojoExecutionException("Failed to check if an app " + appId + "exists", e);
        }
    }
}
