package net.talpidae.centipede.service.wrapper;

import lombok.Getter;
import lombok.Setter;
import net.talpidae.base.util.auth.AuthenticationSecurityContext;


@Getter
@Setter
public class CallContext
{
    private AuthenticationSecurityContext securityContext;
}
