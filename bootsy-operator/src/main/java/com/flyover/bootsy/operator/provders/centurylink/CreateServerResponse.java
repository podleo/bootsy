/**
 * 
 */
package com.flyover.bootsy.operator.provders.centurylink;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;

/**
 * @author mramach
 *
 */
public class CreateServerResponse {
	
	private List<Link> links = new LinkedList<>();
	private Map<String, Object> any = new LinkedHashMap<String, Object>();

	public List<Link> getLinks() {
		return links;
	}

	public void setLinks(List<Link> links) {
		this.links = links;
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
