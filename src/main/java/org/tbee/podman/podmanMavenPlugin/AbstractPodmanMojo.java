package org.tbee.podman.podmanMavenPlugin;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import edu.emory.mathcs.backport.java.util.Arrays;

/*
 * 
 */
abstract class AbstractPodmanMojo extends AbstractMojo
{
	@Parameter(defaultValue = "${project}", required = true, readonly = true)
	protected MavenProject project;
	
    /**
     * tags
     */
	@Parameter(required = false, readonly = false)
	protected String[] tags;

    /* */
    protected String execute(List<String> args, List<Integer> exitCodes) throws MojoExecutionException {
        try {
	        // kick off the build process
	        ProcessBuilder processBuilder = new ProcessBuilder();
	        processBuilder.command(args);
	        
	        // execute command
	        System.out.println(args);
			Process process = processBuilder.start();
			AtomicReference<String> lastLineReference = new AtomicReference<>(null);
			StreamGobbler streamGobbler = new StreamGobbler(process.getInputStream(), s -> {
				System.out.println(s);
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
