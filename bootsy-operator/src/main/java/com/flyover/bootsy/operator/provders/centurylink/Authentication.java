/**
 * 
 */
package com.flyover.bootsy.operator.provders.centurylink;

import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;

/**
 * @author mramach
 *
 */
public class Authentication {
	
    private String bearerToken;
    private Map<String, Object> additionalProperties = new LinkedHashMap<String, Object>();

    public String getBearerToken() {
        return bearerToken;
    }

    public void setBearerToken(String bearerToken) {
        this.bearerToken = bearerToken;
    }

    @JsonAnyGetter
    public Map<String,Object> any() {
        return additionalProperties;
    }

    @JsonAnySetter
    public void set(String name, Object value) {
        additionalProperties.put(name, value);
    }

}
