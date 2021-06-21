package org.tbee.podman.podmanMavenPlugin;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * 
 */
@Mojo(name = "push", defaultPhase = LifecyclePhase.DEPLOY)
public class PodmanPushMojo extends AbstractPodmanMojo
{
    /**
     * registries
     */
	@Parameter(required = true, readonly = false)
    private Registry[] registries;
	static public class Registry {
		public String hostname;
		public String url;
		public String user;
		public String password;
		
		public String toString() {
			return user + "@" + url;
		}
	}
	
	/**
	 * 
	 */
    public void execute() throws MojoExecutionException {
    	
    	if (registries != null && registries.length > 0) {
    		
        	// login
    		for (Registry registry : registries) {
            	execute("podman", "login", "-u", registry.user, "-p", registry.password, registry.url);
    		};

    		// push
    		for (Registry registry : registries) {
    			for (String tag : tags) {
    				execute("podman", "push", registry.hostname + "/" + tag);
    			}
    		};
    	}
    }
}
