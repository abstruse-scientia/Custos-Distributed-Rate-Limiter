package com.abstruse.custos.resolver;

import com.abstruse.custos.core.model.RequestContext;

public class UserKeyResolver implements KeyResolver {
    @Override
    public KeyType getKeyType() {
        return KeyType.USER;
    }

    @Override
    public String resolve(RequestContext context) {
        return context.getUserId();
    }
}
