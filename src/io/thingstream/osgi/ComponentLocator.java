package io.thingstream.osgi;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;


public class ComponentLocator<T> {
	
	private static final String GET = "get";
	
	protected TreeSet<ServiceReference<T>> refs;
	protected HashMap<ServiceReference<T>, T> serviceMap;
	boolean serviceRankingMandatory;
	Object lock = new Object();
	
	public ComponentLocator() {
		this(true);
	}
	
	/**
	 * If serviceRankingMandatory is true a runtime exception is thrown when a service is bound 
	 * that does not have service ranking property set or is not a valid integer.
	 * @param serviceRankingMandatory
	 */
	public ComponentLocator(boolean serviceRankingMandatory) {
		
		this.serviceRankingMandatory = serviceRankingMandatory;
		refs = new TreeSet<ServiceReference<T>>(new ServiceRankingComparator());
		serviceMap = new HashMap<ServiceReference<T>, T>();
	}
	
	public boolean isServiceRankingMandatory() {
		return serviceRankingMandatory;
	}

	public void bind(ServiceReference<T> ref) {
		
		synchronized(lock) {
			refs.add(ref);
			// populate map
			getService(ref);
		}
		
	}
	
	public void unbind(ServiceReference<T> ref) {
		
		synchronized(lock) {
			refs.remove(ref);
			serviceMap.remove(refs);
		}
		
	}
	
	public void update(ServiceReference<T> ref) {
		
		synchronized(lock) {
			
		}
		
	}
	
	public T getService(ServiceReference<T> ref) {
		
		T service = null;
		try {
			
			service = serviceMap.get(ref);
		} catch(Throwable t) {
			// in case an unsynchronized get can fail
		}
		if(service == null) {
			
			synchronized(lock) {
				
				service = serviceMap.get(ref);
				if(service == null) {
					
					BundleContext context = ref.getBundle().getBundleContext();
					service = context.getService(ref);
					if(service != null) {
						serviceMap.put(ref, service);
					}
				}
			}
		}
		return service;
	}
	
	/**
	 * Releases all the service references
	 */
	public void clear() {
		synchronized(lock) {
			refs.clear();
		}
	}
	
	public List<T> getServices() {
		
		List<T> services = new ArrayList<T>();
		for(ServiceReference<T> ref : refs) {
			
			BundleContext context = ref.getBundle().getBundleContext();
			T service = context.getService(ref);
			services.add(service);
			
		}
		return services;
		
	}
	
	@SuppressWarnings("unchecked")
	public Set<ServiceReference<T>> getServiceReferences() {
		
//		List<ServiceReference<T>> services = new ArrayList<ServiceReference<T>>(refs);
//		
//		return services;
		return (Set<ServiceReference<T>>) refs.clone();
	}
	
	public List<T> matchServices(String ldapString) throws InvalidSyntaxException {
		
		List<T> services = new ArrayList<T>();
		Filter filter = null;
		
		for(ServiceReference<T> ref : refs) {
			
			BundleContext context = ref.getBundle().getBundleContext();
			if(filter == null) {
				
				filter = context.createFilter(ldapString);
				
			}
			
			// if the ldapString is duff, then we will throw an exception above
			// and never get here
			
			if(filter.match(ref)) {
			
				T service = getService(ref);
				services.add(service);
				
			}
			
		}
		
		return services;
		
	}
	
	public Map<String, List<T>> matchServicesViaBeanProperties(Object bean) throws IllegalArgumentException {
		
		Map<String, List<T>> services = new HashMap<String, List<T>>();
		
		if(bean == null) {
			
			throw new IllegalArgumentException("Bean cannot be NULL");
			
		}
		
		for(Method m : bean.getClass().getDeclaredMethods()) {
			
			String name = m.getName();
			if(name.startsWith(GET)) {
				
				if(m.getReturnType() == String.class && m.getParameterTypes().length == 0) {
					
					try {
						
						String value = (String) m.invoke(bean, new Object[]{});
						String propertyName = name.substring(GET.length());
						List<T> matches = matchServices("(" + propertyName + "=" + value + ")");
						if(matches != null && matches.size() > 0) {
							
							services.put(propertyName, matches);
							
						}
						
					} catch(Exception ex) {
						
						// we can't do anything about this - ignore and continue
						
					}
					
				}
				
			}
			
		}
		
		return services;
		
	}
	
	private class ServiceRankingComparator implements Comparator<ServiceReference<T>> {

		public int compare(ServiceReference<T> r1, ServiceReference<T> r2) {

			int rank1 = getRank(r1.getProperty(Constants.SERVICE_RANKING));
			int rank2 = getRank(r2.getProperty(Constants.SERVICE_RANKING));
			int diff = rank2 - rank1;
			if(diff == 0) {
				// ensure set still distinguishes between the service references even though they have same ranking
			
				try {
					long val1 = Long.parseLong(r1.getProperty(Constants.SERVICE_ID).toString());
					long val2 = Long.parseLong(r2.getProperty(Constants.SERVICE_ID).toString());
					diff = (int) (val2 - val1);
				} catch(Exception e) {}
			}
			if(diff == 0) {
				diff = r2.hashCode() - r1.hashCode();
			}
			return diff;
			
		}
		protected int getRank(Object rank) {
			int val = -1;
			if(rank == null) {
			
				if(serviceRankingMandatory) {
					
					throw new NullPointerException("Missing service ranking property");
					
				}
			} else {
				
				String strVal = rank.toString();
				try {
					val = Integer.parseInt(strVal);
				} catch(NumberFormatException e) {
					if(serviceRankingMandatory) {
						throw new NumberFormatException("Service ranking \""+ strVal + "\" is not a valid integer");
					}
				}
			}
			return val;
		}
	}

}
