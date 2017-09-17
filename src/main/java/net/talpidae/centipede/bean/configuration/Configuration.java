package net.talpidae.centipede.bean.configuration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import net.talpidae.centipede.bean.service.Service;

import org.jdbi.v3.core.mapper.reflect.ColumnName;

import java.util.List;
import java.util.Map;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;


/**
 * Centipede configuration format (init.json file format).
 */
@Getter
@EqualsAndHashCode
@ToString
@Builder(toBuilder = true)
public class Configuration
{
    private final Map<String, String> environment;

    private final List<Service> initialServices;


    @JsonCreator
    public Configuration(@JsonProperty("environment") @ColumnName("environment") Map<String, String> environment,
                         @JsonProperty("initialServices") @ColumnName("initialServices") List<Service> initialServices)
    {
        this.environment = environment;
        this.initialServices = initialServices;
    }
}
