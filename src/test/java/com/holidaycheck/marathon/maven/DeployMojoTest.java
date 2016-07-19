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


import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.MockWebServer;
import com.squareup.okhttp.mockwebserver.RecordedRequest;
import mesosphere.marathon.client.model.v2.App;
import mesosphere.marathon.client.utils.MarathonException;
import mesosphere.marathon.client.utils.ModelUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.configuration.DefaultPlexusConfiguration;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.FileNotFoundException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.isA;


public class DeployMojoTest extends AbstractMarathonMojoTestWithJUnit4 {

    private static final String APP_ID = "/example-1";
    private static final String MARATHON_PATH = "/v2/apps";

    @Rule
    public final ExpectedException thrown = ExpectedException.none();
    @Rule
    public final MockWebServer server = new MockWebServer();

    private String getMarathonHost() {
        return StringUtils.removeEnd(server.url("").toString(), "/");
    }

    private DeployMojo lookupDeployMojo(String marathonFile) throws Exception {
        PlexusConfiguration pluginCfg = new DefaultPlexusConfiguration("configuration");
        pluginCfg.addChild("marathonHost", getMarathonHost());
        pluginCfg.addChild("finalMarathonConfigFile", marathonFile);
        return (DeployMojo) lookupMarathonMojo("deploy", pluginCfg);
    }

    private DeployMojo lookupDeployMojo() throws Exception {
        return lookupDeployMojo(getTestMarathonConfigFile());
    }

    @Test
    public void testSuccessfulDeployAppNotCreatedYet() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(404));
        server.enqueue(new MockResponse().setResponseCode(200));

        final DeployMojo mojo = lookupDeployMojo();
        assertNotNull(mojo);

        mojo.execute();

        assertEquals(2, server.getRequestCount());

        RecordedRequest getAppRequest = server.takeRequest();
        assertEquals(MARATHON_PATH + APP_ID, getAppRequest.getPath());
        assertEquals("GET", getAppRequest.getMethod());

        RecordedRequest createAppRequest = server.takeRequest();
        assertEquals(MARATHON_PATH, createAppRequest.getPath());
        assertEquals("POST", createAppRequest.getMethod());
        App requestApp = ModelUtils.GSON.fromJson(createAppRequest.getBody().readUtf8(), App.class);
        assertNotNull(requestApp);
        assertEquals(APP_ID, requestApp.getId());
    }

    @Test
    public void testSuccessfulDeployAppAlreadyExists() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200));
        server.enqueue(new MockResponse().setResponseCode(200));

        final DeployMojo mojo = lookupDeployMojo();
        assertNotNull(mojo);

        mojo.execute();

        assertEquals(2, server.getRequestCount());

        RecordedRequest getAppRequest = server.takeRequest();
        assertEquals(MARATHON_PATH + APP_ID, getAppRequest.getPath());
        assertEquals("GET", getAppRequest.getMethod());

        RecordedRequest updateAppRequest = server.takeRequest();
        assertEquals(MARATHON_PATH + APP_ID, updateAppRequest.getPath());
        assertEquals("PUT", updateAppRequest.getMethod());
        App requestApp = ModelUtils.GSON.fromJson(updateAppRequest.getBody().readUtf8(), App.class);
        assertNotNull(requestApp);
        assertEquals(APP_ID, requestApp.getId());
    }

    @Test
    public void testDeployFailedDueToMissingMarathonConfigFile() throws Exception {
        thrown.expect(MojoExecutionException.class);
        thrown.expectCause(isA(FileNotFoundException.class));

        final DeployMojo mojo = lookupDeployMojo("/invalid/path/to/marathon.json");
        assertNotNull(mojo);

        mojo.execute();

        assertEquals(0, server.getRequestCount());
    }

    @Test
    public void testDeployFailedDueToFailedAppStatusCheck() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(500));
        thrown.expect(MojoExecutionException.class);
        thrown.expectCause(isA(MarathonException.class));

        final DeployMojo mojo = lookupDeployMojo();
        assertNotNull(mojo);

        mojo.execute();

        assertEquals(1, server.getRequestCount());

        RecordedRequest getAppRequest = server.takeRequest();
        assertEquals(MARATHON_PATH + APP_ID, getAppRequest.getPath());
        assertEquals("GET", getAppRequest.getMethod());
    }

    @Test
    public void testDeployFailedDueToFailedAppCreation() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200));
        server.enqueue(new MockResponse().setResponseCode(500));
        thrown.expect(MojoExecutionException.class);
        thrown.expectCause(isA(MarathonException.class));

        final DeployMojo mojo = lookupDeployMojo();
        assertNotNull(mojo);

        mojo.execute();

        assertEquals(2, server.getRequestCount());

        RecordedRequest getAppRequest = server.takeRequest();
        assertEquals(MARATHON_PATH + APP_ID, getAppRequest.getPath());
        assertEquals("GET", getAppRequest.getMethod());

        RecordedRequest createAppRequest = server.takeRequest();
        assertEquals(MARATHON_PATH, createAppRequest.getPath());
        assertEquals("POST", createAppRequest.getMethod());
    }

    @Test
    public void testDeployFailedDueToFailedAppUpdate() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(404));
        server.enqueue(new MockResponse().setResponseCode(500));
        thrown.expect(MojoExecutionException.class);
        thrown.expectCause(isA(MarathonException.class));

        final DeployMojo mojo = lookupDeployMojo();
        assertNotNull(mojo);

        mojo.execute();

        assertEquals(2, server.getRequestCount());

        RecordedRequest getAppRequest = server.takeRequest();
        assertEquals(MARATHON_PATH + APP_ID, getAppRequest.getPath());
        assertEquals("GET", getAppRequest.getMethod());

        RecordedRequest updateAppRequest = server.takeRequest();
        assertEquals(MARATHON_PATH + APP_ID + "?force=true", updateAppRequest.getPath());
        assertEquals("PUT", updateAppRequest.getMethod());
    }
    
    @Test
    public void testDeployWithWait() throws Exception {
        final UUID deploymentId = UUID.randomUUID();
        
        server.enqueue(new MockResponse().setResponseCode(404)); //does the app exist
        server.enqueue(new MockResponse().setResponseCode(200)
            .setBody("{ \"id\": \"" + APP_ID + "\", \"deployments\": [ { \"id\": \"" + deploymentId + "\" } ] }")); //create the app
        server.enqueue(new MockResponse().setResponseCode(200)
                .setBody("[ { \"affectedApps\": [ \"" + APP_ID + "\" ], \"id\": \"" + deploymentId + "\" } ]")); //get the current deployments containing ids
        server.enqueue(new MockResponse().setResponseCode(200)
                .setBody("[]")); //get the current deployments excluding ids

        final PlexusConfiguration pluginCfg = new DefaultPlexusConfiguration("configuration");
        pluginCfg.addChild("marathonHost", getMarathonHost());
        pluginCfg.addChild("finalMarathonConfigFile", getTestMarathonConfigFile());
        pluginCfg.addChild("waitForDeploymentFinished", "true");
        final DeployMojo mojo = (DeployMojo) lookupMarathonMojo("deploy", pluginCfg);
        assertNotNull(mojo);

        mojo.execute();

        assertEquals(4, server.getRequestCount());

        RecordedRequest getAppRequest = server.takeRequest();
        assertEquals(MARATHON_PATH + APP_ID, getAppRequest.getPath());
        assertEquals("GET", getAppRequest.getMethod());

        RecordedRequest createAppRequest = server.takeRequest();
        assertEquals(MARATHON_PATH, createAppRequest.getPath());
        assertEquals("POST", createAppRequest.getMethod());
        App requestApp = ModelUtils.GSON.fromJson(createAppRequest.getBody().readUtf8(), App.class);
        assertNotNull(requestApp);
        assertEquals(APP_ID, requestApp.getId());
        
        RecordedRequest getDeploymentsRequest1 = server.takeRequest();
        assertEquals("/v2/deployments", getDeploymentsRequest1.getPath());
        assertEquals("GET", getDeploymentsRequest1.getMethod());
        
        RecordedRequest getDeploymentsRequest2 = server.takeRequest();
        assertEquals("/v2/deployments", getDeploymentsRequest2.getPath());
        assertEquals("GET", getDeploymentsRequest2.getMethod());
    }
    
    @Test
    public void testDeployWithTimeout() throws Exception {
        final UUID deploymentId = UUID.randomUUID();
        
        server.enqueue(new MockResponse().setResponseCode(404)); //does the app exist
        server.enqueue(new MockResponse().setResponseCode(200)
            .setBody("{ \"id\": \"" + APP_ID + "\", \"deployments\": [ { \"id\": \"" + deploymentId + "\" } ] }")); //create the app
        final String deploymentsBody = "[ { \"affectedApps\": [ \"" + APP_ID 
                + "\" ], \"id\": \"" + deploymentId + "\" } ]";
        server.enqueue(new MockResponse().setResponseCode(200)
                .setBodyDelay(2, TimeUnit.SECONDS) //take two seconds for the response
                .setBody(deploymentsBody)); //get the current deployments containing ids

        final PlexusConfiguration pluginCfg = new DefaultPlexusConfiguration("configuration");
        pluginCfg.addChild("marathonHost", getMarathonHost());
        pluginCfg.addChild("finalMarathonConfigFile", getTestMarathonConfigFile());
        pluginCfg.addChild("waitForDeploymentFinished", "true");
        pluginCfg.addChild("waitForDeploymentTimeout", "1");
        final DeployMojo mojo = (DeployMojo) lookupMarathonMojo("deploy", pluginCfg);
        assertNotNull(mojo);

        mojo.execute();

        assertEquals(3, server.getRequestCount());

        RecordedRequest getAppRequest = server.takeRequest();
        assertEquals(MARATHON_PATH + APP_ID, getAppRequest.getPath());
        assertEquals("GET", getAppRequest.getMethod());

        RecordedRequest createAppRequest = server.takeRequest();
        assertEquals(MARATHON_PATH, createAppRequest.getPath());
        assertEquals("POST", createAppRequest.getMethod());
        App requestApp = ModelUtils.GSON.fromJson(createAppRequest.getBody().readUtf8(), App.class);
        assertNotNull(requestApp);
        assertEquals(APP_ID, requestApp.getId());
        
        RecordedRequest getDeploymentsRequest1 = server.takeRequest();
        assertEquals("/v2/deployments", getDeploymentsRequest1.getPath());
        assertEquals("GET", getDeploymentsRequest1.getMethod());
    }

}
