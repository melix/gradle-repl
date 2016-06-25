/*
 * Copyright 2003-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gradle.launcher;

import jline.console.ConsoleReader;
import jline.console.completer.StringsCompleter;
import org.apache.commons.io.output.WriterOutputStream;
import org.gradle.internal.impldep.com.google.common.collect.Iterables;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.model.GradleProject;
import org.gradle.tooling.model.GradleTask;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class Main {
    public static void main(String[] args) throws IOException {
        Iterable<String> gradleArgs = Iterables.concat(Arrays.asList(args), Collections.singleton("-u"));
        System.out.println("Arguments: " + gradleArgs);
        ProjectConnection connection = connect(new File(".").getCanonicalFile());
        System.out.println("Fetching task list...");
        GradleProject project = fetchGradleProject(gradleArgs, connection);
        Set<String> tasks = fetchTasks(project);
        runBuildLoop(gradleArgs, connection, project, tasks);
        connection.close();
        System.exit(0);
    }

    private static void runBuildLoop(final Iterable<String> gradleArgs, final ProjectConnection connection, final GradleProject project, final Set<String> tasks) throws IOException {
        ConsoleReader reader = new ConsoleReader();
        WriterOutputStream output = new WriterOutputStream(reader.getOutput(), Charset.defaultCharset());
        reader.setPrompt(project.getName() + " > ");
        reader.addCompleter(new StringsCompleter(tasks));
        String line;
        while ((line = reader.readLine()) != null) {
            try {
                runBuild(connection, gradleArgs, line.split(" +"), output);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private static Set<String> fetchTasks(final GradleProject project) {
        Set<String> tasks = new LinkedHashSet<>();
        AtomicInteger projectcount = new AtomicInteger();
        addTasks(project, tasks, projectcount);
        System.out.println("Found " + tasks.size() + " tasks in " + projectcount.get() + " projects.");
        return tasks;
    }

    private static GradleProject fetchGradleProject(final Iterable<String> gradleArgs, final ProjectConnection connection) {
        return connection.model(GradleProject.class).withArguments(gradleArgs).get();
    }

    private static void addTasks(final GradleProject project, final Set<String> tasks, final AtomicInteger projectCount) {
        projectCount.incrementAndGet();
        for (GradleTask task : project.getTasks()) {
            tasks.add(project.getParent() == null ? task.getName() : task.getPath());
        }
        for (GradleProject gradleProject : project.getChildren()) {
            addTasks(gradleProject, tasks, projectCount);
        }
    }

    private static ProjectConnection connect(final File projectDirectory) {
        return GradleConnector.newConnector()
                .useInstallation(new File("/home/cchampeau/DEV/gradle-source-build"))
                .forProjectDirectory(projectDirectory)
                .connect();
    }

    private static void runBuild(ProjectConnection connection, Iterable<String> args, String[] tasks, OutputStream output) {
        connection
                .newBuild()
                .setJvmArguments("-Xmx2g", "-Xms2g")
                .withArguments(args)
                .forTasks(tasks)
                .setStandardOutput(output)
                .setStandardError(output)
                .setColorOutput(true)
                .run();
    }
}
