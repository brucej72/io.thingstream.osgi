package io.thingstream.osgi.publisher;

import java.lang.reflect.Array;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;

public class ServicePublisher {
	
	private BundleContext context;
	private Map<Object, ServiceRegistration<?>> refs;
	
	public ServicePublisher(ComponentContext cc) {
		
		this.context = cc.getBundleContext();
		refs = Collections.synchronizedMap(new HashMap<Object, ServiceRegistration<?>>());
		
	}
	
	public void close() {
		
		for(Object key : refs.keySet()) {
			
			ServiceRegistration<?> sr = refs.get(key);
			sr.unregister();
			
		}
		
		refs = null;
		context = null;
		
	}
	
	public void withdraw(Object service) {
		
		ServiceRegistration<?> sr = refs.get(service);
		
		if(sr != null) {

			sr.unregister();
			refs.remove(sr);

		}
		
	}
	
	public void publish(Class<?> clazz, Object service, Dictionary<String, ?> properties) {
		
		ServiceRegistration<?> sr = context.registerService(clazz.getName(), service, properties);
		refs.put(service, sr);
				
	}
	
	public void publish(Class<?> serviceClass, Object service) throws ServicePublicationException {
		
		Class<?> clazz = service.getClass(); 
		Hashtable<String, Object> h = new Hashtable<String, Object>();
		
		for(Property p : clazz.getAnnotationsByType(Property.class)) {
			
			String name = p.name();
			Object value = getValue(p.type(), p.value());
			
			// check to see if the name is already in the map
			// if not just add it
			
			if(!h.containsKey(name)) {
				
				h.put(name, value);
				
			} else {
				
				// if it is we need to get the current entry in the map
				// check if it's the same class, and if it's already an array
				
				Object current = h.get(name);
				Class<?> currentClass = current.getClass();
				int currentSize = 1;
				
				if(currentClass.isArray()) {
										
					if(currentClass.getComponentType() != clazz) {
						
						throw new ServicePublicationException("Service property class mismatch");
						
					}
					
					currentSize = Array.getLength(current);
					
				}
				
				// now we know if it is already an array, we need to create a new array of the same type
				// but 1 element bigger to take the new value
				
				// if it isn't an array, then we need to create one and add the new value to it
				
				Object newArray = Array.newInstance(currentClass, currentSize + 1);
				
				if(currentClass.isArray()) {
					
					// copy the current array entries into the new array
					
					for(int x = 0; x < currentSize; x++) {
						
						Array.set(newArray, x, Array.get(current, x));
						
					}
					
				} else {
					
					// copy the current single-value into the new array
					
					Array.set(newArray, 0, current);
					
				}
				
				// add 'this' entry to the end
				
				Array.set(newArray, currentSize, value);
				
				// put it back into the map
				
				h.put(name, newArray);
				
			}
						
		}
		
		publish(serviceClass, service, h);
		
	}
	
	private Object getValue(Class<?> clazz, String s) throws ServicePublicationException {
		
		Object o = null;
		
		if(s == null || s.length() == 0) {
			
			throw new ServicePublicationException("Unable to parse service property data");
			
		}
		
		try {
		
			if(clazz == String.class) {
				
				o = s;
				
			} else if(clazz == Integer.class) {
				
				o = Integer.parseInt(s);
				
				
			} else if(clazz == Boolean.class) {
				
				o = Boolean.parseBoolean(s);
				
				
			} else if(clazz == Double.class) {
				
				o = Double.parseDouble(s);
				
				
			} else if(clazz == Byte.class) {
				
				o = Byte.parseByte(s);
				
			} else if(clazz == Character.class) {
				
				o = s.charAt(0);
				
			} else if(clazz == Float.class) {
				
				o = Float.parseFloat(s);
				
			} else if(clazz == Long.class) {
				
				o = Long.parseLong(s);
				
			} else if(clazz == Short.class) {
				
				o = Short.parseShort(s);
				
			} else {
				
				throw new ServicePublicationException("Illegal service property type. Must be: String, Integer, Boolean, Double, Byte, " +
						"Character, Float, Long or Short");
				
			}
			
		} catch(NumberFormatException nx) {
			
			throw new ServicePublicationException("Unable to parse service property value", nx);
			
		}
		
		return o;
		
	}

}
