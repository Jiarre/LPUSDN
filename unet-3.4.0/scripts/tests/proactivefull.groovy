import org.arl.fjage.*
import org.arl.unet.*
import org.arl.unet.net.*
import org.arl.unet.Services.*
import org.arl.unet.phy.*
import org.arl.unet.sim.*
import org.arl.unet.sim.channels.*

channel.model = ProtocolChannelModel        // use the protocol channel model
modem.dataRate = [1024, 1024].bps           // arbitrary data rate
modem.frameLength = [32,32].bytes  // 1 second worth of data per frame
modem.preambleDuration = 0                  // no overhead from preamble
modem.txDelay = 0 // don't simulate hardware delays

                            // list with 4 nodes



///////////////////////////////////////////////////////////////////////////////
// simulation details
                          // list with 4 nodes
// collect statistics after a while
println '''
ttxcount,trxcount,offeredload,througput,txcount,rxcount,ctrtxcount,ctrrxcount
'''
def i = 1

for ( i = 3; i<13; i++){
    def nodes = 2..i
    def txcount = 0
    def rxcount = 0
    def ctrtxcount = 0
    def ctrrxcount = 0
    def reliabilities = [true,false]
    def protocols = 40..42
    def flag = false
    simulate 20.minutes, {
        def cont = node "1", address: 1,stack: "$home/etc/setup"
      nodes.each { myAddr ->
        def x = rnditem(0..1500)
        def y = rnditem(0..1500)
        def z = rnditem(0..1500)
        def myNode = node "${myAddr}", address: myAddr, location: [0, 0, 0], stack: "$home/etc/pstack"
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
                    def protocol = protocols.get(r.nextInt(3))
                    
                    while(dst == myAddr){
                        dst = rnditem(nodes)
                    }
                    
                    
                    kernel << new DatagramReq(to: dst,data: new byte[1],shortcircuit:false,reliability: reliability, protocol: protocol)
                    txcount++
                }
                 //print("$myAddr $kernel.flow_table ")
                //print("$myAddr $kernel.cached_flows ")
            
            
               
            })
            add new MessageBehavior(Message, { msg ->
            
            if(msg instanceof RxFrameNtf){
                if(msg.from != 1){
                   rxcount++ 
                }else{
                    ctrrxcount++
                   
                }
                
                //print("$myAddr $txcount $rxcount")
            }
               
           
               
            })
          

       
        
      }
      }
      cont.startup = {
           def phy = agentForService(Services.PHYSICAL)
           subscribe agentForService(Services.PHYSICAL)
           def link = agentForService org.arl.unet.Services.LINK
           subscribe agentForService(Services.LINK)
           
           
           def d = 2
           def count = 0
           def tx = 0
           def payload = []
           def goal = 0
           def c = 0
          
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
           
           for(def j:nodes){
               payload= []
               count = 0
               for(def k = 0; k<nodes.size();k++){
                   for(def q = 0; q<nodes.size();q++){
                       if(nodes[k]!=j){
                           for(rel in [-1,1]){
                               for(proto in protocols){
                                payload << nodes[q]
                                payload << nodes[k]
                                payload << proto
                                payload << rel
                                payload << nodes[k]
                               }
                                
                           }
                          
                   }
                   }
                   
               }
               pay = payload.collate(20)
               goal+=pay.size()
               
               
               for(p in pay){
                  link << new DatagramReq(to:j,data:p,shortcircuit:false,protocol:33)
                  
               }
               //print(payload)
           }
           
           

          
           
          
           
      }
    }
    println sprintf('%6d,%6d,%7.3f,%7.3f,%6d,%6d,%6d,%6d ',
    [trace.txCount, trace.rxCount, (txcount+ctrtxcount)*0.125 / 1200 , (rxcount+ctrrxcount)*0.125 /1200,txcount,rxcount,ctrtxcount,ctrrxcount])
    
}
