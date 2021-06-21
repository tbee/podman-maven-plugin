package org.tbee.podman.podmanMavenPlugin;

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
