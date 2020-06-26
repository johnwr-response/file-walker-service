package no.responseweb.imagearchive.filewalkerservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "response.filestore", ignoreUnknownFields = false)
public class ResponseFilestoreProperties {
    private String pathNickname;
    private String overrideWalkPath = "";
    private int walkerForgetCutoffHours = 1;
}
