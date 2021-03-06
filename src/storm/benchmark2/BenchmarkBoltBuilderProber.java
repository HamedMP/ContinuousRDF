package storm.benchmark2;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.jena.base.Sys;

import storm.bloomfilter.BloomFilter;
import storm.config.TopologyConfiguration;
import backtype.storm.Constants;
import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.IRichBolt;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;

public class BenchmarkBoltBuilderProber implements IRichBolt {
	private OutputCollector collector;
	private int id;

	private ArrayList<BloomFilter[]> bf1;
	private ArrayList<BloomFilter[]> bf2;
	private BloomFilter bf3;
	
	private ArrayList<String> bf1_ids;
	private ArrayList<String> bf2_ids;

	private int[] bf1_index;
	private int[] bf2_index;
	
	private ArrayList<Tuple> problist[];
	private int problist_index=0;

	private Set<String> queryResult;

	String Predicate="";
	String PredicateValue="";

	private int NUM_BF1 = TopologyConfiguration.NUMBER_BF1;
	private int NUM_BF2 = TopologyConfiguration.NUMBER_BF2;
	private int GenerationSize = TopologyConfiguration.GENERATION_SIZE;
	private int NumberOfGenerations = TopologyConfiguration.NUMBER_OF_GENERATIONS;

	Set<String> hs[];
	int slidingWindowPading = 0;

	//FileWriter filerwriter=null;

	int slidingWindowNumber=0;
	public BenchmarkBoltBuilderProber(String predicate, String value) {
		// TODO Auto-generated constructor stub

		Predicate=predicate;
		PredicateValue=value;

	}

	public void prepare(Map stormConf, TopologyContext context,
			OutputCollector collector) {

		this.collector = collector;
		this.id =  context.getThisTaskId();

		this.bf1 = new ArrayList<BloomFilter[]>();
		this.bf1_ids = new ArrayList<String>();
		this.bf1_index = new int[NUM_BF1];

		this.bf2 = new ArrayList<BloomFilter[]>();
		this.bf2_ids = new ArrayList<String>();
		this.bf2_index = new int[NUM_BF2];

		bf3 = new BloomFilter(0.01, GenerationSize);
		
		problist = new ArrayList[NumberOfGenerations];
		for(int i=0;i<NumberOfGenerations;i++) {
			problist[i] = new ArrayList<Tuple>();
		}

		hs = new HashSet[NumberOfGenerations];
		for(int i=0;i<NumberOfGenerations;i++) {
			hs[i] = new HashSet<>();
		}
		queryResult = new HashSet<String>();

	}

	private static boolean isTickTuple(Tuple tuple) {
		return tuple.getSourceComponent().equals(Constants.SYSTEM_COMPONENT_ID)
				&& tuple.getSourceStreamId().equals(Constants.SYSTEM_TICK_STREAM_ID);
	}
	
	public void execute(Tuple tuple) {

		//String tripleID = tuple.getStringByField("id");
		//if(tripleID.equals("process")) {
		if(isTickTuple(tuple)) {
			ProcessGeneration();
		}
		else {
			try {
				UpdateGeneration(tuple);

			} catch (IOException e) {

			}
		}
		

	}

	private void UpdateGeneration(Tuple tuple) throws IOException {

		String id = tuple.getStringByField("id");

		if(id.contains("bf1")) {

			if(bf1_ids.size()==0) {
				BloomFilter[] bf = new BloomFilter[NumberOfGenerations];
				for(int i=0;i<NumberOfGenerations;i++) {
					bf[i]= new BloomFilter(0.01, GenerationSize);
				}
				bf[0] = (BloomFilter<String>)tuple.getValueByField("bf");
				bf1.add(bf);
				bf1_ids.add(id);
				bf1_index[0]=1;
			}

			else if(bf1_ids.contains(id)) {
				int bfnumber = bf1_ids.indexOf(id);
				bf1.get(bfnumber)[bf1_index[bfnumber]%NumberOfGenerations] = (BloomFilter<String>)tuple.getValueByField("bf");
				bf1_index[bfnumber] = (bf1_index[bfnumber] +1)%NumberOfGenerations;

			}
			else if(! bf1_ids.contains(id)){
				bf1_ids.add(id);
				BloomFilter[] bf = new BloomFilter[NumberOfGenerations];
				for(int i=0;i<NumberOfGenerations;i++) {
					bf[i]= new BloomFilter(0.01, GenerationSize);
				}
				bf[0] = (BloomFilter<String>)tuple.getValueByField("bf");
				bf1.add(bf);
				bf1_index[0]=1;
			}

		}

		if(id.contains("bf2")) {
			if(bf2_ids.size()==0) {

				BloomFilter[] bf = new BloomFilter[NumberOfGenerations];
				for(int i=0;i<NumberOfGenerations;i++) {
					bf[i]= new BloomFilter(0.01, GenerationSize);
				}
				bf[0] = (BloomFilter<String>)tuple.getValueByField("bf");
				bf2.add(bf);
				bf2_ids.add(id);
				bf2_index[0]=1;
			}

			else if(bf2_ids.contains(id)) {
				int bfnumber = bf2_ids.indexOf(id);
				bf2.get(bfnumber)[bf2_index[bfnumber]%NumberOfGenerations] = (BloomFilter<String>)tuple.getValueByField("bf");
				bf2_index[bfnumber] = (bf2_index[bfnumber] +1)%NumberOfGenerations;
			}
			else if(! bf2_ids.contains(id)){
				bf2_ids.add(id);
				BloomFilter[] bf = new BloomFilter[NumberOfGenerations];
				for(int i=0;i<NumberOfGenerations;i++) {
					bf[i]= new BloomFilter(0.01, GenerationSize);
				}
				bf[0] = (BloomFilter<String>)tuple.getValueByField("bf");
				bf2.add(bf);
				bf2_index[0]=1;
			}

		}

		
		else if (id.equals("triple")){ 
			problist[problist_index].add(tuple);
			bf3.add(tuple.getValueByField("Subject"));
		}

	}

	void ProcessGeneration() { 

		for(int i=0;i<NumberOfGenerations;i++) {
			System.out.println("ProberList Size is: "+problist[i].size());
		}
		
		for(int i=0;i<bf1.size();i++) {
			for(int j=0;j<NumberOfGenerations;j++) {
				System.out.println("Bf1 Size is: "+bf1.get(i)[j].count());
			}
		}

		for(int i=0;i<bf2.size();i++) {
			for(int j=0;j<NumberOfGenerations;j++) {
				System.out.println("Bf2 Size is: "+bf2.get(i)[j].count());
			}
		}

		BloomFilter<String> bf3ToSend=new BloomFilter(bf3);
		collector.emit(new Values("bf2"+this.id,bf3ToSend));
		bf3.clear();
		
		for(int i=0;i<NumberOfGenerations;i++) {
			List<Tuple> tuplelist=problist[i];
			try {
				Join(tuplelist);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		problist_index = (problist_index+1) % NumberOfGenerations;
		problist[problist_index].clear();

		slidingWindowPading = (slidingWindowPading+1) % NumberOfGenerations;
		hs[slidingWindowPading].clear();


		System.out.println(this.id + " Results for Sliding Window: "+slidingWindowNumber++);
		System.out.println("Size is: "+queryResult.size()+" Query Result is: "+ queryResult);
		System.out.println("\n\n------------------------------------------------------------------\n\n");
		queryResult.clear();

	}
	private void Join(List<Tuple> tuplelist) throws IOException {

		for (Tuple tuple : tuplelist) {

			String Subject = tuple.getStringByField("Subject");
			String Predicate = tuple.getStringByField("Predicate");
			String Object = tuple.getStringByField("Object");

			boolean contains1=false;
			boolean contains2 = false;

			for(int i=0;i<NUM_BF1 && i< bf1.size() && !contains1;i++) {
				for(int j=0;j<NumberOfGenerations && !contains1;j++) {
					contains1 =  bf1.get(i)[j].contains(Subject);
				}
			} 

			for(int i=0;i<NUM_BF2 && i< bf2.size() && !contains2;i++) {
				for(int j=0;j<NumberOfGenerations && !contains2;j++) {
					contains2 =  bf2.get(i)[j].contains(Subject);
				}
			}
			
			if(contains1 && contains2){
				queryResult.add(Subject+","+Object);

				//String tripleid = tuple.getMessageId()
				String msgID = tuple.getMessageId().toString();

				boolean isFreshTriple = true;

				for(int i=0; i< NumberOfGenerations ; i++) {
					if( hs[i].contains(msgID) ) {
						isFreshTriple = false;
						break;
					}
				}

				if (isFreshTriple && hs[slidingWindowPading].add(msgID) ) {
					this.collector.ack(tuple);

					long start_time = Long.valueOf(tuple.getStringByField("timestamp"));
					long end_time = System.currentTimeMillis();
					System.out.println("Time for "+  Subject + " is: "+start_time + " "+ end_time);
					System.out.println("Time for "+  Subject + " is: "+ (end_time-start_time));
				}

			}

		}


	}

	public void declareOutputFields(OutputFieldsDeclarer declarer) {
		declarer.declare(new Fields("id","bf"));
	}

	public void cleanup() {
	}

	public Map<String, Object> getComponentConfiguration() {
		return null;
	}
}
