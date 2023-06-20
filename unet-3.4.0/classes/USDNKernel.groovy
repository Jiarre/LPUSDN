import org.arl.fjage.*
import org.arl.unet.*
import org.arl.fjage.param.Parameter

class USDNKernel extends UnetAgent {
    
    enum Params implements Parameter {        
        controller_address,
        flow_table,
        cached_flows,
        quarantine_flowtable,
        buffer
        }

      final String title = 'Underwater SDN Kernel'        
      final String description = 'Handles flow table for SDN communication'
    
    int controller_address = 10;
    int ID = 0;
    ArrayList flow_table = [[ID,-1,-1,-1,-1,controller_address]];
    ArrayList cached_flows = []
    ArrayList quarantine_flowtable = []
    def buffer = [:]
    
    //flow_table.add(default_entry);
 int getAction(int id){
     for(e in flow_table){
         if(id==e[0]){
             return e[5]
         }
     }
 }
 
 
    
  ArrayList getMask(msg){
      def msg_mask = [-1,-1,-1,-1]
      if(msg.hasProperty('from') && msg.from){
          msg_mask[0] = msg.from
      }
      if(msg.hasProperty('to') && msg.to){
          msg_mask[1] = msg.to
      }
      if(msg.hasProperty('protocol') && msg.protocol){
          msg_mask[2] = msg.protocol
      }
      if(msg.hasProperty('reliability') && msg.reliability){
          if(msg.reliability == true){
              msg_mask[3] = 1
          }else{
              msg_mask[3] = 0
          }
      }
      return msg_mask
  }
  int isInCache(mask){
      for(flow in cached_flows){
          if(mask[0]==flow[1] && mask[1]==flow[2] && mask[2]==flow[3] && mask[3]==flow[4]){
              return flow[0]
          }
      }
      return -1
  }
  
  int match(mask){
      def elegible = [:]
      elegible[0] = 4
      
      for(flow in flow_table){
          int count = 0
          if((mask[0] != flow[1] && mask[0]!=-1) || (mask[1] != flow[2] && mask[1]!=-1) || (mask[2] != flow[3] && mask[2]!=-1) || (mask[3] != flow[4] && mask[3]!=-1)){
              continue
          }
          print(flow)
          if(flow[1] == -1){
              count++
          }
          if(flow[2] == -1){
              count++
          }
          if(flow[3] == -1){
              count++
          }
          if(flow[4] == -1){
              count++
          }
          elegible[flow[0]] = count
      }
      int minkey = elegible.min{it.value}.key
      int action = getAction(minkey)
      return action
      
  }
  
  @Override
  void setup() {
    register Services.ROUTING  
    register Services.DATAGRAM // advertise that the agent provides a Routing service
  }
    @Override
  void startup() {
    subscribeForService(Services.DATAGRAM)
    subscribeForService(Services.LINK)
  }

  @Override
  Message processRequest(Message msg) {
    if(msg instanceof DatagramReq){
        return sendDatagram(msg)
    }
    if(msg instanceof DatagramReq){
        print(msg)
    }
    
    return null

  }
  @Override
  void processMessage(Message msg) {
    def link = agentForService(org.arl.unet.Services.LINK)
    def node = agentForService(Services.NODE_INFO)
    //Protocols:
    //. 34: update controller on a link situation
    //  35: new flow
    //  36: delete flow
    //  37: delete all flows with an action
    //  38: update action of a flow
    if(msg instanceof LinkStatusNtf || msg instanceof DatagramFailureNtf) {
        print(msg)
    }
    
    if (msg instanceof DatagramNtf && msg.protocol == 35) {
         newFlow(msg)
    }
    if (msg instanceof DatagramNtf && msg.protocol == 36) {
         deleteFlow(msg)
    }
    if (msg instanceof DatagramNtf && msg.protocol == 37) {
         deleteAllFlowsWithAction(msg)
    }
    if (msg instanceof DatagramNtf && msg.protocol == 38) {
         updateFlow(msg)
    }
    if (msg instanceof DatagramNtf){
        if(node.address != msg.to){
            forward(msg)
        }
        
    }
    
    return null
  }
  
  Message sendDatagram(msg){
        def link = agentForService(org.arl.unet.Services.LINK)
        ArrayList mask = getMask(msg)
        int id = isInCache(mask)
        if(id != -1){
            if(buffer[id]){
                buffer[id].add(msg)
            }
            
            return new Message(msg,Performative.REFUSE)
        }
        
        
        
        def action = match(mask)
 
        if(action == controller_address){
            ID++
            if(id == -1){
                buffer[ID] = new ArrayList<DatagramReq>()
                buffer[ID].add(msg)
            }
            ArrayList cached = mask.plus(0,ID)
            cached_flows.add(cached)
            link << new DatagramReq(to:controller_address,protocol:33,reliability:true,data:cached)
        }else{
            msg.to = action
            link << msg
        }
        return new Message(msg,Performative.AGREE)
  }
  
  void newFlow(msg){
        def link = agentForService(org.arl.unet.Services.LINK)

        def reply = msg.data.toList()
        def flow = []
        int id = reply[0]
        int action = reply[1]
        for(e in cached_flows){
            if(e[0] == id){
                flow = e
                break
            }
                 
        }
        cached_flows.remove(flow)
        flow.add(action)
        flow_table.add(flow)
        
        if(buffer.containsKey(id)){
          
            for(datagram in buffer[id]){
                datagram.to = action
                link << datagram
            }
            buffer.remove(id)
        }
  }
  
  void deleteFlow(msg){
      int idToDelete = msg.data.toList()[0]
      def flow_to_delete = []
      for(flow in flow_table){
          if(idToDelete == flow[0] ){
              flow_to_delete = flow
              break
          }
      }
      if(!flow_to_delete.isEmpty()){
          flow_table.remove(flow_to_delete)
      }
  }
  
  void deleteAllFlowsWithAction(msg){
      int actionToDelete = msg.data.toList()[0]
      def flows_to_delete = []
      for(flow in flow_table){
          if(actionToDelete == flow[5] ){
              flows_to_delete.add(flow)
          }
      }
      
      if(!flows_to_delete.isEmpty()){
          for(e in flows_to_delete){
          flow_table.remove(e)
            }
      }

  }
  
  void updateFlow(msg){
      def reply = msg.data.toList()
      int id = reply[0]
      int action = reply[1]
      def flag = false
      for(flow in flow_table){
          if(id == flow[0] ){
              flow[5] = action
              flag = true
          }
      }
  }
  void forward(msg){
      sendDatagram(msg)
  }
  

  List<Parameter> getParameterList() {      
    allOf(Params)
  }
    

}
