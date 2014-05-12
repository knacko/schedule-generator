package ca.uottawa.ui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import ca.uottawa.schedule.Course;
import ca.uottawa.schedule.Schedule;
import ca.uottawa.schedule.ScheduleMessage;

import com.lloseng.ocsf.server.ConnectionToClient;

public class GenerateTask implements Runnable {
	ScheduleMessage message;
	ConnectionToClient client;
	ScheduleGeneratorServer server;

	public GenerateTask(ScheduleMessage message, ConnectionToClient client, ScheduleGeneratorServer server) {
			this.message = message;
			this.client = client;
			this.server = server;
	 }

	public void run() {
		generateSchedules(message, client, server);
	}
	
	private void generateSchedules(ScheduleMessage message, ConnectionToClient client, ScheduleGeneratorServer server) {
		//User is generating schedules
    	List<Course> mandatoryCourses = message.getCourses();
        int k = message.getK();
        List<Course> optional = message.getOptionalCourses();
       
        server.display("Received " + mandatoryCourses.size() + " mandatory courses.");
        server.display("Received " + optional.size() + " optional courses, choosing " + k);
        
        String sortOrder = message.getSortOrder();
    	boolean ignoreExtras = message.isIgnoreExtras();
    	List<Schedule> result;
    	if (k>0) {
    		result = Schedule.generateSchedules(mandatoryCourses, optional, k);
    	} else {
    		result = Schedule.generateSchedules(mandatoryCourses);
    	}
    	server.display("Generated " + result.size() + " schedules. Now sorting by " + sortOrder);
    	if (result.size() > 0) {
	    	//We should update the schedule stats in preparation of sorting.
	    	for (Schedule s: result) {
	    		s.updateStats();
	    	}
	    	//Now sort
	    	result = Schedule.sort(sortOrder, result, ignoreExtras);
    	}
    	server.display("Sorted. Sending back...");
    	server.updateStats(client.getInetAddress().getHostAddress().toString(), mandatoryCourses.size(), optional.size(), k, result.size());
    	
    	ScheduleMessage schedulesMsg = new ScheduleMessage();
    	schedulesMsg.setCommand("SCHEDULES");
    	
    	if (result.size() > 1000) {
    		List<Schedule> shortResult = result.subList(0, 1000); //Shrink list because it takes a while to transfer.
        	result = new ArrayList<Schedule>();
        	result.addAll(shortResult);
    		server.display("New results size: " + result.size());
    	}
    	
    	schedulesMsg.setSchedules(result);

		try {
			server.display("SEND part 1");
			client.sendToClient(schedulesMsg);
			server.display("SEND part 2");
		} catch (IOException e) {
			e.printStackTrace();
			server.display("Unable to report back generated schedules. Possible connection lost.");
			server.display(client.toString());
		}
	}

}