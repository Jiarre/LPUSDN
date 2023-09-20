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
modem.frameLength = [16,16].bytes  // 1 second worth of data per frame
modem.headerLength = 5                      // no overhead from header
modem.preambleDuration = 0                  // no overhead from preamble
modem.txDelay = 0 // don't simulate hardware delays

                            // list with 4 nodes



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
    def txcount = 0
    def rxcount = 0
    simulate 1.hours, {
        def cont = node "1", address: 1,stack: "$home/etc/setup"
        def rel = node "50", address: 50,stack: "$home/etc/setup"
      nodes.each { myAddr ->
        def x = rnditem(0..1500)
        def y = rnditem(0..1500)
        def z = rnditem(0..1500)
        def myNode = node "${myAddr}", address: myAddr, location: [0, 0, 0], stack: "$home/etc/setup"
        myNode.startup = {                      // startup script to run on each node
          def phy = agentForService(Services.PHYSICAL)
          def link = agentForService org.arl.unet.Services.LINK
          subscribe agentForService(Services.LINK)
          def flow_table = []
          def cachedst = []
          int count = 0
          
          add new PoissonBehavior((long)(1000), {  // avg time between events in ms
                // choose destination randomly (excluding self)


            def dst = rnditem(nodes)
            def reliability = rnditem(0..1)
            def protocol = rnditem(40..50)
            
            while(dst == myAddr){
                dst = rnditem(nodes)
            }
            def mask = [dst,reliability,protocol,myAddr]
            //print("k of $myAddr $knowndst and dst $dst ")
            if(flow_table.contains(mask)){
                //println("IKNOWHIM")
                phy << new ClearReq()
                link << new DatagramReq(to: dst,data: new byte[16],shortcircuit:false,reliability: false)
            }else{
                if(!cachedst.contains(mask)){
                    def entry = mask.plus(0,count)
                    txcount++
                    cachedst.add(mask)
                    link << new DatagramReq(to: 50,data: entry,protocol: 35,reliability: false,shortcircuit: false)
                    

                }
                
            }
            })
            add new MessageBehavior(Message, { msg ->
           if(msg instanceof DatagramNtf && msg.protocol==35){
                def data = msg.data.toList()
                def index = data[0]
                def flow = [cachedst[index][1],cachedst[index][2],cachedst[index][3],cachedst[index][4]]
                flow_table.add(cachedst[index])
                //println("k of $myAddr updated to $flow_table")
                
           }else{
               if(msg instanceof DatagramNtf){
                   rxcount++
               }
           }
               
           
               
            })
          add new TickerBehavior((long)(60000), {  // avg time between events in ms

            link << new DatagramReq(to: 50,data: new byte[8],protocol: 36,shortcircuit:false,reliability: false)
          })
        }
      }
      cont.startup = {
           def phy = agentForService(Services.PHYSICAL)
           subscribe agentForService(Services.PHYSICAL)
           def link = agentForService org.arl.unet.Services.LINK
           subscribe agentForService(Services.LINK)
           add new MessageBehavior(Message, { msg ->
             

           if(msg instanceof DatagramNtf && msg.protocol == 35){
                
                def data = msg.data.toList()
                def from = msg.from
                //print("received from $from $data")
                def id = data[0]
                def act = data[1]
                def resp = [data[0],data[1],data[2],0,0]
                txcount++
                link << new DatagramReq(to: 50,data: resp,protocol:35,shortcircuit:false,reliability: false)
           }
               
           
               
            })
           
      }
      rel.startup = {
           def phy = agentForService(Services.PHYSICAL)
           subscribe agentForService(Services.PHYSICAL)
           def link = agentForService org.arl.unet.Services.LINK
           subscribe agentForService(Services.LINK)
           add new MessageBehavior(Message, { msg ->
             

           if(msg instanceof DatagramNtf && msg.protocol == 35 && msg.from != 1){
                //println("KOK")
                def data = msg.data.toList()
                def from = msg.from
                //print("forwarding from $from $data to controller")
                def id = data[0]
                def act = data[1]
                def resp = [id,act,from,0,0]
                txcount++
                link << new DatagramReq(to: 1,data: resp,protocol:35,shortcircuit:false,reliability: false)
           }
           if(msg instanceof DatagramNtf && msg.protocol == 35 && msg.from == 1){
                
                def data = msg.data.toList()
                def from = msg.from
                //print("forwarding resulting mask from to  $data")
                def id = data[0]
                def act = data[1]
                def resp = [id,act,from,0,0]
                txcount++
                link << new DatagramReq(to: data[2], data: resp,protocol:35,shortcircuit:false,reliability: false)
           }
           if(msg instanceof DatagramNtf && msg.protocol == 36 ){
                
                
                link << new DatagramReq(to: 1,data: new byte[8],protocol:36,shortcircuit:false,reliability: false)
           }
               
           
               
            })
           
      }
    }
    println sprintf('%6d,%6d,%7.3f,%7.3f,%6d ',
    [trace.txCount, trace.rxCount, trace.offeredLoad, trace.throughput,txcount])
    
}
