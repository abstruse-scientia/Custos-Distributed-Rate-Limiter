package io.github.abstruse_scientia.custos.resolver;


import io.github.abstruse_scientia.custos.core.model.RequestContext;


public interface KeyResolver {

    /**
     * @return KeyType
     */
    KeyType getKeyType();
    /**
     *
     * @param context: sent by the user
     * @return A unique string identifier.
     */
    String resolve(RequestContext context);
}
