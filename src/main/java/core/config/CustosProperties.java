package core.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "custos.token-bucket")
@Getter
@Setter
public class CustosProperties {

    private int capacity = 50;
    private int refillRate = 10;
}
