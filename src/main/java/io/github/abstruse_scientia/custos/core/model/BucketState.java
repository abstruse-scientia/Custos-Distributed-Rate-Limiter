package io.github.abstruse_scientia.custos.core.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class BucketState {

    private double tokens;
    private long lastRefillTime;
}
