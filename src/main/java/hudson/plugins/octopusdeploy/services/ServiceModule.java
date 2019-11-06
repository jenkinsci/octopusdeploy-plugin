package hudson.plugins.octopusdeploy.services;

import com.google.inject.AbstractModule;
import hudson.plugins.octopusdeploy.services.impl.FileServiceImpl;

public class ServiceModule extends AbstractModule {
    @Override
    public void configure() {
        bind(FileService.class).to(FileServiceImpl.class).in(com.google.inject.Singleton.class);
    }
}
