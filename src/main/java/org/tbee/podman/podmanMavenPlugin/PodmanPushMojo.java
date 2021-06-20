package org.tbee.podman.podmanMavenPlugin;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 * 
 */
@Mojo(name = "push", defaultPhase = LifecyclePhase.DEPLOY)
public class PodmanPushMojo extends AbstractMojo
{
	@Parameter(defaultValue = "${project}", required = true, readonly = true)
	MavenProject project;
	
	/**
	 * 
	 */
    public void execute() throws MojoExecutionException
    {
    }
}
