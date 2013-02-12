package monitor.main;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;

import org.apache.log4j.Logger;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;

public class Quarts implements Job {
	private JobDetail job;
	private Trigger trigger;
	private Scheduler scheduler;
	private String ipAdress;
	private ArrayList<IpAddressRefreshListener> ipAddressRefreshListeners;
	private static final Logger LOGGER = LoggerHelper.getLogger(Quarts.class);
	

	public Quarts(){
		super();
		init();
	}

	public void addIpAddressRefreshListener(IpAddressRefreshListener ipRefreshListener){
		ipAddressRefreshListeners.add(ipRefreshListener);
	}

	public void init() {
		ipAddressRefreshListeners = new ArrayList<IpAddressRefreshListener>(1);
		refreshIpAdress();
		try{
			scheduler = StdSchedulerFactory.getDefaultScheduler();
			scheduler.start();

			job = JobBuilder.newJob(Quarts.class).withIdentity("ipMonitorJob").build();

			trigger =  TriggerBuilder.newTrigger()
					.withIdentity("ipMonitorTrigger")
					.startNow()
					.withSchedule(
							SimpleScheduleBuilder
							.simpleSchedule()
							.withIntervalInSeconds(30)
							.repeatForever())
							.build();

			scheduler.scheduleJob(job, trigger);

			//			while(ipMonitorFrame!=null && ipMonitorFrame.isShowing()){
			//				Thread.sleep(1000);
			//			}
		}
		catch(Exception e){
			e.printStackTrace(System.out);
		}
	}

	@Override
	public void execute(JobExecutionContext arg0) throws JobExecutionException {
		refreshIpAdress();
		for (IpAddressRefreshListener ipRefreshListener : ipAddressRefreshListeners){
			ipRefreshListener.onIpAddresRefresh();
		}
	}

	public void refreshIpAdress(){
		String ipAdress = "null";
		try {
			Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
			while (interfaces.hasMoreElements()){
				NetworkInterface current = interfaces.nextElement();

				if (!current.isUp() || current.isLoopback() || current.isVirtual()) continue;

				Enumeration<InetAddress> addresses = current.getInetAddresses();
				while (addresses.hasMoreElements()){
					InetAddress current_addr = addresses.nextElement();
					if (current_addr.isLoopbackAddress() || current_addr instanceof Inet6Address) continue;
					if (current_addr instanceof Inet4Address){
						ipAdress = current_addr.getHostAddress();
						LOGGER.info("Refreshed ip: "+ipAdress);
					}
				}
			}
		} catch (SocketException e) {
			ipAdress = e.getClass().toString();
			e.printStackTrace();
		}
		this.ipAdress = ipAdress;
	}

	public String getIpAdress() {
		if(ipAdress!=null){
			return ipAdress;
		}
		else{
			refreshIpAdress();
			return ipAdress;
		}
	}
}
