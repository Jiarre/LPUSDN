import org.arl.fjage.*
import org.arl.unet.*
import org.arl.unet.mac.*

class USDNKernel extends UnetAgent {

  
  
  @Override
  void setup() {
    register Services.ROUTING                // advertise that the agent provides a ROUTING service
  }
  
  @Override
  void startup(){

  }

  @Override
  Message processRequest(Message msg) {
   if(msg instanceof GetRouteReq){
       def res = new Message(new RouteInfo(to:10),Performative.INFORM)
       def returned = new RouteRsp(res)
       println(res)
       return res
       
   }
   
   
    return null
  }
}
