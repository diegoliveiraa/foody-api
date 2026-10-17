package com.foody.testsupport.api;

import com.foody.api.FoodyApiApplication;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest(classes = FoodyApiApplication.class)
@AutoConfigureMockMvc
@Import(ApiIntegrationConfiguration.class)
public @interface ApiIntegrationTest {

}
