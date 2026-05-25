import org.arl.fjage.*
import org.arl.unet.*
import org.arl.unet.net.*
import org.arl.unet.Services.*
import org.arl.unet.phy.*
import org.arl.unet.sim.*
import org.arl.unet.sim.channels.*
import org.arl.unet.mac.*

channel.model = ProtocolChannelModel        // use the protocol channel model
modem.dataRate = [1024, 1024].bps           // arbitrary data rate
modem.frameLength = [32,64].bytes  // 1 second worth of data per frame
modem.preambleDuration = 0                  // no overhead from preamble
modem.txDelay = 0 // don't simulate hardware delays
channel.pDetection = 0.9
channel.pDecoding = 0.9
modem.powerLevel = [0,-10,-10]
modem.headerLength = 0  

                            // list with 4 nodes



///////////////////////////////////////////////////////////////////////////////
// simulation details
                          // list with 4 nodes
// collect statistics after a while
println '''
nodes,txcount,offeredload,througput,rxcount,ctrtxcount,ctrrxcount,txdup,rxdup
'''
def i = 1
for ( i = 10; i<12; i++){
    def mean = [0,0,0,0,0,0]
    for (ep = 0; ep < 20; ep++){
    def nodes = 2..i
    def txcount = 0
    def rxcount = 0
    def ctrtxcount = 0
    def ctrrxcount = 0
    def reliabilities = [true,false]
    def protocols = 40..45
    def flag = true
    def txdup = 0
    def rxdup = 0
    simulate 10.minutes, {
        def cont = node "1", address: 1,stack: "$home/etc/setup"
      nodes.each { myAddr ->
        def ra = rnditem(0..400)
        def theta = new Random().nextFloat()*6.28
        def phi = new Random().nextFloat()*3.14
        location = [ra*Math.sin(phi)*Math.cos(theta), ra*Math.sin(phi)*Math.sin(theta), ra*Math.cos(phi)]
        def myNode = node "${myAddr}", address: myAddr, location: location, stack: "$home/etc/hstack"
        myNode.startup = {                      // startup script to run on each node
          def phy = agentForService(Services.PHYSICAL)
          def link = agentForService org.arl.unet.Services.LINK
          def kernel = agentForService(org.arl.unet.Services.ROUTING)
          subscribe agentForService(Services.LINK)
          subscribe agentForService(Services.PHYSICAL)

          def cycl = 0
           
          add new PoissonBehavior((long)(1000), {  // avg time between events in ms
                // choose destination randomly (excluding self)
                if(flag == true){
                    def dst = rnditem(nodes)
                    def r = new Random()
                    def reliability = reliabilities.get(r.nextInt(2))
                    def protocol = protocols.get(r.nextInt(6))
                    
                    while(dst == myAddr){
                        dst = rnditem(nodes)
                    }
                    
                    
                    kernel << new DatagramReq(to: dst,data: new byte[56],shortcircuit:false,reliability:false, protocol: protocol)
                    txcount++
                    //print("$myAddr $kernel.flow_table ")
                    //print("$myAddr $kernel.buffer ")
                }
                 //print("$myAddr $kernel.flow_table ")
                //print("$myAddr $kernel.cached_flows ")
                //print("$myAddr $kernel.cached_flows remaining flows: $kernel.cached_flows.size \n")

            
            
               
            })
            add new MessageBehavior(Message, { msg ->
            

        
            if(msg instanceof DatagramNtf){
                if(msg.from != 1 && msg.protocol >= 40){
                   rxcount++ 
                }else{
                    if(msg.protocol == 33 && msg.from == 1 && msg.to == myAddr){
                        //print("$myAddr $kernel.cached_flows remaining flows: $kernel.cached_flows.size \n")
                        //print("\n")
                        //print(msg.data.toList())
                        //print("\n")
                        ctrrxcount++
                    }
                    
                   
                }
                
                //print("$myAddr $txcount $rxcount")
            }
           
           
               
            })
          

       
        
      }
      }
      cont.startup = {
           def phy = agentForService(Services.PHYSICAL)
           def mac = agentForService(Services.MAC)
           subscribe agentForService(Services.PHYSICAL)
           def link = agentForService org.arl.unet.Services.LINK
           subscribe agentForService(Services.LINK)
           
           
           def d = 2
           def count = 0
           def tx = 0
           def payload = []
           def goal = 0
           def c = 0
           def base_flows = []
          
           add new MessageBehavior(Message, { msg ->
            
            if(msg instanceof TxFrameStartNtf){
                //print(msg)
                ctrtxcount++
                if(ctrtxcount >= goal && flag == false){
                    //print("FLAG")
                    flag = true
                }
            }
               
           
               
            })
            add new MessageBehavior(Message, { msg ->
           if((msg instanceof RxFrameNtf && msg.protocol == 33) ){
                def data = msg.data.toList()
                def pay2 = []
                def from = msg.from
                d = data.size() / 4
                def payload2 = []
                for( def j=0; j<d; j++){
                    payload2 << data[(j*4) + 0]
                    payload2 << data[(j*4) + 1]
                    payload2 << data[(j*4) + 2]
                    payload2 << data[(j*4) + 3]
                    payload2 << data[(j*4) + 1]
                

    
               
                  
               }
                pay2 = payload2.collate(20)

                mac << new ReservationReq(duration:pay.size()*250,to:from)

               for(p in pay2){
                  
                  phy << new TxFrameReq(to:from,data:p,protocol:33,type: 1)
                  txdup++
               }
                //def resp_msg = new TxFrameReq(to: from,data: data,protocol:33,reliability:false,type:1)
                //phy << new ClearReq()
                //phy << resp_msg
                //phy << resp_msg //double ack
           }
               
           
               
            })
           base_flows = []
           payload= []
           count = 0
           for(def k = 0; k<nodes.size();k++){
               //for(def q = 0; q<nodes.size();q++){
                   
                       for(rel in [-1]){
                           for(proto in [40,41,42]){
                            //payload << nodes[q]
                            payload << -1
                            payload << nodes[k]
                            payload << proto
                            payload << rel
                            payload << nodes[k]
                            base_flows.add([-1,nodes[k],proto,rel,nodes[k]])
                           }
                            
                       
                      
              // }
               }
               
           }
           pay = payload.collate(20)
           goal+=pay.size()
           
           mac << new ReservationReq(duration:pay.size()*250,to:0)

           for(p in pay){
              phy << new TxFrameReq(to:0,data:p,protocol:33,type: 1)
              ctrtxcount++
           }
           
           
           //print(payload)
           
           
           

          
           
          
           
      }
    }
    mean[0]= mean[0]+txcount
    mean[1]= mean[1]+(txcount)*0.5 / 1200
    mean[2]= mean[2]+(rxcount)*0.5 /1200
    mean[3]= mean[3]+rxcount
    mean[4]= mean[4]+ctrtxcount
    mean[5]= mean[5]+ctrrxcount
    
    
    
    /*println sprintf('%7.3f, %7.3f, %7.3f, %7.3f, %7.3f, %7.3f, %7.3f ',
    [nodes.size(),txcount,(txcount)*0.5 / 1200 ,(rxcount)*0.5 /1200 ,rxcount ,ctrtxcount ,ctrrxcount])*/
    def off = ((txcount)*0.5 / 600)
    def thr = ((rxcount)*0.5 / 600)
    def num = nodes.size()
    print("$num, $txcount, $off, $thr, $rxcount, $ctrtxcount, $ctrrxcount, $txdup, $rxdup")
    }
    
    
}
