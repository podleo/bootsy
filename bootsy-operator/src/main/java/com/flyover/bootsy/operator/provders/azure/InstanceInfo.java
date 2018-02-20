/**
 * 
 */
package com.flyover.bootsy.operator.provders.azure;

import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;

/**
 * @author mramach
 *
 */
public class InstanceInfo {

	private String status;
	private Map<String, Object> any = new LinkedHashMap<String, Object>();

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	@JsonAnyGetter
	public Map<String,Object> any() {
	    return any;
	}

	@JsonAnySetter
	public void set(String name, Object value) {
	    any.put(name, value);
	}
	
}
