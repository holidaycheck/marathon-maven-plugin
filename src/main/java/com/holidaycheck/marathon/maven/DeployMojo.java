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
import mesosphere.marathon.client.model.v2.Deployment;

import javax.annotation.Nonnull;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Deploys via Marathon by sending config.
 */
@Mojo(name = "deploy", defaultPhase = LifecyclePhase.DEPLOY)
public class DeployMojo extends AbstractMarathonMojo {

    /**
     * URL of the marathon host as specified in pom.xml.
     */
    @Parameter(property = "marathonHost", required = true)
    private String marathonHost;

    @Parameter(property = "waitForDeploymentFinished", required = false)
    private boolean waitForDeploymentFinished = false;

    @Parameter(property = "waitForDeploymentTimeout", required = false)
    private Long waitForDeploymentTimeout = 10L;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        final Marathon marathon = MarathonClient.getInstance(marathonHost);
        final App app = readApp(finalMarathonConfigFile);
        getLog().info("deploying Marathon config for " + app.getId()
                + " from " + finalMarathonConfigFile + " to " + marathonHost);
        if (appExists(marathon, app.getId())) {
            getLog().info(app.getId() + " already exists - will be updated");
            updateApp(marathon, app);
        } else {
            getLog().info(app.getId() + " does not exist yet - will be created");
            final App createdApp = createApp(marathon, app);

            if (waitForDeploymentFinished) {
                waitForApp(marathon, createdApp);
            }
        }
    }

    private void updateApp(Marathon marathon, App app) throws MojoExecutionException {
        try {
            marathon.updateApp(app.getId(), app, true);
        } catch (Exception updateAppException) {
            throw new MojoExecutionException("Failed to update Marathon config file at "
                    + marathonHost, updateAppException);
        }
    }

    private App createApp(Marathon marathon, App app) throws MojoExecutionException {
        try {
            return marathon.createApp(app);
        } catch (Exception createAppException) {
            throw new MojoExecutionException("Failed to push Marathon config file to "
                    + marathonHost, createAppException);
        }
    }

    /**
     * Get the marathon deployments in a loop until we find our deployment is
     * completed or we have a timeout.
     */
    private void waitForApp(Marathon marathon, App app) {
        //transform our app deployments into a collection of ids
        final Collection<String> appDeploymentIds = Collections2.transform(
                app.getDeployments(), new DeploymentIdExtractor());

        //capture our start time
        final long start = new Date().getTime();

        //loop until we time out.  if we are successful then the loop will exit
        while (new Date().getTime() - start < waitForDeploymentTimeout * 1000) {
            //get the list of active deployment ids filtered by our app id 
            final Collection<String> activeDeploymentIds = 
                    Collections2.transform(Collections2.filter(
                            marathon.getDeployments(), new SpecificAppPredicate(app.getId())),
                            new DeploymentIdExtractor());

            //match our app's deployment ids against the list of active ones
            //  if none of our ids are in active deployment, then we have started up.
            if (Collections.disjoint(appDeploymentIds, activeDeploymentIds)) {
                getLog().info("All deployments are started: " + appDeploymentIds);
                return;
            }
            
            getLog().debug("Deployment still found for at least one deployment id");
        }

        //we normally exited the while loop, so we have a timeout.
        getLog().warn("Timeout waiting for deployment: " + appDeploymentIds);
    }

    private static class DeploymentIdExtractor implements Function<Deployment, String> {
        @Override
        public String apply(@Nonnull final Deployment input) {
            return input.getId();
        }
    }

    private static class SpecificAppPredicate implements Predicate<Deployment> {
        
        final String appId;

        public SpecificAppPredicate(final String appId) {
            this.appId = appId;
        }

        @Override
        public boolean apply(@Nonnull final Deployment input) {
            return input.getAffectedApps().contains(this.appId);
        }
    }

}
