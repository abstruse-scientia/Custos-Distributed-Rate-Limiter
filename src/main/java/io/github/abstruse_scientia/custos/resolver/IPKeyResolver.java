package io.github.abstruse_scientia.custos.resolver;

import io.github.abstruse_scientia.custos.core.model.RequestContext;
import org.springframework.stereotype.Component;

@Component
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
