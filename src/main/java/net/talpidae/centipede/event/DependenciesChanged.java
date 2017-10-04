package net.talpidae.centipede.event;

import java.util.Set;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;


@RequiredArgsConstructor
@Getter
@EqualsAndHashCode
@ToString
@Builder
public class DependenciesChanged
{
    private final String name;

    private final Set<String> dependencies;
}