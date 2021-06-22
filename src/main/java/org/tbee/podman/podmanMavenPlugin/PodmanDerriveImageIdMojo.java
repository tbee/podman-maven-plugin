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
@Mojo(name = "imageId", defaultPhase = LifecyclePhase.INITIALIZE)
public class PodmanDerriveImageIdMojo extends AbstractMojo
{
	@Parameter(defaultValue = "${project}", required = true, readonly = true)
	protected MavenProject project;
	
    /**
     * Location of the container file.
     */
	@Parameter(defaultValue = "containerImageId", required = true, readonly = false)
	protected String imageIdPropertyName;

	/**
	 * 
	 */
    public void execute() throws MojoExecutionException {
    	String groupId = project.getGroupId();
    	String artifactId = project.getArtifactId();
    	String imageId = (groupId + "." + artifactId).toLowerCase();
    	
    	getLog().info("Generated container image id stored in ${" + imageIdPropertyName + "} = " + imageId);
    	project.getProperties().setProperty(imageIdPropertyName, imageId);
    }
}
