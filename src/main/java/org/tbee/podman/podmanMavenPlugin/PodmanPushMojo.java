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

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

/**
 * 
 */
@Mojo(name = "push", defaultPhase = LifecyclePhase.DEPLOY)
public class PodmanPushMojo extends AbstractPodmanMojo
{
	/**
	 * 
	 */
    public void execute() throws MojoExecutionException {
    	
    	if (registry != null) {
    		
        	// login
        	execute("podman", "login", "-u", registry.user, "-p", registry.password, registry.url);

            // tag
            if (tags != null && tags.length > 0) {
            	
	        	for (String tag : tags) {
	        		String pushTag = registry.hostname + "/" + tag;
	        		
	            	// rmi
	            	{
	    	        	List<String> command = new ArrayList<>();
	    	        	command.add("podman");
	    	        	command.add("rmi");
		        		command.add(pushTag);	        			
		        		execute(command, List.of(0,1,125));
	            	}
            	
	            	// tag
		        	{
	    	        	List<String> command = new ArrayList<>();
	    	        	command.add("podman");
	    	        	command.add("tag");
	    	        	command.add(tag);
		        		command.add(pushTag);	        			
	    	        	execute(command);
		        	}

		        	// push
	        		execute("podman", "push", pushTag);
	        	}
			}
    	}
    }
}
