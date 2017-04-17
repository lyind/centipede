package net.talpidae.centipede.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.talpidae.base.insect.message.payload.Mapping;


@RequiredArgsConstructor
@Getter
public class NewMapping
{
    private final Mapping mapping;
}
