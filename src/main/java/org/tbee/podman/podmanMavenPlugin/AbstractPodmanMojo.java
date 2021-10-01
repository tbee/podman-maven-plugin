package org.tbee.podman.podmanMavenPlugin;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;

/*-
 * #%L
 * podman-maven-plugin
 * %%
 * Copyright (C) 2020 - 2021 Tom Eugelink
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

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import edu.emory.mathcs.backport.java.util.Arrays;

/*
 * 
 */
abstract public class AbstractPodmanMojo extends AbstractMojo
{
	// get access to the project parameters (pom.xml)
	@Parameter(defaultValue="${project}", readonly=true, required=true)
	protected MavenProject project;

    /**
     * Show complete podman command line (including possible passwords)
     */
	@Parameter(property = "podman.verbose", defaultValue = "false", required = true, readonly = false)
	protected Boolean verbose;

    /**
     * Does not copy the output of podman
     */
	@Parameter(property = "podman.silent", defaultValue = "false", required = true, readonly = false)
	protected Boolean silent;

    /**
     * Lets podman generate debug outpu 
     */
	@Parameter(property = "podman.debug", defaultValue = "false", required = true, readonly = false)
	protected Boolean debug;

    /**
     * tags
     */
	@Parameter(property = "podman.tags", required = false, readonly = false)
	protected String[] tags;

    /**
     * registry to push to (and with pullFromRegistry=true, also to pull from)
     */
	@Parameter(property = "podman.registry", required = false, readonly = false)
	protected Registry registry;

	static public class Registry {
		public String hostname;
		public String url;
		public String user;
		public String password;
		
		public String toString() {
			return user + "@" + url;
		}
	}
	
	/* */
	protected List<String> podmanCommand() {
    	List<String> command = new ArrayList<>();
		command.add("podman");
		if (debug) {
			command.add("--log-level");
			command.add("debug");
		}
		return command;
	}

    /* */
    protected String execute(List<String> args, List<Integer> exitCodes) throws MojoExecutionException {
        try {
	        // kick off the build process
	        ProcessBuilder processBuilder = new ProcessBuilder();
	        processBuilder.directory(project.getBasedir()); // set the working directory to the location of the pom.xml
	        processBuilder.command(args);
	        
	        // execute command
	        if (verbose) {
	        	String commandText = "";
	        	for (String arg : args) {
	        		commandText += arg + " ";
	        	}
	        	getLog().info(project.getBasedir().getAbsolutePath() + ": " + commandText);	        	
	        }
	        else {
	        	getLog().info(args.get(0) + " " + args.get(1));	        		        	
	        }	        
			Process process = processBuilder.start();
			
			// read output
			AtomicReference<String> lastLineReference = new AtomicReference<>(null);
			try (
				BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))
			) {
				String line;
				while ((line = reader.readLine()) != null) {
					if (!silent) {
						getLog().info(line);
					}
					lastLineReference.set(line); // capture last line to get to the imageId after a build
				}
			}
			// read error
			try (
				BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))
			) {
				String line;
				while ((line = reader.readLine()) != null) {
					getLog().error(line);
				}
			}
			
			// wait for completion
			int exitCode = process.waitFor();
			if (!exitCodes.contains(exitCode)) {
	            throw new MojoExecutionException(args + " did not finish successfully: " + exitCode);				
			}
			String lastLine = lastLineReference.get();
			return lastLine;
        }
        catch (Exception e) {
        	throw new MojoExecutionException(e.getMessage(), e);
        }
        
    }
    protected String execute(List<String> args) throws MojoExecutionException {
    	return execute(args, List.of(0));    	
    }
    protected String execute(String... args) throws MojoExecutionException {
    	return execute(Arrays.asList(args), List.of(0));
    }    
}
