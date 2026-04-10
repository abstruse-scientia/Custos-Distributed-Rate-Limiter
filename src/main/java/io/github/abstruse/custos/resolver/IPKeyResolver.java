package com.abstruse.custos.resolver;

import com.abstruse.custos.core.model.RequestContext;

public class IPKeyResolver implements KeyResolver {

    @Override
    public KeyType getKeyType() {
        return KeyType.IP;
    }

    @Override
    public String resolve(RequestContext context) {
        return context.getIpAddress();
    }
}
