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
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.model.GradleProject;
import org.gradle.tooling.model.GradleTask;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.LinkedHashSet;
import java.util.Set;

public class Main {
    public static void main(String[] args) throws IOException {
        ProjectConnection connection = connect(new File(".").getCanonicalFile());
        GradleProject project = connection.getModel(GradleProject.class);
        ConsoleReader reader = new ConsoleReader();
        WriterOutputStream output = new WriterOutputStream(reader.getOutput(), Charset.defaultCharset());
        reader.setPrompt(project.getName() + " > ");
        Set<String> tasks = new LinkedHashSet<>();
        addTasks(project, tasks);
        reader.addCompleter(new StringsCompleter(tasks));
        String line;
        while ((line = reader.readLine()) != null) {
            runBuild(connection, line.split(" +"), output);
        }
        connection.close();
        System.exit(0);
    }

    private static void addTasks(final GradleProject project, final Set<String> tasks) {
        for (GradleTask task : project.getTasks()) {
            tasks.add(project.getParent() == null ? task.getName() : task.getPath());
        }
        for (GradleProject gradleProject : project.getChildren()) {
            addTasks(gradleProject, tasks);
        }
    }

    private static ProjectConnection connect(final File projectDirectory) {
        return GradleConnector.newConnector()
                .useInstallation(new File("/home/cchampeau/DEV/gradle-source-build"))
                .forProjectDirectory(projectDirectory)
                .connect();
    }

    private static void runBuild(ProjectConnection connection, String[] args, OutputStream output) {
        connection
                .newBuild()
                .setJvmArguments("-Xmx2g", "-Xms2g")
                .withArguments("-u")
                .forTasks(args)
                .setStandardOutput(output)
                .setStandardError(output)
                .setColorOutput(true)
                .run();
    }
}
