package org.tbee.podman.podmanMavenPlugin;

/*-
 * #%L
 * TeslaAPI
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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import edu.emory.mathcs.backport.java.util.Arrays;


/**
 * The goal that builds, and if successful tags, a container image usually during install.
 */
@Mojo(name = "build", defaultPhase = LifecyclePhase.INSTALL)
public class PodmanBuildMojo extends AbstractPodmanMojo
{
    /**
     * Location of the container file.
     */
	@Parameter(property = "podman.containerFile", defaultValue = "src/main/container/Containerfile", required = true, readonly = false)
	protected File containerFile;

    /**
     * Additional registries used for pulling
     */
	@Parameter(property = "podman.registries", required = false, readonly = false)
	protected Registry[] registries;
	
    /**
     * Marks the registry entry we're push to also as the one we're pulling from, so a login is needed prior to pulling
     */
	@Parameter(property = "podman.registry", defaultValue = "false", required = true, readonly = false)
	protected Boolean pullFromRegistry;
	
	/**
	 * 
	 */
    public void execute() throws MojoExecutionException
    {
    	// check containerFile
        File f = containerFile;
        if ( !f.exists() ) {
            throw new MojoExecutionException("Container file does not exist: " + containerFile.getAbsolutePath());
        }
        
        // If login is needed
        List<Registry> registries = new ArrayList<>();
        if (pullFromRegistry && this.registry != null) {
        	registries.add(this.registry);
        }
        if (this.registries != null) {
        	registries.addAll(Arrays.asList(this.registries));
        }
        for (Registry registry : registries) {
        	execute("podman", "login", "-u", registry.user, "-p", registry.password, registry.url);
        }
        
        
        // build
        String imageId = execute("podman", "build", "--file", containerFile.getAbsolutePath(), ".");
        
        // tag
        if (tags != null && tags.length > 0) {

        	String tempTag = "tag"  + imageId;
			try {
	        	// place temporarily tag (to lock down the image)
	        	{
		        	List<String> command = podmanCommand();
		        	command.add("tag");
		        	command.add(imageId);
	        		command.add(tempTag);
		        	execute(command);
	        	}
	        	
	        	// rmi (remove) any existing tags
	        	{
		        	List<String> command = podmanCommand();
		        	command.add("rmi");
		        	for (String tag : tags) {
		        		command.add(tag);
		        	}
		        	execute(command, List.of(0,1,125));
	        	}
	        	
	        	// tag again
	        	{
		        	List<String> command = podmanCommand();
		        	command.add("tag");
		        	command.add(imageId);
		        	for (String tag : tags) {
		        		command.add(tag);
		        	}
		        	execute(command);
	        	}
        	}
        	finally { 
        		// rmi (remove) temporarily tag
	        	List<String> command = podmanCommand();
	        	command.add("rmi");
        		command.add(tempTag);
	        	execute(command, List.of(0,1,125));
        	}
        }
    }
}
