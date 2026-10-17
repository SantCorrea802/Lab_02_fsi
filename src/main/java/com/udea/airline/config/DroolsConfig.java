package com.udea.airline.config;

import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.Message;
import org.kie.api.runtime.KieContainer;
import org.kie.internal.io.ResourceFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DroolsConfig {

    @Bean
    public KieContainer kieContainer() {
        KieServices services = KieServices.Factory.get();
        KieFileSystem fileSystem = services.newKieFileSystem();
        fileSystem.write(ResourceFactory.newClassPathResource("com/udea/airline/rules/airline-rules.drl"));
        KieBuilder builder = services.newKieBuilder(fileSystem).buildAll();
        if (builder.getResults().hasMessages(Message.Level.ERROR)) {
            throw new IllegalStateException("Error al compilar las reglas Drools: "
                    + builder.getResults().getMessages(Message.Level.ERROR));
        }
        return services.newKieContainer(builder.getKieModule().getReleaseId());
    }
}
