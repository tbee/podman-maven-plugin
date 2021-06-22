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


/**
 * The goal that builds, and if successful tags, a container image usually during install.
 */
@Mojo(name = "build", defaultPhase = LifecyclePhase.INSTALL)
public class PodmanBuildMojo extends AbstractPodmanMojo
{
    /**
     * Location of the container file.
     */
	@Parameter(defaultValue = "src/main/container/Containerfile", required = true, readonly = false)
	protected File containerFile;

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
        
        // build
        String imageId = execute("podman", "build", "--file", containerFile.getAbsolutePath(), ".");
        
        // tag
        if (tags != null && tags.length > 0) {
        	
        	// rmi
        	{
	        	List<String> command = new ArrayList<>();
	        	command.add("podman");
	        	command.add("rmi");
	        	for (String tag : tags) {
	        		command.add(tag);
	        	}
	        	execute(command, List.of(0,1,125));
        	}
        	
        	// tag
        	{
	        	List<String> command = new ArrayList<>();
	        	command.add("podman");
	        	command.add("tag");
	        	command.add(imageId);
	        	for (String tag : tags) {
	        		command.add(tag);
	        	}
	        	execute(command);
        	}
        }
    }
}
