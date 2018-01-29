/**
 * 
 */
package com.flyover.bootsy.operator;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import org.springframework.boot.SpringApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.flyover.bootsy.operator.k8s.KubeAdapter;
import com.flyover.bootsy.operator.provders.Provider;
import com.flyover.bootsy.operator.provders.centurylink.CenturyLinkProvider;

/**
 * @author mramach
 *
 */
@EnableScheduling
public class Application {
	
	@Bean
	public ScheduledExecutorService executor() {
		return new ScheduledThreadPoolExecutor(4);
	}
	
	@Bean
	public KubeAdapter kubeAdapter() {
		return new KubeAdapter("http://10.82.98.33:8080");
	}
	
	@Bean
	public Provider centurylink() {
		return new CenturyLinkProvider();
	}
	
	@Bean
	public Operator operator() {
		return new Operator();
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

}
