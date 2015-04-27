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

import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.File;

@RunWith(JUnit4.class)
public abstract class AbstractMarathonMojoTestWithJUnit4 extends AbstractMojoTestCase {

    private String groupId;
    private String artifactId;
    private String version;

    @Before
    @Override
    public void setUp() throws Exception {
        File pluginPom = getTestFile("pom.xml");
        Xpp3Dom pluginPomDom = Xpp3DomBuilder.build(ReaderFactory.newXmlReader(pluginPom));
        artifactId = pluginPomDom.getChild("artifactId").getValue();
        groupId = resolveFromRootThenParent(pluginPomDom, "groupId");
        version = resolveFromRootThenParent(pluginPomDom, "version");
        super.setUp();
    }

    @After
    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }

    private String resolveFromRootThenParent(Xpp3Dom pluginPomDom, String element) throws Exception {
        Xpp3Dom elementDom = pluginPomDom.getChild(element);
        if (elementDom == null) {
            Xpp3Dom pluginParentDom = pluginPomDom.getChild("parent");
            if (pluginParentDom != null) {
                elementDom = pluginParentDom.getChild(element);
                if (elementDom == null) {
                    throw new Exception("unable to determine " + element);
                } else {
                    return elementDom.getValue();
                }
            } else {
                throw new Exception("unable to determine " + element);
            }
        } else {
            return elementDom.getValue();
        }
    }

    protected String getTestMarathonConfigFile() {
        return getClass().getResource("/marathon.json").getFile();
    }

    protected Mojo lookupMarathonMojo(String goal, PlexusConfiguration pluginCfg) throws Exception {
        return lookupMojo(groupId, artifactId, version, goal, pluginCfg);
    }


}
