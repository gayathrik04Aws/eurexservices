package com.prisma.eurex.util;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;



@Component
public class PropertyPlaceHolderConfigurer extends
		PropertyPlaceholderConfigurer implements ApplicationContextAware {


	@Override
	public void postProcessBeanFactory(
			ConfigurableListableBeanFactory beanFactory) throws BeansException {

		String tomcatDir = System.getProperty("catalina.base");

		Resource[] resources = null;
		
		resources = new Resource[]
					{new FileSystemResource(tomcatDir+"/conf/config.properties")};
		
		 setLocations(resources);


		super.postProcessBeanFactory(beanFactory);
	}

	@Override
	public void setApplicationContext(ApplicationContext arg0) throws BeansException {
		// TODO Auto-generated method stub
		
	}



}

