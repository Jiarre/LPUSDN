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

channel.model = ProtocolChannelModel        // use the protocol channel model
modem.dataRate = [1024, 1024].bps           // arbitrary data rate
modem.frameLength = [32,32].bytes  // 1 second worth of data per frame
modem.headerLength = 5                      // no overhead from header
modem.preambleDuration = 0                  // no overhead from preamble
modem.txDelay = 0 // don't simulate hardware delays

                            // list with 4 nodes

//trace.warmup = 60000
///////////////////////////////////////////////////////////////////////////////
// simulation details
                          // list with 4 nodes
// collect statistics after a while
println '''
txcount,rxcount,offeredload,througput
'''
def i = 1

for ( i = 3; i<50; i++){
    

    def nodes = 2..i
    def wait =  nodes.size()*nodes.size()*2000
    def flag = false
    def txcount = 0
    def rxcount = 0
    simulate 20.minutes, {
        def cont = node "1", address: 1,stack: "$home/etc/setup"
      nodes.each { myAddr ->
        def x = rnditem(0..1500)
        def y = rnditem(0..1500)
        def z = rnditem(0..1500)
        def myNode = node "${myAddr}", address: myAddr, location: [0, 0, 0], stack: "$home/etc/setup"
        myNode.startup = {                      // startup script to run on each node
          def phy = agentForService(Services.PHYSICAL)
          def link = agentForService org.arl.unet.Services.LINK
          subscribe agentForService(Services.LINK)
          def knowndst = []
          def cachedst = []
          
          
          add new PoissonBehavior((long)(1000), {  // avg time between events in ms
                // choose destination randomly (excluding self)
            if(flag==true){
                
            
            def dst = rnditem(nodes)
            while(dst == myAddr){
                dst = rnditem(nodes)
            }
            def mask = [dst,0,0,0,0]
           //print("k of $myAddr $knowndst and dst $dst ")
            
                //println("IKNOWHIM")
                txcount++

                //phy << new ClearReq()
                link << new DatagramReq(to: dst,data: new byte[32],shortcircuit:false,reliability: false)
            
                
            }
            })
            add new MessageBehavior(Message, { msg ->
           if(msg instanceof DatagramNtf && msg.protocol==35){
              

                def data = msg.data.toList()
                knowndst.add(data[0])

           }else{
               if(msg instanceof DatagramNtf){
                   rxcount++
               }
           }
               
           
               
            })
          add new PoissonBehavior((long)(60000), {  // avg time between events in ms
                // choose destination randomly (excluding self)
            txcount++
            if(flag==true){
                link << new DatagramReq(to: 1,data: new byte[8],shortcircuit:false,reliability: false,protocol:35)

            }
          })
        }
      }
      cont.startup = {
           def phy = agentForService(Services.PHYSICAL)
           subscribe agentForService(Services.PHYSICAL)
           def link = agentForService org.arl.unet.Services.LINK
           subscribe agentForService(Services.LINK)
           

           
           
           /*for(int j : nodes){
               for(int k: nodes){
                   if(j!=k){
                       link << new DatagramReq(to: j,data: [k,0,0,0,0],protocol:35,shortcircuit:false,reliability: false)
                   }
               }
               
           }*/
           for(int j: nodes){
               def d = Math.ceil((nodes.size()-1)/2) 
               
               for(def k = 2; k<d; k++){
                   link << new DatagramReq(to: j,data: [k,0,0,0,0],protocol:35,shortcircuit:false,reliability: false)
               }
           }
           flag = true
           
           add new PoissonBehavior((long)(60000), {  // avg time between events in ms
                // choose destination randomly (excluding self)
            def source = rnditem(nodes)
            if(flag == true){
                txcount++
                link << new DatagramReq(to: source,data: [0,0,0,0,0],shortcircuit:false,reliability: false,protocol:37)

            }
            
          })
               
           add new MessageBehavior(Message, { msg ->
           
               if(msg instanceof DatagramNtf){
                   rxcount++
               }
           })
           
           
           
      }
    }
    //println(rxcount*0.125 / 3600)
    println sprintf('%6d,%6d,%7.3f,%7.3f ',
    [trace.txCount, trace.rxCount, trace.offeredLoad, trace.throughput])
    //[trace.txCount, trace.rxCount, trace.offeredLoad, trace.rxCount * 0.125 / (3600 - (wait/ 1000))]
    
}
