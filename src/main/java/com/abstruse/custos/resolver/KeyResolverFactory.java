package com.abstruse.custos.resolver;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class KeyResolverFactory {

    private final Map<KeyType, KeyResolver> resolverMap;

    public KeyResolverFactory(List<KeyResolver> keyResolvers) {
        this.resolverMap = keyResolvers.stream().collect(Collectors.toMap(
                KeyResolver::getKeyType,
                keyResolver -> keyResolver
        ));
    }

    public KeyResolver getKeyResolver(KeyType keyType) {
        return resolverMap.get(keyType);
    }
}
