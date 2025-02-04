package edu.dio.zanetti;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
"name",
"power",
"alias",
"imageUrl"
})
public class SuperheroRequest {
    @JsonProperty("name") private String name;
    @JsonProperty("power") private String power;
    @JsonProperty("alias") private String alias;
    @JsonProperty("imageBase64") private String imageBase64;
    
    @JsonProperty("name")
    public String getName() {
        return name;
    }
    
    @JsonProperty("name")
    public void setName(String name) {
        this.name = name;
    }
    
    @JsonProperty("power")
    public String getPower() {
        return power;
    }
    
    @JsonProperty("power")
    public void setPower(String power) {
        this.power = power;
    }
    
    @JsonProperty("alias")
    public String getAlias() {
        return alias;
    }
    
    @JsonProperty("alias")
    public void setAlias(String alias) {
        this.alias = alias;
    }
    
    @JsonProperty("imageBase64")
    public String getImageBase64 () {
        return imageBase64;
    }
    
    @JsonProperty("imageBase64")
    public void setImageBase64(String imageBase64) {
        this.imageBase64 = imageBase64;
    }
    
}