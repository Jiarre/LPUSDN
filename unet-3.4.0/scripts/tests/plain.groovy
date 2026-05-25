//! Simulation



///////////////////////////////////////////////////////////////////////////////
// simulation settings


///////////////////////////////////////////////////////////////////////////////
// channel and modem settings

import org.arl.fjage.*
import org.arl.unet.*
import org.arl.unet.net.*
import org.arl.unet.Services.*
import org.arl.unet.phy.*
import org.arl.unet.sim.*
import org.arl.unet.sim.channels.*

channel.model = ProtocolChannelModel
channel.pDetection = 0.9
channel.pDecoding = 0.9 // use the protocol channel model
modem.dataRate = [1024, 1024].bps           // arbitrary data rate
modem.frameLength = [32,64].bytes  // 1 second worth of data per frame
modem.headerLength = 0                     // no overhead from header
modem.preambleDuration = 0   
modem.powerLevel = [0,-10,-10]// no overhead from preamble
modem.txDelay = 0 // don't simulate hardware delays

                            // list with 4 nodes



///////////////////////////////////////////////////////////////////////////////
// simulation details
                          // list with 4 nodes
// collect statistics after a while
println '''
ttxcount,offeredload,througput,rxcount,ctrtxcount,ctrrxcount
'''
def i = 1

for ( i = 3; i<12; i++){
    def mean = [0,0,0,0,0,0]
    for (ep = 0; ep < 5; ep++){
    def nodes = 2..i
    def txcount = 0
    def rxcount = 0
    def ctrtxcount = 0
    def ctrrxcount = 0
    def reliabilities = [true,false]
    def protocols = 40..42
    simulate 10.minutes, {
        def cont = node "1", address: 1,stack: "$home/etc/setup"
      nodes.each { myAddr ->
        def ra = rnditem(0..400)
        def theta = new Random().nextFloat()*6.28
        def phi = new Random().nextFloat()*3.14
        location = [ra*Math.sin(phi)*Math.cos(theta), ra*Math.sin(phi)*Math.sin(theta), ra*Math.cos(phi)]
        def myNode = node "${myAddr}", address: myAddr, location: location, stack: "$home/etc/setup"
        myNode.startup = {                      // startup script to run on each node
          def phy = agentForService(Services.PHYSICAL)
          def link = agentForService org.arl.unet.Services.LINK
          def kernel = agentForService(org.arl.unet.Services.ROUTING)

          subscribe agentForService(Services.LINK)
        subscribe agentForService(Services.PHYSICAL)

     
          def c = 0
          add new PoissonBehavior((long)(1000), {  // avg time between events in ms
                // choose destination randomly (excluding self)
            c++
            def dst = rnditem(nodes)
            def r = new Random()
            def reliability = reliabilities.get(r.nextInt(2))
            def protocol = protocols.get(r.nextInt(3))
            
            while(dst == myAddr){
                dst = rnditem(nodes)
            }
            
            
            //link << new DatagramReq(to: dst,data: new byte[1],shortcircuit:false,reliability: false)
            
            link << new DatagramReq(to: dst,data: new byte[56],shortcircuit:false,reliability: false,protocol: protocol)

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
      
    }
    mean[0]= mean[0]+trace.txCount
    mean[1]= mean[1]+(txcount)*0.5 / 1200
    mean[2]= mean[2]+(rxcount)*0.5 /1200
    mean[3]= mean[3]+rxcount
    mean[4]= mean[4]+ctrtxcount
    mean[5]= mean[5]+ctrrxcount
    
    
    

    }
    println sprintf('%7.3f,%7.3f,%7.3f,%7.3f,%7.3f,%7.3f ',
    [mean[0]/ep ,mean[1]/ep ,mean[2]/ep ,mean[3]/ep ,mean[4]/ep ,mean[5]/ep])
    
}
