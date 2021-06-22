package org.tbee.podman.podmanMavenPlugin;

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
