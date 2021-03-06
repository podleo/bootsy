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
public class ServerDetailResponse {
	
	private String id;
	private String name;
	private String status;
	private ServerDetails details = new ServerDetails();
	private List<Link> links = new LinkedList<>();
	private Map<String, Object> any = new LinkedHashMap<String, Object>();

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public ServerDetails getDetails() {
		return details;
	}

	public void setDetails(ServerDetails details) {
		this.details = details;
	}

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
