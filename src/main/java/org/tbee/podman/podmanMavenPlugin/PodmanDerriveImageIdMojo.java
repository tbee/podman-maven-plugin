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
