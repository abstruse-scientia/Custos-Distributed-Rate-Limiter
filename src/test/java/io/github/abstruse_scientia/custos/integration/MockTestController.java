package io.github.abstruse_scientia.custos.integration;

import io.github.abstruse_scientia.custos.annotations.RateLimit;
import io.github.abstruse_scientia.custos.core.model.Algorithm;
import io.github.abstruse_scientia.custos.resolver.KeyType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test/v1/")
public class MockTestController {

    @GetMapping("/user-limited")
    @RateLimit(keytype = KeyType.USER, capacity = 100, rate = 2, algorithm = Algorithm.TOKEN_BUCKET)
    public String test() { return "ok"; }

    @GetMapping("/ip-limited")
    @RateLimit(keytype = KeyType.IP,
            capacity = 50, rate = 10,
            algorithm = Algorithm.SLIDING_WINDOW_COUNTER)
    public String ipLimited() { return "ok"; }

}
