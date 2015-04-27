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


import static org.hamcrest.CoreMatchers.isA;

import java.io.FileNotFoundException;

import mesosphere.marathon.client.model.v2.App;
import mesosphere.marathon.client.utils.MarathonException;
import mesosphere.marathon.client.utils.ModelUtils;

import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.configuration.DefaultPlexusConfiguration;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.RecordedRequest;
import com.squareup.okhttp.mockwebserver.rule.MockWebServerRule;


public class DeployMojoTest extends AbstractMarathonMojoTestWithJUnit4 {

    public static final String APP_ID = "/example-1";
    public static final String MARATHON_PATH = "/v2/apps";

    @Rule
    public final ExpectedException thrown = ExpectedException.none();
    @Rule
    public final MockWebServerRule server = new MockWebServerRule();

    private String getMarathonHost() {
        return server.getUrl("").toString();
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
        assertEquals(MARATHON_PATH + APP_ID + "?force=true", updateAppRequest.getPath());
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

}
