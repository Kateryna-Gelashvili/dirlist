package org.k.config;

import com.google.common.collect.ImmutableList;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.DefaultServletHandlerConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Configuration
public class SpringConfig extends WebMvcConfigurerAdapter {

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.add(new ResourceHttpMessageConverter());

        MappingJackson2HttpMessageConverter jsonConverter
                = new MappingJackson2HttpMessageConverter(objectMapper());
        jsonConverter.setSupportedMediaTypes(ImmutableList.of(MediaType.ALL));
        jsonConverter.setDefaultCharset(StandardCharsets.UTF_8);

        converters.add(jsonConverter);
    }

    @Bean
    ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Override
    public void configureDefaultServletHandling(DefaultServletHandlerConfigurer configurer) {
        configurer.enable();
    }
}