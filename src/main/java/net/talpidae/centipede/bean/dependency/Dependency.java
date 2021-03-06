package net.talpidae.centipede.bean.dependency;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import net.talpidae.centipede.bean.service.State;

import org.jdbi.v3.core.mapper.reflect.ColumnName;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;


@Getter
@EqualsAndHashCode
@ToString
public class Dependency
{
    private final String source;

    private final String target;

    private final State sourceState;

    private final State targetState;


    @Builder
    @JsonCreator
    public Dependency(@JsonProperty("source") @ColumnName("source") String source,
                      @JsonProperty("target") @ColumnName("target") String target,
                      @JsonProperty("sourceState") @ColumnName("sourceState") State sourceState,
                      @JsonProperty("targetState") @ColumnName("targetState") State targetState)
    {
        this.source = source;
        this.target = target;
        this.sourceState = sourceState;
        this.targetState = targetState;
    }
}