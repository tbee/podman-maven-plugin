package org.tbee.podman.podmanMavenPlugin;

import java.io.File;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import edu.emory.mathcs.backport.java.util.Arrays;

// TODO: quiet
// TODO: per default hide command because of password
// TODO: verbose (show the whole command)
// TODO: can we derrive the contained id from the project?

/*
 * 
 */
abstract public class AbstractPodmanMojo extends AbstractMojo
{
	@Parameter(defaultValue = "${project}", required = true, readonly = true)
	protected MavenProject project;
	
    /**
     * List all output (including possible passwords, etc)
     */
	@Parameter(defaultValue = "false", required = true, readonly = false)
	protected Boolean verbose;

    /**
     * No output at all
     */
	@Parameter(defaultValue = "false", required = true, readonly = false)
	protected Boolean silent;

    /**
     * Location of the container file.
     */
	@Parameter(defaultValue = "src/main/container/Containerfile", required = true, readonly = false)
	protected File containerFile;

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
	        	getLog().debug(commandText);	        	
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
