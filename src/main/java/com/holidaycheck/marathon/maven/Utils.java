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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;

import mesosphere.marathon.client.model.v2.App;
import mesosphere.marathon.client.utils.ModelUtils;

import org.apache.maven.plugin.MojoExecutionException;

import com.google.common.base.Charsets;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

public class Utils {

    private Utils() {
    }

    public static final App readApp(String file) throws MojoExecutionException {
        try (Reader reader = new InputStreamReader(new FileInputStream(new File(file)),
                Charsets.UTF_8)) {
            return ModelUtils.GSON.fromJson(reader, App.class);
        } catch (FileNotFoundException e) {
            throw new MojoExecutionException("Marathon config file not found at " + file, e);
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to read Marathon config file", e);
        } catch (JsonSyntaxException e) {
            throw new MojoExecutionException("Failed to parse Marathon config file", e);
        }
    }

    public static final void writeApp(App app, String file) throws MojoExecutionException {
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(new File(file)),
                Charsets.UTF_8)) {
            ModelUtils.GSON.toJson(app, writer);
            writer.flush();
        } catch (FileNotFoundException e) {
            throw new MojoExecutionException("Marathon config file cannot be written at "
                    + file, e);
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to write Marathon config file", e);
        } catch (JsonIOException e) {
            throw new MojoExecutionException("Failed to serialize Marathon config file", e);
        }
    }
}
