import org.arl.fjage.*
import org.arl.unet.*
import org.arl.unet.net.*
import org.arl.unet.Services.*
import org.arl.unet.phy.*
import org.arl.unet.sim.*
import org.arl.unet.sim.channels.*

channel.model = ProtocolChannelModel        // use the protocol channel model
modem.dataRate = [1024, 1024].bps           // arbitrary data rate
modem.frameLength = [32,64].bytes  // 1 second worth of data per frame
modem.preambleDuration = 0     
modem.headerLength = 0                      // no overhead from header

modem.powerLevel = [0,-10,-10]// no overhead from preamble
modem.txDelay = 0 // don't simulate hardware delays
                            // list with 4 nodes
//channel.communicationRange = 1500.m
channel.pDetection = 0.9
channel.pDecoding = 0.9


///////////////////////////////////////////////////////////////////////////////
// simulation details
                          // list with 4 nodes
// collect statistics after a while
println '''
ttxcount,offeredload,txcount,rxcount,ctrtxcount,ctrrxcount
'''
def i = 1
def ep = 0
for ( i = 3; i<13; i++){
    mean = [0,0,0,0,0,0]
    for(ep = 0; ep<5; ep++){
    def nodes = 2..i
    def txcount = 0
    def rxcount = 0
    def ctrtxcount = 0
    def ctrrxcount = 0
    def sum = 0
    def reliabilities = [true,false]
    def protocols = 40..44
    def flag = true
    simulate 20.minutes, {
        def cont = node "1", address: 1,stack: "$home/etc/setup"
      nodes.each { myAddr ->
        def ra = rnditem(0..500)
        def theta = new Random().nextFloat()*6.28
        def phi = new Random().nextFloat()*3.14
        location = [ra*Math.sin(phi)*Math.cos(theta), ra*Math.sin(phi)*Math.sin(theta), ra*Math.cos(phi)]
        //print(Math.sqrt(Math.pow(ra*Math.sin(phi)*Math.cos(theta),2)+Math.pow( ra*Math.sin(phi)*Math.sin(theta),2)+Math.pow(ra*Math.cos(phi),2)))
        def myNode = node "${myAddr}", address: myAddr, location: location, stack: "$home/etc/pstack"
        myNode.startup = {                      // startup script to run on each node
          def phy = agentForService(Services.PHYSICAL)
          def link = agentForService org.arl.unet.Services.LINK
          def kernel = agentForService(org.arl.unet.Services.ROUTING)
          subscribe agentForService(Services.LINK)
          subscribe agentForService(Services.PHYSICAL)
          
           
          add new PoissonBehavior((long)(1000), {  // avg time between events in ms
                // choose destination randomly (excluding self)
                if(flag == true){
                    def dst = rnditem(nodes)
                    def r = new Random()
                    //def reliability = reliabilities.get(r.nextInt(2))
                    def protocol = protocols.get(r.nextInt(5))
                    
                    while(dst == myAddr){
                        dst = rnditem(nodes)
                    }
                    
                    //print(protocol)
                    kernel << new DatagramReq(to: dst,data: new byte[56],shortcircuit:false,reliability: false, protocol: protocol)
                    txcount++
                }
                 //print("$myAddr $kernel.flow_table ")
                //print("$myAddr $kernel.cached_flows ")
            
            
               
            })
            add new MessageBehavior(Message, { msg ->
        
            if(msg instanceof DatagramNtf){
                if(msg.from != 1 && msg.protocol >= 40){
                   rxcount++ 
                }else{
                    if(msg.protocol == 35 && msg.from == 1 && msg.to == myAddr){
                        print("CTR $msg")
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
                   
                       if(nodes[k]!=j){
                           
                                payload << -1
                                payload << nodes[k]
                                payload << -1
                                payload << -1
                                payload << nodes[k]
                               
                                
                           
                          
                   
                   }
                   
               }
               pay = payload.collate(20)
               goal+=pay.size()
               
               
               for(p in pay){
                  phy << new TxFrameReq(to:j,data:p,shortcircuit:false,protocol:33,type: 1)
               }
               //print(payload)
           }
           
           

          
           
          
           
      }
    }
    
    mean[0]= mean[0]+txcount
    mean[1]= mean[1]+(txcount)*0.5 / 1200
    mean[2]= mean[2]+(rxcount)*0.5 /1200
    mean[3]= mean[3]+rxcount
    mean[4]= mean[4]+ctrtxcount
    mean[5]= mean[5]+ctrrxcount
    
    
    

    }
    println sprintf('%7.3f,%7.3f,%7.3f,%7.3f,%7.3f,%7.3f ',
    [mean[0]/ep ,mean[1]/ep ,mean[2]/ep ,mean[3]/ep ,mean[4]/ep ,mean[5]/ep])
}
