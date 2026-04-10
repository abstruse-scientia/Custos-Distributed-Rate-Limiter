package io.github.abstruse_scientia.custos.core.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class RequestContext {

    private final String userId;
    private final String ipAddress;

}
