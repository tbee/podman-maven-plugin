package org.tbee.podman.podmanMavenPlugin;

/*-
 * #%L
 * podman-maven-plugin
 * %%
 * Copyright (C) 2020 - 2026 Tom Eugelink
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.xml.Xpp3Dom;

/// Goal that recreates a pod and starts the configured containers in that pod.
/// Example usage:
/// ```java
///                     <plugin>
///                         <groupId>org.tbee</groupId>
///                         <artifactId>podman-maven-plugin</artifactId>
///                         <version>main</version>
///                         <executions>
///                             <execution>
///                                 <id>run-pod</id>
///                                 <goals>
///                                     <goal>runPod</goal>
///                                 </goals>
///                                 <configuration>
///                                     <verbose>true</verbose>
///                                     <pod>
///                                         <name>spotifydanceinfo-pod</name>
///                                         <publish>5005:5005</publish>
///                                         <publish>6379:6379</publish>
///                                         <publish>8080:8080</publish>
///                                     </pod>
///                                     <containers>
///                                         <container>
///                                             <image>redis:7-alpine</image>
/// 											<mute>true</mute>
///                                         </container>
///                                         <container>
///                                             <image>org.tbee.spotifydanceinfo/spotify-dance-info:${project.version}</image>
///                                             <dns>8.8.8.8</dns>
///                                             <dns>1.1.1.1</dns>
///                                             <env>BASEURL=http://127.0.0.1:8080</env>
///                                             <env>SPRING_DATA_REDIS_HOST=localhost</env>
///                                             <env>SPRING_DATA_REDIS_PORT=6379</env>
///                                         </container>
///                                     </containers>
///                                 </configuration>
///                             </execution>
///                         </executions>
///                     </plugin>
/// ```
@Mojo(name = "runPod", defaultPhase = LifecyclePhase.VALIDATE)
public class PodmanRunPodMojo extends AbstractPodmanMojo {

	@Parameter(required = true)
	protected Pod pod;

	@Parameter(required = true)
	protected Container[] containers;

	@Parameter(defaultValue = "${mojoExecution.configuration}", readonly = true)
	protected Xpp3Dom mojoConfiguration;

	private final List<Process> logProcesses = Collections.synchronizedList(new ArrayList<>());
	private final CountDownLatch stopSignal = new CountDownLatch(1);
	private volatile boolean runCompleted;
	private Thread shutdownHook;

	// https://docs.podman.io/en/latest/markdown/podman-pod-create.1.html
	public static class Pod {
		public String name;
		public String network;
		public String[] publish;
		public String[] dns;
		public String[] addHost;
	}

	// https://docs.podman.io/en/latest/markdown/podman-run.1.html
	public static class Container {
		public String name;
		public String image;
		public String[] dns;
		public String[] env;
		public String[] volume;
		public String[] publish;
		public String[] addHost;
		public String workdir;
		public String user;
		public boolean mute = false; // If true, do not follow the container's log
	}

	public void execute() throws MojoExecutionException {
		validateConfiguration();
		applyArrayFallbackConfiguration();
		registerShutdownHook();

		try {
			removePod();
			createPod();
			startContainers();
			startLogFollowers();
			waitUntilStopped();
			runCompleted = true;
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			cleanupOnInterrupt();
			throw new MojoExecutionException("runPod interrupted", e);
		}
	}

	private void registerShutdownHook() {
		shutdownHook = new Thread(this::cleanupOnInterrupt, "podman-runpod-cleanup");
		Runtime.getRuntime().addShutdownHook(shutdownHook);
	}

	private void cleanupOnInterrupt() {
		if (runCompleted) {
			return;
		}
		try {
			stopSignal.countDown();

			synchronized (logProcesses) {
				for (Process process : logProcesses) {
					if (process.isAlive()) {
						process.destroy();
					}
				}
			}

			for (Container container : containers) {
				execute(stopContainerCommand(container.name), List.of(0, 1, 125));
			}

			execute(stopPodCommand(), List.of(0, 1, 125));
			execute(removePodCommand(), List.of(0, 1, 125));
		}
		catch (Exception e) {
			getLog().warn("Interrupted cleanup failed: " + e.getMessage());
		}
	}

	private List<String> stopContainerCommand(String containerName) {
		List<String> command = podmanCommand();
		command.add("stop");
		command.add(containerName);
		return command;
	}

	private List<String> stopPodCommand() {
		List<String> command = podmanCommand();
		command.add("pod");
		command.add("stop");
		command.add(pod.name);
		return command;
	}

	private List<String> removePodCommand() {
		List<String> command = podmanCommand();
		command.add("pod");
		command.add("rm");
		command.add("-f");
		command.add(pod.name);
		return command;
	}

	private void validateConfiguration() throws MojoExecutionException {
		if (pod == null || pod.name == null || pod.name.isBlank()) {
			throw new MojoExecutionException("pod.name is required");
		}
		if (containers == null || containers.length == 0) {
			throw new MojoExecutionException("At least one container is required");
		}
		for (int i = 0; i < containers.length; i++) {
			Container container = containers[i];
			if (container.image == null || container.image.isBlank()) {
				throw new MojoExecutionException("containers[" + i + "].image is required");
			}
			// if name is missing, use a default derived from the image
			if (container.name == null || container.name.isBlank()) {
				container.name = container.image.replaceAll("[^a-zA-Z0-9-_.]", "");
			}
		}
	}

	private void applyArrayFallbackConfiguration() {
		Xpp3Dom configuration = configurationDom();
		if (configuration == null) {
			return;
		}

		Xpp3Dom podNode = configuration.getChild("pod");
		if (podNode != null) {
			pod.publish = preferParsedValues(pod.publish, readChildrenValues(podNode, "publish"));
			pod.dns = preferParsedValues(pod.dns, readChildrenValues(podNode, "dns"));
			pod.addHost = preferParsedValues(pod.addHost, readChildrenValues(podNode, "addHost"));
		}

		Xpp3Dom containersNode = configuration.getChild("containers");
		if (containersNode == null || containers == null) {
			return;
		}

		Xpp3Dom[] containerNodes = containersNode.getChildren("container");
		for (int i = 0; i < containers.length && i < containerNodes.length; i++) {
			Container container = containers[i];
			Xpp3Dom containerNode = containerNodes[i];
			container.dns = preferParsedValues(container.dns, readChildrenValues(containerNode, "dns"));
			container.env = preferParsedValues(container.env, readChildrenValues(containerNode, "env"));
			container.volume = preferParsedValues(container.volume, readChildrenValues(containerNode, "volume"));
			container.publish = preferParsedValues(container.publish, readChildrenValues(containerNode, "publish"));
			container.addHost = preferParsedValues(container.addHost, readChildrenValues(containerNode, "addHost"));
		}
	}

	private Xpp3Dom configurationDom() {
		return mojoConfiguration;
	}

	private String[] readChildrenValues(Xpp3Dom parent, String childName) {
		if (parent == null) {
			return new String[0];
		}
		Xpp3Dom[] children = parent.getChildren(childName);
		List<String> values = new ArrayList<>();
		for (Xpp3Dom child : children) {
			String value = child.getValue();
			if (value != null && !value.isBlank()) {
				values.add(value);
			}
		}
		return values.toArray(new String[0]);
	}

	private String[] preferParsedValues(String[] injectedValues, String[] parsedValues) {
		return countNonBlank(parsedValues) > countNonBlank(injectedValues) ? parsedValues : injectedValues;
	}

	private int countNonBlank(String[] values) {
		if (values == null) {
			return 0;
		}
		int count = 0;
		for (String value : values) {
			if (value != null && !value.isBlank()) {
				count++;
			}
		}
		return count;
	}

	private void removePod() throws MojoExecutionException {
		List<String> command = podmanCommand();
		command.add("pod");
		command.add("rm");
		command.add("-f");
		command.add(pod.name);
		execute(command, List.of(0, 1, 125));
	}

	private void createPod() throws MojoExecutionException {
		List<String> command = podmanCommand();
		command.add("pod");
		command.add("create");
		command.add("--name");
		command.add(pod.name);

		if (pod.network != null && !pod.network.isBlank()) {
			command.add("--network");
			command.add(pod.network);
		}
		add(command, "--publish", pod.publish);
		add(command, "--dns", pod.dns);
		add(command, "--add-host", pod.addHost);

		execute(command);
	}

	private void startContainers() throws MojoExecutionException {
		for (Container container : containers) {
			List<String> command = new ArrayList<>(podmanCommand());
			command.add("run");
			command.add("--detach");
			command.add("--rm");
			command.add("--replace");
			command.add("--pod");
			command.add(pod.name);
			command.add("--name");
			command.add(container.name);
			if (container.workdir != null && !container.workdir.isBlank()) {
				command.add("--workdir");
				command.add(container.workdir);
			}
			if (container.user != null && !container.user.isBlank()) {
				command.add("--user");
				command.add(container.user);
			}
			add(command, "--dns", container.dns);
			add(command, "--env", container.env);
			add(command, "--volume", container.volume);
			add(command, "--publish", container.publish);
			add(command, "--add-host", container.addHost);
			command.add(container.image);

			execute(command);
		}
	}

	private void startLogFollowers() throws MojoExecutionException {
		for (Container container : containers) {
			if (container.mute) {
				continue;
			}
			String containerName = container.name;
			List<String> command = podmanCommand();
			command.add("logs");
			command.add("--follow");
			command.add(containerName);
			Process process = startProcess(command);
			logProcesses.add(process);
			startLogPump(process.getInputStream(), containerName, false);
			startLogPump(process.getErrorStream(), containerName, true);
		}
	}

	private Process startProcess(List<String> command) throws MojoExecutionException {
		try {
			if (verbose) {
				String commandText = "";
				for (String arg : command) {
					commandText += arg + " ";
				}
				getLog().info(project.getBasedir().getAbsolutePath() + ": " + commandText);
			}
			ProcessBuilder processBuilder = new ProcessBuilder();
			processBuilder.directory(project.getBasedir());
			processBuilder.command(command);
			return processBuilder.start();
		}
		catch (Exception e) {
			throw new MojoExecutionException(e.getMessage(), e);
		}
	}

	private void startLogPump(InputStream inputStream, String containerName, boolean errorStream) {
		Thread thread = new Thread(() -> {
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
				String line;
				while ((line = reader.readLine()) != null) {
					if (errorStream) {
						getLog().warn(containerName + ": " + line);
					}
					else {
						getLog().info(containerName + ": " + line);
					}
				}
			}
			catch (Exception e) {
				getLog().warn("Failed to stream logs for " + containerName + ": " + e.getMessage());
			}
		}, "podman-log-" + containerName + (errorStream ? "-err" : "-out"));
		thread.setDaemon(true);
		thread.start();
	}

	private void waitUntilStopped() throws InterruptedException {
		while (!stopSignal.await(1, TimeUnit.SECONDS)) {
			// keep mojo alive until shutdown signal is received
		}
	}

	private void add(List<String> command, String option, String... values) {
		if (values == null) {
			return;
		}
		for (String value : values) {
			if (value != null && !value.isBlank()) {
				command.add(option);
				command.add(value);
			}
		}
	}
}

