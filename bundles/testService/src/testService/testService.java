package testService;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ServiceScope;

@Component(immediate = true, enabled = true, scope = ServiceScope.SINGLETON, service = { testService.class },
property = {
	    "service.exported.interfaces=ai.darklight.dl.shared.init.cert.ICertificateManager",
	    org.osgi.framework.Constants.SERVICE_EXPORTED_CONFIGS + "=ecf.generic.server",
	    "ecf.generic.server.needClientAuth=true",
	    "osgi.basic.timeout=50000" })
public class testService {
	public String helloWorld() {
		return "Hello World";
	}
	
	@Activate
	public void activate() {
		System.out.println("Activated");
	}
}
