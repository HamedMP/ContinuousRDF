package storm.bolt;

import java.util.Map;

import storm.bloomfilter.BloomFilter;
import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.IRichBolt;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;

public class BoltBuilderWithThreeBF implements IRichBolt {
	private OutputCollector collector;
	private BloomFilter<String> bf1;
	private BloomFilter<String> bf2;
	private int id;
	
	/**
	 * initialization
	 */
	public void prepare(Map stormConf, TopologyContext context,
			OutputCollector collector) {	
		//initialize the emitter
		this.collector = collector;
		//initialize an empty Bloom Filter with fp=0.001 and maximum_element=20 
		this.bf1 = new BloomFilter(0.01, 10);
		this.bf2 = new BloomFilter(0.01, 10);
		this.id = context.getThisTaskId();
	}
	
	/**
	 * The main method of Bolt, it will be called when the bolt receives a new tuple
	 * It will add the subject to the triple received into the Bloom Filter 
	 */
	public void execute(Tuple input) {
		String Subject = input.getStringByField("Subject");
		String Predicate = input.getStringByField("Predicate");
		String Object = input.getStringByField("Object");
		
		/* call corresponding function if you run
		 * 
		 * if you run oneVariableJoin Query Result will be : [Sophie, Justine, Fabrice]
		 * if you run twoVariableJoin Query Result will be : [Sophie, Justine, Fabrice, Lea]
		 * if you run multiVariableJoin Query Result will be : [Sophie, Justine, Fabrice, Lea, Frederic]
		 * 
		 * */
		
		//oneVariableJoin(Subject, Predicate, Object);
		//twoVariableJoin(Subject, Predicate, Object);
		multiVariableJoin(Subject, Predicate, Object);
	}

	//("1-variable join, to find the authors for paper kNN who works in INRIA and who has a Ph.D diplome:");
	public void oneVariableJoin(String Subject,String Predicate, String Object) {
		
		if(Predicate.equals("Diplome")){
			if(Object.equals("Ph.D")){
				collector.emit(new Values("ProberTaskID_"+id, Subject));
			}
		}else if(Predicate.equals("Work")){
			if(Object.equals("INRIA")){
				collector.emit(new Values("BuilderTaskID_1_"+id, Subject));
			}
			
		}else if(Predicate.equals("Paper")){
			if(Object.equals("kNN")){
				collector.emit(new Values("BuilderTaskID_2_"+id, Subject));
			}
			
		}
		
	}
	
	//("2-variable join, to find the authors for paper kNN who works in INRIA and their diplome:");
	public void twoVariableJoin(String Subject,String Predicate, String Object) {
		
		if(Predicate.equals("Diplome")){
			//if(Object.equals("Ph.D")){
				collector.emit(new Values("ProberTaskID_"+id, Subject));
			//}
			
		}else if(Predicate.equals("Work")){
			if(Object.equals("INRIA")){
				collector.emit(new Values("BuilderTaskID_1_"+id, Subject));
			}
			
		}else if(Predicate.equals("Paper")){
			if(Object.equals("kNN")){
				collector.emit(new Values("BuilderTaskID_2_"+id, Subject));
			}
		}
		
	}
	
	//("multi-variable join, to find the authors for paper kNN, and the place they work, and their diplome: ");
	public void multiVariableJoin(String Subject,String Predicate, String Object) {
		if(Predicate.equals("Diplome")){
			//if(Object.equals("Ph.D")){
				collector.emit(new Values("ProberTaskID_"+id, Subject));
			//}
			
		}else if(Predicate.equals("Work")){
			//if(Object.equals("INRIA")){
				collector.emit(new Values("BuilderTaskID_1_"+id, Subject));
			//}
			
		}else if(Predicate.equals("Paper")){
			if(Object.equals("kNN")){
				collector.emit(new Values("BuilderTaskID_2_"+id, Subject));
			}
		}
	}
	
	public void declareOutputFields(OutputFieldsDeclarer declarer) {
		declarer.declare(new Fields("ID","Content"));
	}

	public void cleanup() {
		
	}

	public Map<String, Object> getComponentConfiguration() {
		return null;
	}
}