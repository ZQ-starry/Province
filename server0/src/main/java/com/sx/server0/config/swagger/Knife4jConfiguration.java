package com.sx.server0.config.swagger;

import io.swagger.annotations.Api;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2WebMvc;

import java.security.Timestamp;

/**
 * @Author: ZhangQi
 * @Date: 2023/7/26 8:41
 * @Description:
 */
@Configuration
@EnableSwagger2WebMvc
public class Knife4jConfiguration {

    /**
     * 通过knife4j生成接口文档
     * @return
     */
    @Bean
    public Docket createDocket() {
        Docket docket = new Docket(DocumentationType.SWAGGER_2)
                .apiInfo(new ApiInfoBuilder()
                        .title("ApiDoc")
                        .description("API Document for QuZhou Project")
                        .version("1.0")
                        .contact(new Contact("SX_software", "http://www.hz-energy.cn", "software@sx-energy.com.cn"))
                        .build())
                .select()
                //这里指定Controller扫描包路径
                .apis(RequestHandlerSelectors.any())
                .apis(RequestHandlerSelectors.basePackage("com.sx.server0.controller"))
                .apis(RequestHandlerSelectors.withClassAnnotation(Api.class))
                .paths(PathSelectors.any())
                .build()
                .directModelSubstitute(Timestamp.class, String.class);
        return docket;
    }
}
