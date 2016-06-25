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
import static com.holidaycheck.marathon.maven.Utils.writeApp;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import mesosphere.marathon.client.model.v2.App;

import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.filtering.MavenFilteringException;
import org.apache.maven.shared.filtering.MavenResourcesExecution;
import org.apache.maven.shared.filtering.MavenResourcesFiltering;

/**
 * Used to process Marathon config file.
 */
@Mojo(name = "processConfig", defaultPhase = LifecyclePhase.VERIFY)
public class ProcessConfigMojo extends AbstractMarathonMojo {

    /**
     * Path to JSON file to read from when processing Marathon config.
     * Default is ${basedir}/marathon.json
     */
    @Parameter(property = "sourceMarathonConfigFile",
            defaultValue = "${basedir}/marathon.json")
    private String sourceMarathonConfigFile;

    /**
     * Image name as specified in pom.xml.
     */
    @Parameter(property = "image", required = true)
    private String image;

    /**
     * ID to use for the Marathon config.
     */
    @Parameter(property = "id", required = false)
    private String id;

    @Parameter(property = "encoding", defaultValue = "${project.build.sourceEncoding}")
    protected String encoding;

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    private MavenProject project;

    @Component(role = MavenResourcesFiltering.class, hint = "default")
    protected MavenResourcesFiltering resourcesFiltering;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().info("processing Marathon config file from " + sourceMarathonConfigFile
                + " to " + finalMarathonConfigFile);
        App app = readApp(filterSourceFile(sourceMarathonConfigFile));
        if (id != null) {
            app.setId(id);
        }
        app.getContainer().getDocker().setImage(image);
        writeApp(app, finalMarathonConfigFile);
    }

    private String filterSourceFile(final String inputFile) {
        final File targetDir = new File(System.getProperty("java.io.tmpdir"));
        final File sourceFile = new File(inputFile);
        final Resource resource = new Resource();
        resource.setDirectory(sourceFile.getParent());
        resource.getIncludes().add("**" + File.separator + sourceFile.getName());
        resource.setFiltering(true);
        final List<String> filters = Collections.emptyList();
        final List<String> nonFilteredFileExtensions = Collections.emptyList();
        try {
            resourcesFiltering.filterResources(new MavenResourcesExecution(Arrays.asList(resource),
                    targetDir, project, encoding, filters, nonFilteredFileExtensions, session));
            return targetDir + File.separator + sourceFile.getName();
        } catch (final NullPointerException | MavenFilteringException e) {
            getLog().warn("resource filtering of Marathon config file failed", e);
            return inputFile;
        }
    }

}
