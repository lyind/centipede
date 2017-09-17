package net.talpidae.centipede.util.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

import net.talpidae.centipede.bean.configuration.Configuration;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;

import javax.inject.Inject;
import javax.inject.Singleton;

import lombok.extern.slf4j.Slf4j;


@Singleton
@Slf4j
public class ConfigurationLoader
{
    private final static String CONFIGURATION_FILE_NAME = "init.json";

    private ObjectReader configurationReader;


    @Inject
    public ConfigurationLoader(ObjectMapper objectMapper) throws IOException
    {
        configurationReader = objectMapper.readerFor(Configuration.class);
    }


    /**
     * Return de-serialized init.json or an empty default configuration.
     *
     * @throws ConfigurationException in case of format or IO errors other than FileNotFoundException.
     */
    public Configuration load() throws ConfigurationException
    {
        try
        {
            Configuration configuration = null;
            try
            {
                configuration = configurationReader.readValue(new File(CONFIGURATION_FILE_NAME));
            }
            catch (FileNotFoundException e)
            {
                log.warn("no {} in working directory, using default configuration", CONFIGURATION_FILE_NAME);
            }

            if (configuration == null || (configuration.getEnvironment() == null && configuration.getInitialServices() == null))
            {
                log.warn("{} in working directory looks suspiciously empty, using default configuration", CONFIGURATION_FILE_NAME);
                configuration = Configuration.builder() // empty default config
                        .environment(Collections.emptyMap())
                        .initialServices(Collections.emptyList())
                        .build();
            }

            return configuration;
        }
        catch (Exception e)
        {
            throw new ConfigurationException("failed to load: " + CONFIGURATION_FILE_NAME, e);
        }
    }
}