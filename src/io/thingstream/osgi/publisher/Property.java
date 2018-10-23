package io.thingstream.osgi.publisher;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(value=RetentionPolicy.RUNTIME)
@Target(value=ElementType.TYPE)
@Inherited
@Repeatable(Properties.class)
public @interface Property {
	
	public String name();
	public Class<?> type();
	public String value();

}


