package io.github.abstruse_scientia.custos.resolver;

import io.github.abstruse_scientia.custos.core.model.RequestContext;

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
