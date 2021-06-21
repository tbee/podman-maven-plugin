# podman-maven-plugin

This is an opinionated Podman maven plugin, which means that it implements a certain approach and workflow to using Podman.
If this does not fit your needs, it often is more easy to simply use the exec-maven-plugin and execute podman to your liking.

The approach and workflow this plugin implements is:

Build goal:
* In Maven's install phase "podman build" is called.
* If successful, the build is tagged with the configured tags.

Push goal:
* In Maven's deploy phase, a login is performed on the configured registry.
* If successful, for each configured tag a second tag is added by prefixing it with the registry's hostname for pushing to the registry.
* Next "podman push" is called for each tag.

Tags are removed using "podman rmi" prior to creating a new tag, ignoring any errors.

This opinionated flow means that the whole sequence of podman build, rmi, tag, login and push is configured using the configuration below:


```xml
	<plugin>
		<groupId>org.tbee</groupId>
		<artifactId>podman-maven-plugin</artifactId>
		<version>...</version>				
		<executions>
			<execution>
				<goals>
					<goal>build</goal>
					<goal>push</goal>
				</goals>
			</execution>
		</executions>
		<configuration>
			<containerFile>src/main/container/Containerfile</containerFile>
			<tags>
				<tag>${containerImageId}:${project.version}</tag>
			</tags>
			<registry>
				<hostname>docker.io</hostname>					
				<url>https://docker.io/library</url>					
				<user>${dockerio.user}</user>
				<password>${dockerio.password}</password>
			</registry>
		</configuration>
	</plugin>
```

The user and password are defined as properties in Maven's settings.xml.