package net.talpidae.centipede.util.configuration;

public class ConfigurationException extends RuntimeException
{
    public ConfigurationException(String message, Exception cause)
    {
        super(message, cause);
    }
}
