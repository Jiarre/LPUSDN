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
 int getAction(int uuid){
     for(e in flow_table){
         if(uuid==e[0]){
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
    // subscribe to all agents that provide the datagram service
    subscribeForService(Services.DATAGRAM)
  }

  @Override
  Message processRequest(Message msg) {
    if(msg instanceof GetRouteReq){
        RouteRsp rsp = new RouteRsp(msg)
        println(flow_table)
        return new Message(msg,Performative.AGREE)
    }
    if(msg instanceof DatagramReq){
        def link = agentForService(org.arl.unet.Services.LINK)
        ArrayList mask = getMask(msg)
        int id = isInCache(mask)
        if(id != -1){
            if(buffer[id]){
                buffer[id].add(msg)
            }
            else{
                buffer[id] = new ArrayList<DatagramReq>()
                buffer[id].add(msg)
            }
            return new Message(msg,Performative.REFUSE)
        }
        def action = match(mask)
        println(action)
        println(mask)
        if(action == controller_address){
            ID++
            ArrayList cached = mask.plus(0,ID)
            cached_flows.add(cached)
            link << new DatagramReq(to:controller_address,protocol:33,reliability:true,data:cached)
        }else{
            msg.to = action
            link << msg
        }
        return new Message(msg,Performative.AGREE)
    }
    return null

  }
  
  @Override
  void processMessage(Message msg) {
    def link = agentForService(org.arl.unet.Services.LINK)
    //Protocols:
    //  35: new flow
    //  36: delete flow
    //  37: generate flow
    if (msg instanceof DatagramNtf && msg.protocol == 35) {
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
    return null
  }

  List<Parameter> getParameterList() {      
    allOf(Params)
  }

}
