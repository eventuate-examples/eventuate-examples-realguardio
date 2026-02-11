package io.realguardio.orchestration.sagas;

import io.realguardio.orchestration.sagas.proxies.CustomerServiceProxy;
import io.realguardio.orchestration.sagas.proxies.SecuritySystemServiceProxy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SagaProxyConfiguration {

    @Bean
    public SecuritySystemServiceProxy securitySystemServiceProxy() {
        return new SecuritySystemServiceProxy();
    }

    @Bean
    public CustomerServiceProxy customerServiceProxy() {
        return new CustomerServiceProxy();
    }
}