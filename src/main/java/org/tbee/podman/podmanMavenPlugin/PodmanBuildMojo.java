package org.tbee.podman.podmanMavenPlugin;

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
	@Parameter(defaultValue = "src/main/container/Containerfile", required = true, readonly = false)
    private File containerFile;

	// TODO: quiet
	
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
        	List<String> tagList = Arrays.asList(tags);
        	
        	// rmi
        	{
	        	List<String> command = new ArrayList<>();
	        	command.add("podman");
	        	command.add("rmi");
	        	command.addAll(tagList);
	        	execute(command, List.of(0,1,125));
        	}
        	
        	// tag
        	{
	        	List<String> command = new ArrayList<>();
	        	command.add("podman");
	        	command.add("tag");
	        	command.add(imageId);
	        	command.addAll(tagList);
	        	execute(command);
        	}
        }
    }
    
/*
			<!-- http://docs.podman.io/en/latest/Commands.html -->
			<plugin>
				<groupId>org.codehaus.mojo</groupId>
				<artifactId>exec-maven-plugin</artifactId>
				<executions>

					<!-- Remove existing image from local repo -->
					<execution>
						<id>container-clean</id>
						<phase>install</phase>
						<goals>
							<goal>exec</goal>
						</goals>
						<configuration>
							<executable>podman</executable>
							<workingDirectory>${project.basedir}</workingDirectory>
							<arguments>
								<argument>rmi</argument>
								<argument>${containerImageId}:${project.version}</argument>
								<argument>${containerregistry.hostname}/${containerImageId}:${project.version}</argument>
							</arguments>
							<!-- exit code 1 means the image does not exist, which is correct 
								on the first run -->
							<successCodes>0,1,125</successCodes>
						</configuration>
					</execution>

					<!-- Create new container image using Containerfile -->
					<!-- Tag the image using maven project version information, both for the local repo as for the remote (but it is not pushed yet) -->
					<!-- "To push any local image to a Container Registry, you need to first tag it with the registry name and then push the image." -->
					<execution>
						<id>container-build</id>
						<phase>install</phase>
						<goals>
							<goal>exec</goal>
						</goals>
						<configuration>
							<executable>podman</executable>
							<workingDirectory>${project.basedir}</workingDirectory>
							<arguments>
								<argument>build</argument>
								<argument>--file</argument>
								<argument>src/main/container/standalone.Containerfile</argument>

								<argument>--tag</argument>
								<argument>${containerImageId}:${project.version}</argument>

								<argument>--tag</argument>
								<argument>${containerregistry.hostname}/${containerImageId}:${project.version}</argument>
								
								<argument>.</argument>
							</arguments>
						</configuration>
					</execution>

					<!-- Login and Push the image to a container repo. -->
					<!-- To run without maven repo deploy: mvn -Dmaven.deploy.skip=true deploy -->
					<!-- TBEERNOT: force overwrite auth.js cache -->
					<!-- https://dockerregistry.softworks.nl/v2/_catalog -->
					<!-- https://dockerregistry.softworks.nl/v2/nl.softworks.teslatasks/teslatasksselfhosted/tags/list -->
					<execution>
						<id>container-login</id>
						<phase>deploy</phase>
						<goals>
							<goal>exec</goal>
						</goals>
						<configuration>
							<executable>podman</executable>
							<workingDirectory>${project.basedir}</workingDirectory>
							<arguments>
								<argument>login</argument>
								<argument>-u</argument>
								<argument>${containerregistry.user}</argument>
								<argument>-p</argument>
								<argument>${containerregistry.password}</argument>
								<argument>${containerregistry.url}</argument>
							</arguments>
						</configuration>
					</execution>
					
					<execution>
						<id>container-push</id>
						<phase>deploy</phase>
						<goals>
							<goal>exec</goal>
						</goals>
						<configuration>
							<executable>podman</executable>
							<workingDirectory>${project.basedir}</workingDirectory>
							<arguments>
								<argument>push</argument>
								<argument>${containerregistry.hostname}/${containerImageId}:${project.version}</argument>
							</arguments>
						</configuration>
					</execution>
				</executions>
			</plugin>
		</plugins>
    
 */
}
