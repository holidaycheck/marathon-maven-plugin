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

import mesosphere.marathon.client.model.v2.App;

import org.codehaus.plexus.configuration.DefaultPlexusConfiguration;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.junit.Test;

public class ProcessConfigMojoTest extends AbstractMarathonMojoTestWithJUnit4 {

    private static final String IMAGE = "dummyImageName";

    private String getProcessedMarathonConfigFile() {
        return getTestPath("target") + "/marathon.json";
    }

    private ProcessConfigMojo lookupProcessConfigMojo() throws Exception {
        PlexusConfiguration pluginCfg = new DefaultPlexusConfiguration("configuration");
        pluginCfg.addChild("sourceMarathonConfigFile", getTestMarathonConfigFile());
        pluginCfg.addChild("finalMarathonConfigFile", getProcessedMarathonConfigFile());
        pluginCfg.addChild("image", IMAGE);
        return (ProcessConfigMojo) lookupMarathonMojo("processConfig", pluginCfg);
    }

    @Test
    public void testProcessConfig() throws Exception {
        ProcessConfigMojo mojo = lookupProcessConfigMojo();
        assertNotNull(mojo);

        mojo.execute();

        App app = Utils.readApp(getProcessedMarathonConfigFile());
        assertNotNull(app);

        String image = app.getContainer().getDocker().getImage();
        assertNotNull(image);
        assertTrue(image.contains(IMAGE));
    }

}
