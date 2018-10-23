package io.thingstream.osgi;

import java.lang.reflect.Array;
import java.util.Dictionary;

import org.osgi.service.component.ComponentContext;

public class DictionaryHelper {
	
	public static <T> T getAndCheck(String key, ComponentContext context) throws IllegalArgumentException {
		
		@SuppressWarnings("unchecked")
		T s = (T) context.getProperties().get(key);
		
		if(s == null) {
			
			throw new IllegalArgumentException("Property not configured: " + key + " cannot be null");
			
		}
		
		return s;
		
	}
	
	public static String getAndCheckEnv(String key, ComponentContext context) throws IllegalArgumentException {
		
		String s = (String) context.getProperties().get(key);
		String e = context.getBundleContext().getProperty(key);
		
		if(e != null) return e;
		return s;
		
	}
	
	@SuppressWarnings("unchecked")
	public static <T> T getWithDefault(Dictionary<?,?> d, String key, T defaultValue) {
			
		T t = null;
		
		Class<?> clazz = defaultValue.getClass();
		
		Object o = d.get(key);
		if(o == null) {
			
			t = defaultValue;
			
		} else {
		
			Class<?> valueClass = o.getClass();
					
			// first check to see whether the classes are the same, and if so we're done
			
			if(valueClass.equals(clazz)) {
				
				t = (T) d.get(key);
				
			} else {
			
				// do we have a unary Object that can be formed into an array?
				
				if(clazz.isArray() && !valueClass.isArray()) {
					
					Class<?> base = clazz.getComponentType();
					if(valueClass.equals(base)) {
						
						// we do - encapsulate it into an singleton array
											
						Object array = Array.newInstance(base, 1);
						Array.set(array, 0, o);
						t = (T) array;
						
					}
					
				} else {
					
					// no, this just doesn't match so throw
					
					throw new ClassCastException("Dictionary value class and return type do not match ( " + 
							valueClass.getName() + " != " + clazz.getName() + " )");
					
				}
				
			}
			
		}
		
		return t;
		
	}

}
