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
modem.frameLength = [1024/8,1024/8].bytes  // 1 second worth of data per frame
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

for ( i = 10; i<50; i++){
    def nodes = 2..i
    def txcount = 0
    def rxcount = 0
    simulate 2.hours, {
        def cont = node "1", address: 1,stack: "$home/etc/setup"
      nodes.each { myAddr ->
        def x = rnditem(0..1500)
        def y = rnditem(0..1500)
        def z = rnditem(0..1500)
        def myNode = node "${myAddr}", address: myAddr, location: [x, y, z], stack: "$home/etc/setup"
        myNode.startup = {                      // startup script to run on each node
          def phy = agentForService(Services.PHYSICAL)
          def link = agentForService org.arl.unet.Services.LINK
             // arrival rate per node
          add new PoissonBehavior((long)(60000), {  // avg time between events in ms
                // choose destination randomly (excluding self)
            txcount++
            phy << new ClearReq()
            link << new DatagramReq(to: 1,data: new byte[5],reliability: false)
          })
          add new PoissonBehavior((long)(200), {  // avg time between events in ms
                // choose destination randomly (excluding self)
            txcount++
            def dst = rnditem(nodes-myAddr)
            phy << new ClearReq()
            link << new DatagramReq(to: dst,data: new byte[5],reliability: false)
          })
          /*add new PoissonBehavior((long)(120000), {  // avg time between events in ms
                // choose destination randomly (excluding self)
            txcount++
            phy << new ClearReq()
            link << new DatagramReq(to: 1,data: new byte[5],reliability: false,protocol:35)
          })*/
        }
      }
      cont.startup = {
           def phy = agentForService(Services.PHYSICAL)
           subscribe agentForService(Services.PHYSICAL)
           def link = agentForService org.arl.unet.Services.LINK
           add new MessageBehavior(Message, { msg ->
           if(msg instanceof RxFrameNtf && msg.protocol == 35){
               rxcount++
                  def from = msg.from
                phy << new ClearReq()
                link << new DatagramReq(to: from,data: new byte[5],reliability: false)
           }
               
           
               
            })
           add new PoissonBehavior((long)(60000), {  // avg time between events in ms
                // choose destination randomly (excluding self)
            phy << new ClearReq()
            nodes.each { addr -> 
                link << new DatagramReq(to: addr,data: new byte[5],reliability: false)
            }
            
          })
      }
    }
    println sprintf('%6d,%6d,%7.3f,%7.3f ',
    [trace.txCount, trace.rxCount, trace.offeredLoad, trace.throughput])
    
}
