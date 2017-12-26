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
package org.gradle.launcher

import jline.console.ConsoleReader
import org.apache.commons.io.output.WriterOutputStream
import org.gradle.tooling.BuildException
import org.gradle.tooling.GradleConnector
import org.gradle.tooling.ProjectConnection
import org.gradle.tooling.model.GradleProject
import org.gradle.tooling.model.build.BuildEnvironment
import java.io.File
import java.io.OutputStream
import java.nio.charset.Charset
import java.util.concurrent.atomic.AtomicInteger

val splitter = " +".toRegex()

fun main(args: Array<String>) {
    val gradleArgs = setOf(*args)
    val connection = connect(File(".").canonicalFile)
    showBuildEnvironment(connection)
    println("Fetching task list (this may take a few seconds)...")
    println("Arguments: $gradleArgs")
    val project = fetchGradleProject(gradleArgs, connection)
    runBuildLoop(gradleArgs, connection, project, fetchTasks(project))
    connection.close()
    System.exit(0)
}

fun showBuildEnvironment(connection: ProjectConnection) {
    val buildEnv = buildEnvironment(connection)
    val version = findGradleVersion(buildEnv)
    println("Connected to Gradle $version")
    println("Java ${buildEnv.java.javaHome}")
    println("JVM arguments ${buildEnv.java.jvmArguments}")
}

private
fun findGradleVersion(buildEnvironment: BuildEnvironment) =
    buildEnvironment.gradle.gradleVersion

private fun buildEnvironment(connection: ProjectConnection) =
        connection.getModel(BuildEnvironment::class.java)

private
fun runBuildLoop(gradleArgs: Iterable<String>, connection: ProjectConnection, project: GradleProject, tasks: Set<String>) =
        ConsoleReader().apply {
            setPrompt("${project.name}  > ")
            addCompleter(Kompleter(tasks))
            WriterOutputStream(output, Charset.defaultCharset()).let { out ->
                eachLine {
                    try {
                        runBuild(connection, gradleArgs, split(splitter).dropLastWhile { it.isEmpty() }.toTypedArray(), out)
                    } catch (e: BuildException) {
                        System.err.println("Build failed with an exception")
                        e.printStackTrace()
                    }
                }
            }
        }


private
fun fetchTasks(project: GradleProject): Set<String> {
    val tasks = LinkedHashSet<String>()
    val projectcount = AtomicInteger()
    addTasks(project, tasks, projectcount)
    println("Found ${tasks.size} tasks in ${projectcount.get()} projects.")
    return tasks
}

private
fun fetchGradleProject(gradleArgs: Iterable<String>, connection: ProjectConnection): GradleProject =
        connection.model(GradleProject::class.java).withArguments(gradleArgs).get()

private
fun addTasks(project: GradleProject, tasks: MutableSet<String>, projectCount: AtomicInteger) {
    projectCount.incrementAndGet()
    for (task in project.tasks) {
        tasks.add(if (project.parent == null) task.name else task.path)
    }
    for (gradleProject in project.children) {
        addTasks(gradleProject, tasks, projectCount)
    }
}

private
fun connect(projectDirectory: File): ProjectConnection {
    return GradleConnector.newConnector()
            .forProjectDirectory(projectDirectory)
            .connect()
}

private
fun runBuild(connection: ProjectConnection, args: Iterable<String>, tasks: Array<String>, output: OutputStream) {
    connection
            .newBuild()
//            .setJvmArguments("-Xmx2g", "-Xms2g")
            .withArguments(args)
            .forTasks(*tasks)
            .setStandardOutput(output)
            .setStandardError(output)
            .setColorOutput(true)
            .run()
}

private
fun ConsoleReader.nextLine(action: String.() -> Unit): Boolean {
    val line = readLine()
    if (line != null) {
        action(line)
        return true
    }
    return false
}

private
fun ConsoleReader.eachLine(action: String.() -> Unit) {
    while (nextLine(action)) {
    }
}


