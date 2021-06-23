package org.tbee.podman.podmanMavenPlugin;

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
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;

import edu.emory.mathcs.backport.java.util.Arrays;

/*
 * 
 */
abstract public class AbstractPodmanMojo extends AbstractMojo
{
    /**
     * Show complete podman command line (including possible passwords)
     */
	@Parameter(defaultValue = "false", required = true, readonly = false)
	protected Boolean verbose;

    /**
     * Does not copy the output of podman
     */
	@Parameter(defaultValue = "false", required = true, readonly = false)
	protected Boolean silent;

    /**
     * tags
     */
	@Parameter(required = false, readonly = false)
	protected String[] tags;

    /**
     * registry
     */
	@Parameter(required = false, readonly = false)
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
    protected String execute(List<String> args, List<Integer> exitCodes) throws MojoExecutionException {
        try {
	        // kick off the build process
	        ProcessBuilder processBuilder = new ProcessBuilder();
	        processBuilder.command(args);
	        
	        // execute command
	        if (verbose) {
	        	String commandText = "";
	        	for (String arg : args) {
	        		commandText += arg + " ";
	        	}
	        	getLog().info(commandText);	        	
	        }
	        else {
	        	getLog().info(args.get(0) + " " + args.get(1));	        		        	
	        }
	        
			Process process = processBuilder.start();
			AtomicReference<String> lastLineReference = new AtomicReference<>(null);
			StreamGobbler streamGobbler = new StreamGobbler(process.getInputStream(), s -> {
				if (!silent) {
					getLog().info(s);
				}
				lastLineReference.set(s);
			});
			Executors.newSingleThreadExecutor().submit(streamGobbler);
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
    protected  String execute(List<String> args) throws MojoExecutionException {
    	return execute(args, List.of(0));    	
    }
    protected  String execute(String... args) throws MojoExecutionException {
    	return execute(Arrays.asList(args), List.of(0));
    }    
}
