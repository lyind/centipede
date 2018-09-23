package net.talpidae.centipede.service.wrapper;

import net.talpidae.base.util.auth.AuthenticationSecurityContext;
import net.talpidae.centipede.bean.Api;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;


@AllArgsConstructor
public class Call
{
    @Getter
    private final Api request;

    @Getter
    private final Api.ApiBuilder response = Api.builder();

    @Getter
    @Setter
    private AuthenticationSecurityContext securityContext;
}
