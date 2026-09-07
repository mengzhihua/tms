package com.tms.thirdparty;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ThirdPartyConfig {
    @Bean
    public ThirdPartyLogisticsAdapter mockSfAdapter() {
        return new MockExpressAdapter("MOCK_SF");
    }

    @Bean
    public ThirdPartyLogisticsAdapter mockJdAdapter() {
        return new MockExpressAdapter("MOCK_JD");
    }

    @Bean
    public ThirdPartyLogisticsAdapter mockZtoAdapter() {
        return new MockExpressAdapter("MOCK_ZTO");
    }
}
