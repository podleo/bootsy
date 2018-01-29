/**
 * 
 */
package com.flyover.bootsy.operator.provders;

/**
 * @author mramach
 *
 */
public abstract class AbstractProvider implements Provider {
	
	private String name;
	
	public AbstractProvider(String name) {
		this.name = name;
	}
	
	public String name() {
		return name;
	}

}
